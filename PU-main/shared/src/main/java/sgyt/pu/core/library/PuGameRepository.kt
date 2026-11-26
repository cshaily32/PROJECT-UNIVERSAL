package sgyt.pu.core.library

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.provider.DocumentsContract
import sgyt.pu.core.archive.PuArchiveExtractor
import sgyt.pu.core.detection.PuGameDetector
import sgyt.pu.core.json.PuJson
import sgyt.pu.core.json.PuJsonObject
import sgyt.pu.core.json.PuJsonValue
import sgyt.pu.core.model.PuCompatibilityEstimate
import sgyt.pu.core.model.PuDetectionResult
import sgyt.pu.core.model.PuEngineType
import sgyt.pu.core.model.PuGameRecord
import sgyt.pu.core.security.PuCrypto
import sgyt.pu.core.storage.PuStorage
import sgyt.pu.model.PuProfile
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID

data class PuImportResult(
    val game: PuGameRecord,
    val detection: PuDetectionResult,
)

/**
 * Persistent library and import pipeline. Imported content is copied into
 * app-owned storage so document-provider permissions are not required at launch.
 */
class PuGameRepository(
    private val context: Context,
    private val storage: PuStorage,
    private val detector: PuGameDetector = PuGameDetector(),
    private val archives: PuArchiveExtractor = PuArchiveExtractor(),
) : PuGameLibrary {
    private val recordsFile = File(storage.stateRoot, "games.json")

    @Synchronized
    override fun listGames(): List<PuGameRecord> = loadRecords().sortedByDescending { it.updatedAt }

    @Synchronized
    override fun getGame(gameId: String): PuGameRecord? = loadRecords().firstOrNull { it.id == gameId }

    @Synchronized
    fun importFolder(source: File, sourceUri: Uri? = null): PuImportResult {
        require(source.isDirectory) { "The selected game folder does not exist" }
        val id = newGameId()
        val destination = storage.gameDirectory(id)
        destination.mkdirs()
        storage.copyDirectory(source, destination)
        return persistImportedGame(id, destination, sourceUri)
    }

    @Synchronized
    fun importArchive(source: File, sourceUri: Uri? = null): PuImportResult {
        require(source.isFile) { "The selected game archive does not exist" }
        val id = newGameId()
        val destination = storage.gameDirectory(id)
        archives.extract(source, destination)
        return persistImportedGame(id, destination, sourceUri)
    }

    @Synchronized
    override fun importUri(uri: Uri): PuImportResult {
        val resolver = context.contentResolver
        val mimeType = resolver.getType(uri)
        val displayName = resolver.query(uri, arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME), null, null, null)
            ?.use(::readDisplayName)
            ?: "imported-game"
        val incoming = File(storage.incomingRoot, "${UUID.randomUUID()}-$displayName")
        incoming.parentFile?.mkdirs()

        if (mimeType == DocumentsContract.Document.MIME_TYPE_DIR || uri.toString().contains("/tree/")) {
            copyTree(resolver, uri, incoming)
        } else {
            resolver.openInputStream(uri)?.use { input ->
                FileOutputStream(incoming).use { output -> input.copyTo(output) }
            } ?: throw IOException("Could not read the selected item")
        }

        return try {
            when (archives.detectFormat(incoming)) {
                sgyt.pu.core.archive.PuArchiveFormat.ZIP,
                sgyt.pu.core.archive.PuArchiveFormat.TAR_GZ,
                -> importArchive(incoming, uri)

                sgyt.pu.core.archive.PuArchiveFormat.UNKNOWN -> {
                    if (incoming.isDirectory) importFolder(incoming, uri)
                    else {
                        val id = newGameId()
                        val destination = storage.gameDirectory(id)
                        destination.mkdirs()
                        incoming.copyTo(File(destination, incoming.name), overwrite = true)
                        persistImportedGame(id, destination, uri)
                    }
                }
            }
        } finally {
            storage.deleteRecursively(incoming)
        }
    }

    @Synchronized
    override fun deleteGame(gameId: String): Boolean {
        val records = loadRecords()
        val record = records.firstOrNull { it.id == gameId } ?: return false
        storage.deleteRecursively(storage.gameDirectory(gameId))
        storage.deleteRecursively(storage.cacheDirectory(gameId))
        storage.deleteRecursively(storage.sandboxDirectory(gameId))
        saveRecords(records.filterNot { it.id == gameId })
        return true
    }

    @Synchronized
    override fun renameGame(gameId: String, title: String): PuGameRecord? {
        val records = loadRecords().toMutableList()
        val index = records.indexOfFirst { it.id == gameId }
        if (index < 0) return null
        val cleanTitle = title.trim().take(120)
        if (cleanTitle.isEmpty()) return records[index]
        records[index] = records[index].copy(title = cleanTitle, updatedAt = System.currentTimeMillis())
        saveRecords(records)
        return records[index]
    }

    @Synchronized
    override fun setProfile(gameId: String, profile: PuProfile): PuGameRecord? {
        val records = loadRecords().toMutableList()
        val index = records.indexOfFirst { it.id == gameId }
        if (index < 0) return null
        records[index] = records[index].copy(selectedProfile = profile, updatedAt = System.currentTimeMillis())
        saveRecords(records)
        return records[index]
    }

    @Synchronized
    override fun setCacheSize(gameId: String, sizeBytes: Long): PuGameRecord? {
        val records = loadRecords().toMutableList()
        val index = records.indexOfFirst { it.id == gameId }
        if (index < 0) return null
        records[index] = records[index].copy(
            cacheSizeBytes = sizeBytes.coerceAtLeast(0),
            updatedAt = System.currentTimeMillis(),
        )
        saveRecords(records)
        return records[index]
    }

    @Synchronized
    override fun setCompatibility(gameId: String, estimate: PuCompatibilityEstimate): PuGameRecord? {
        val records = loadRecords().toMutableList()
        val index = records.indexOfFirst { it.id == gameId }
        if (index < 0) return null
        records[index] = records[index].copy(compatibility = estimate, updatedAt = System.currentTimeMillis())
        saveRecords(records)
        return records[index]
    }

    @Synchronized
    fun applyFix(gameId: String, fixId: String): PuGameRecord? {
        val records = loadRecords().toMutableList()
        val index = records.indexOfFirst { it.id == gameId }
        if (index < 0) return null
        val existing = records[index].appliedFixIds
        records[index] = records[index].copy(
            appliedFixIds = (existing + fixId).distinct(),
            updatedAt = System.currentTimeMillis(),
        )
        saveRecords(records)
        return records[index]
    }

    private fun persistImportedGame(id: String, importedRoot: File, sourceUri: Uri?): PuImportResult {
        val detectionRoot = chooseDetectionRoot(importedRoot)
        val detection = detector.detect(detectionRoot)
        val now = System.currentTimeMillis()
        val record = PuGameRecord(
            id = id,
            rootPath = detectionRoot.absolutePath,
            title = detection.title,
            engine = detection.engine,
            version = detection.version.raw,
            fingerprint = detection.fingerprint,
            importedAt = now,
            updatedAt = now,
            originalSizeBytes = storage.directorySize(importedRoot),
            compatibility = if (detection.supported) PuCompatibilityEstimate.LIKELY_SMOOTH else PuCompatibilityEstimate.PROBABLY_WONT_RUN,
            sourceUri = sourceUri?.toString(),
        )
        val records = loadRecords().toMutableList()
        records += record
        saveRecords(records)
        cacheDetection(record, detection)
        return PuImportResult(record, detection)
    }

    private fun chooseDetectionRoot(importedRoot: File): File {
        val rootDetection = detector.detect(importedRoot)
        if (rootDetection.engine != PuEngineType.UNKNOWN) return importedRoot
        val directories = importedRoot.listFiles()?.filter { it.isDirectory }.orEmpty()
        if (directories.size == 1) {
            val nested = detector.detect(directories.first())
            if (nested.engine != PuEngineType.UNKNOWN) return directories.first()
        }
        return importedRoot
    }

    private fun cacheDetection(record: PuGameRecord, detection: PuDetectionResult) {
        val cacheFile = File(storage.stateRoot, "detections.json")
        val cached = storage.readJson(cacheFile)
            ?.let { it as? PuJsonValue.JsonObject }
            ?.values
            ?.mapNotNull { (key, value) -> (value as? PuJsonValue.JsonObject)?.let { key to it } }
            ?.toMap()
            ?: mutableMapOf()
            .toMutableMap()
        val metadata = File(record.rootPath).listFiles()?.sortedBy { it.name }?.joinToString("|") {
            "${it.name}:${it.length()}:${it.lastModified()}"
        }.orEmpty()
        cached[record.id] = PuJson.obj(
            "fingerprint" to PuJson.string(PuCrypto.sha256(metadata.toByteArray(Charsets.UTF_8))),
            "detectedAt" to PuJson.number(System.currentTimeMillis()),
            "engine" to PuJson.string(detection.engine.name),
            "version" to PuJson.string(detection.version.raw),
        )
        storage.writeJson(cacheFile, PuJsonValue.JsonObject(cached))
    }

    private fun loadRecords(): List<PuGameRecord> {
        val root = storage.readJson(recordsFile) as? PuJsonValue.JsonObject ?: return emptyList()
        return root.values.mapNotNull { value -> gameFromJson(value as? PuJsonValue.JsonObject ?: return@mapNotNull null) }
    }

    private fun saveRecords(records: List<PuGameRecord>) {
        storage.writeJson(recordsFile, PuJsonValue.JsonObject(records.associateBy { it.id }.mapValues { gameToJson(it.value) }))
    }

    private fun copyTree(resolver: ContentResolver, treeUri: Uri, destination: File) {
        destination.mkdirs()
        val documentId = DocumentsContract.getTreeDocumentId(treeUri)
        copyDocumentChildren(resolver, treeUri, documentId, destination)
    }

    private fun copyDocumentChildren(
        resolver: ContentResolver,
        treeUri: Uri,
        parentDocumentId: String,
        destination: File,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, parentDocumentId)
        resolver.query(
            childrenUri,
            arrayOf(
                DocumentsContract.Document.COLUMN_DOCUMENT_ID,
                DocumentsContract.Document.COLUMN_DISPLAY_NAME,
                DocumentsContract.Document.COLUMN_MIME_TYPE,
            ),
            null,
            null,
            null,
        )?.use { cursor ->
            val idIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DOCUMENT_ID)
            val nameIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
            val mimeIndex = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_MIME_TYPE)
            while (cursor.moveToNext()) {
                val id = cursor.getString(idIndex) ?: continue
                val name = cursor.getString(nameIndex) ?: continue
                val mime = cursor.getString(mimeIndex).orEmpty()
                val target = File(destination, name)
                if (mime == DocumentsContract.Document.MIME_TYPE_DIR) {
                    target.mkdirs()
                    copyDocumentChildren(resolver, treeUri, id, target)
                } else {
                    val childUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, id)
                    resolver.openInputStream(childUri)?.use { input ->
                        FileOutputStream(target).use { output -> input.copyTo(output) }
                    } ?: throw IOException("Could not read $name")
                }
            }
        }
    }

    private fun readDisplayName(cursor: Cursor): String? {
        val index = cursor.getColumnIndex(DocumentsContract.Document.COLUMN_DISPLAY_NAME)
        return if (index >= 0 && cursor.moveToFirst()) cursor.getString(index) else null
    }

    private fun newGameId(): String = UUID.randomUUID().toString()

    private fun gameToJson(game: PuGameRecord): PuJsonValue = PuJson.obj(
        "id" to PuJson.string(game.id),
        "rootPath" to PuJson.string(game.rootPath),
        "title" to PuJson.string(game.title),
        "engine" to PuJson.string(game.engine.name),
        "version" to PuJson.string(game.version),
        "fingerprint" to PuJson.string(game.fingerprint),
        "importedAt" to PuJson.number(game.importedAt),
        "updatedAt" to PuJson.number(game.updatedAt),
        "originalSizeBytes" to PuJson.number(game.originalSizeBytes),
        "cacheSizeBytes" to PuJson.number(game.cacheSizeBytes),
        "selectedProfile" to PuJson.nullableString(game.selectedProfile?.name),
        "compatibility" to PuJson.string(game.compatibility.name),
        "appliedFixIds" to PuJson.arr(game.appliedFixIds.map(PuJson::string)),
        "sourceUri" to PuJson.nullableString(game.sourceUri),
    )

    private fun gameFromJson(json: PuJsonValue.JsonObject): PuGameRecord? {
        val objectValue = PuJsonObject(json.values)
        val id = objectValue.string("id") ?: return null
        val rootPath = objectValue.string("rootPath") ?: return null
        return PuGameRecord(
            id = id,
            rootPath = rootPath,
            title = json.string("title").orEmpty(),
            engine = json.string("engine")?.let { value ->
                runCatching { PuEngineType.valueOf(value) }.getOrDefault(PuEngineType.UNKNOWN)
            } ?: PuEngineType.UNKNOWN,
            version = json.string("version").orEmpty(),
            fingerprint = json.string("fingerprint").orEmpty(),
            importedAt = json.long("importedAt") ?: 0,
            updatedAt = json.long("updatedAt") ?: 0,
            originalSizeBytes = json.long("originalSizeBytes") ?: 0,
            cacheSizeBytes = json.long("cacheSizeBytes") ?: 0,
            selectedProfile = json.string("selectedProfile")?.let { value ->
                runCatching { PuProfile.valueOf(value) }.getOrNull()
            },
            compatibility = json.string("compatibility")?.let { value ->
                runCatching { PuCompatibilityEstimate.valueOf(value) }.getOrDefault(PuCompatibilityEstimate.LIKELY_SMOOTH)
            } ?: PuCompatibilityEstimate.LIKELY_SMOOTH,
            appliedFixIds = json.array("appliedFixIds").mapNotNull { (it as? PuJsonValue.JsonString)?.value },
            sourceUri = json.string("sourceUri"),
        )
    }
}
