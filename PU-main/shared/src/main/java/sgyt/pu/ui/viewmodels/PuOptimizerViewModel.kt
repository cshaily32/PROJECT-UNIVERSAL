package sgyt.pu.ui.viewmodels

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import sgyt.pu.core.device.PuDeviceProbe
import sgyt.pu.core.library.PuGameLibrary
import sgyt.pu.core.model.PuOptimizationManifest
import sgyt.pu.core.model.optimizationBlockReason
import sgyt.pu.core.optimizer.PuOptimizationCancelledException
import sgyt.pu.core.optimizer.PuOriginalFilesChangedException
import sgyt.pu.core.optimizer.PuOptimizer
import sgyt.pu.model.PuEdition
import sgyt.pu.model.PuProfile
import java.util.concurrent.atomic.AtomicBoolean

enum class PuOptimizerPhase {
    IDLE,
    RUNNING,
    PAUSED,
    COMPLETED,
    ERROR,
}

data class PuOptimizerState(
    val phase: PuOptimizerPhase = PuOptimizerPhase.IDLE,
    val gameTitle: String = "",
    val profile: PuProfile = PuProfile.LOW,
    val totalAssets: Int = 0,
    val completedAssets: Int = 0,
    val savedBytes: Long = 0,
    val sourceBytes: Long = 0,
    val cacheBytes: Long = 0,
    val currentAsset: String = "",
    val manifest: PuOptimizationManifest? = null,
    val originalFilesUnchanged: Boolean? = null,
    val errorMessage: String? = null,
) {
    val progress: Float
        get() = if (totalAssets > 0) (completedAssets.toFloat() / totalAssets).coerceIn(0f, 1f) else 0f
}

sealed interface PuOptimizerAction {
    data object OnStart : PuOptimizerAction
    data object OnPause : PuOptimizerAction
    data object OnResume : PuOptimizerAction
    data object OnRefresh : PuOptimizerAction
}

sealed interface PuOptimizerEvent {
    data object Completed : PuOptimizerEvent
}

