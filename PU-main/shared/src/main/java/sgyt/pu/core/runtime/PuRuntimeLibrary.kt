package sgyt.pu.core.runtime

import sgyt.pu.core.model.PuInstalledRuntime
import sgyt.pu.core.model.PuRuntimeCatalog
import sgyt.pu.core.model.PuRuntimeDescriptor
import java.util.concurrent.atomic.AtomicBoolean

/** Domain-facing runtime catalog and installation contract used by the UI. */
interface PuRuntimeLibrary {
    fun listCatalog(): PuRuntimeCatalog

    fun refreshCatalog(): PuRuntimeCatalog

    fun listInstalled(): List<PuInstalledRuntime>

    fun getRuntime(runtimeId: String, revision: Long? = null): PuRuntimeDescriptor?

    fun isInstalled(runtime: PuRuntimeDescriptor): Boolean

    fun install(
        descriptor: PuRuntimeDescriptor,
        cancellation: AtomicBoolean = AtomicBoolean(false),
        onProgress: (PuDownloadProgress) -> Unit = {},
    ): PuInstalledRuntime

    fun remove(runtimeId: String, revision: Long? = null): Boolean
}