/** Runs the real, resumable per-game optimization pipeline. */
class PuOptimizerViewModel(
    private val gameId: String,
    private val edition: PuEdition,
    private val library: PuGameLibrary,
    private val optimizer: PuOptimizer,
    private val deviceProbe: PuDeviceProbe,
) : ViewModel() {

    private val _state = MutableStateFlow(PuOptimizerState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuOptimizerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var cancellation: AtomicBoolean? = null
    private var safetyPauseReason: String? = null

    init {
        refresh()
    }

    fun onAction(action: PuOptimizerAction) {
        when (action) {
            PuOptimizerAction.OnStart -> start()
            PuOptimizerAction.OnPause -> pause()
            PuOptimizerAction.OnResume -> start()
            PuOptimizerAction.OnRefresh -> refresh()
        }
    }

    private fun refresh() {
        viewModelScope.launch {
            try {
                val game = withContext(Dispatchers.IO) { library.getGame(gameId) }
                    ?: throw IllegalStateException("The game is no longer in the library")
                val recommendedProfile = withContext(Dispatchers.Default) {
                    deviceProbe.recommend(deviceProbe.snapshot()).profile
                }
                val manifest = withContext(Dispatchers.IO) { optimizer.manifest(gameId) }
                val cacheBytes = withContext(Dispatchers.IO) { optimizer.cacheSize(gameId) }
                _state.update {
                    it.copy(
                        phase = if (manifest == null) PuOptimizerPhase.IDLE else PuOptimizerPhase.COMPLETED,
                        gameTitle = game.title,
                        profile = game.selectedProfile ?: manifest?.profile ?: recommendedProfile,
                        totalAssets = manifest?.assets?.size ?: 0,
                        completedAssets = manifest?.assets?.size ?: 0,
                        savedBytes = manifest?.assets?.sumOf { asset ->
                            if (asset.outputPath == null) 0L else (asset.sourceSizeBytes - asset.outputSizeBytes).coerceAtLeast(0)
                        } ?: 0,
                        sourceBytes = manifest?.sourceBytes ?: 0,
                        cacheBytes = cacheBytes,
                        manifest = manifest,
                        originalFilesUnchanged = null,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Optimization load failed", e)
                _state.update {
                    it.copy(
                        phase = PuOptimizerPhase.ERROR,
                        errorMessage = "PU could not prepare optimization for this game.",
                    )
                }
            }
        }
    }

    private fun start() {
        if (_state.value.phase == PuOptimizerPhase.RUNNING) return
        viewModelScope.launch {
            try {
                val game = withContext(Dispatchers.IO) { library.getGame(gameId) }
                    ?: throw IllegalStateException("The game is no longer in the library")
                val recommendedProfile = withContext(Dispatchers.Default) {
                    deviceProbe.recommend(deviceProbe.snapshot()).profile
                }
                val profile = game.selectedProfile ?: recommendedProfile
                val device = withContext(Dispatchers.Default) { deviceProbe.snapshot() }
                optimizationBlockReason(device)?.let { reason ->
                    _state.update {
                        it.copy(phase = PuOptimizerPhase.PAUSED, errorMessage = reason)
                    }
                    return@launch
                }

                val runCancellation = AtomicBoolean(false)
                cancellation = runCancellation
                safetyPauseReason = null
                _state.update {
                    it.copy(
                        phase = PuOptimizerPhase.RUNNING,
                        profile = profile,
                        completedAssets = 0,
                        savedBytes = 0,
                        sourceBytes = 0,
                        currentAsset = "",
                        errorMessage = null,
                    )
                }

                val result = withContext(Dispatchers.IO) {
                    optimizer.optimize(
                        game = game,
                        profile = profile,
                        lite = edition == PuEdition.LITE,
                        cancellation = runCancellation,
                    ) { progress ->
                        _state.update {
                            it.copy(
                                completedAssets = progress.completedAssets,
                                totalAssets = progress.totalAssets,
                                savedBytes = progress.savedBytes,
                                sourceBytes = progress.completedBytes,
                                currentAsset = progress.currentPath,
                            )
                        }
                        if (progress.completedAssets % SAFETY_CHECK_INTERVAL == 0) {
                            val reason = deviceProbe.snapshot().optimizationBlockReason()
                            if (reason != null) {
                                safetyPauseReason = reason
                                runCancellation.set(true)
                            }
                        }
                    }
                }

                withContext(Dispatchers.IO) {
                    library.setProfile(gameId, profile)
                    library.setCacheSize(gameId, result.cacheBytes)
                }
                _state.update {
                    it.copy(
                        phase = PuOptimizerPhase.COMPLETED,
                        completedAssets = result.manifest.assets.size,
                        totalAssets = result.manifest.assets.size,
                        savedBytes = result.manifest.assets.sumOf { asset ->
                            if (asset.outputPath == null) 0L else (asset.sourceSizeBytes - asset.outputSizeBytes).coerceAtLeast(0)
                        },
                        sourceBytes = result.manifest.sourceBytes,
                        cacheBytes = result.cacheBytes,
                        manifest = result.manifest,
                        originalFilesUnchanged = result.originalFilesUnchanged,
                        errorMessage = null,
                    )
                }
                _events.send(PuOptimizerEvent.Completed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: PuOptimizationCancelledException) {
                _state.update {
                    it.copy(
                        phase = PuOptimizerPhase.PAUSED,
                        errorMessage = safetyPauseReason ?: "Optimization paused. Resume when the phone is ready.",
                    )
                }
            } catch (e: Exception) {
                Log.w(TAG, "Optimization failed", e)
                _state.update {
                    it.copy(
                        phase = PuOptimizerPhase.ERROR,
                        errorMessage = "PU could not optimize this game. The original files were not changed.",
                    )
                }
            } finally {
                cancellation = null
            }
        }
    }

    private fun pause() {
        if (_state.value.phase != PuOptimizerPhase.RUNNING) return
        cancellation?.set(true)
        _state.update {
            it.copy(errorMessage = "Pausing after the current asset…")
        }
    }

    override fun onCleared() {
        cancellation?.set(true)
        super.onCleared()
    }

    private companion object {
        const val TAG = "PuOptimizer"
        const val SAFETY_CHECK_INTERVAL = 25
    }
}
