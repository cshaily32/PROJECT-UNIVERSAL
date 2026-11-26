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
import sgyt.pu.core.model.PuRuntimeDescriptor
import sgyt.pu.core.model.PuRuntimeInstallationState
import sgyt.pu.core.runtime.PuRuntimeLibrary
import sgyt.pu.ui.model.PuRuntimeInfo
import sgyt.pu.ui.model.toRuntimeInfo
import java.util.concurrent.atomic.AtomicBoolean

data class PuRuntimeDownloadState(
    val screenState: ScreenState = ScreenState.Loading,
    val runtime: PuRuntimeInfo? = null,
    val installationState: PuRuntimeInstallationState = PuRuntimeInstallationState.NOT_INSTALLED,
    val downloadedBytes: Long = 0,
    val totalBytes: Long = 0,
    val resumedFromBytes: Long = 0,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }

    val progress: Float
        get() = if (totalBytes > 0) (downloadedBytes.toFloat() / totalBytes).coerceIn(0f, 1f) else 0f
}

sealed interface PuRuntimeDownloadAction {
    data object OnDownload : PuRuntimeDownloadAction
    data object OnCancel : PuRuntimeDownloadAction
    data object OnRemove : PuRuntimeDownloadAction
    data object OnRefresh : PuRuntimeDownloadAction
}

sealed interface PuRuntimeDownloadEvent {
    data object Installed : PuRuntimeDownloadEvent
}

/** Downloads one verified runtime and keeps partial transfers available for resume. */
class PuRuntimeDownloadViewModel(
    private val runtimeId: String,
    private val revision: Long,
    private val runtimeLibrary: PuRuntimeLibrary,
    private val deviceProbe: PuDeviceProbe,
) : ViewModel() {

    private val _state = MutableStateFlow(PuRuntimeDownloadState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuRuntimeDownloadEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var descriptor: PuRuntimeDescriptor? = null
    private var downloadCancellation: AtomicBoolean? = null

    init {
        load()
    }

    fun onAction(action: PuRuntimeDownloadAction) {
        when (action) {
            PuRuntimeDownloadAction.OnDownload -> download()
            PuRuntimeDownloadAction.OnCancel -> cancel()
            PuRuntimeDownloadAction.OnRemove -> remove()
            PuRuntimeDownloadAction.OnRefresh -> load()
        }
    }

    private fun load() {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    screenState = PuRuntimeDownloadState.ScreenState.Loading,
                    errorMessage = null,
                )
            }
            try {
                val runtimeDescriptor = withContext(Dispatchers.IO) {
                    runtimeLibrary.refreshCatalog()
                    runtimeLibrary.getRuntime(runtimeId, revision)
                } ?: throw IllegalStateException("The selected runtime is no longer in the catalog")
                descriptor = runtimeDescriptor
                val snapshot = withContext(Dispatchers.Default) { deviceProbe.snapshot() }
                val installed = withContext(Dispatchers.IO) {
                    runtimeLibrary.listInstalled()
                        .firstOrNull { record ->
                            record.runtimeId == runtimeDescriptor.id &&
                                record.revision == runtimeDescriptor.catalogRevision
                        }
                        ?.takeIf { runtimeLibrary.isInstalled(runtimeDescriptor) }
                }
                val runtimeInfo = runtimeDescriptor.toRuntimeInfo(snapshot, installed)
                _state.update {
                    it.copy(
                        screenState = PuRuntimeDownloadState.ScreenState.Success,
                        runtime = runtimeInfo,
                        installationState = if (installed == null) {
                            PuRuntimeInstallationState.NOT_INSTALLED
                        } else {
                            PuRuntimeInstallationState.INSTALLED
                        },
                        downloadedBytes = if (installed == null) 0 else runtimeDescriptor.compressedBytes,
                        totalBytes = runtimeDescriptor.compressedBytes,
                        resumedFromBytes = 0,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Runtime load failed", e)
                _state.update {
                    it.copy(
                        screenState = PuRuntimeDownloadState.ScreenState.Error,
                        errorMessage = "This runtime is no longer available in the signed catalog.",
                    )
                }
            }
        }
    }

    private fun download() {
        val runtimeDescriptor = descriptor ?: return
        val current = _state.value
        if (current.screenState != PuRuntimeDownloadState.ScreenState.Success) return
        if (current.runtime?.supported != true) return
        if (current.installationState == PuRuntimeInstallationState.DOWNLOADING) return
        if (current.installationState == PuRuntimeInstallationState.INSTALLED) return

        val cancellation = AtomicBoolean(false)
        downloadCancellation = cancellation
        _state.update {
            it.copy(
                installationState = PuRuntimeInstallationState.DOWNLOADING,
                downloadedBytes = 0,
                totalBytes = runtimeDescriptor.compressedBytes,
                resumedFromBytes = 0,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                val installed = withContext(Dispatchers.IO) {
                    runtimeLibrary.install(runtimeDescriptor, cancellation) { progress ->
                        _state.update { state ->
                            state.copy(
                                downloadedBytes = progress.downloadedBytes,
                                totalBytes = if (progress.totalBytes > 0) {
                                    progress.totalBytes
                                } else {
                                    runtimeDescriptor.compressedBytes
                                },
                                resumedFromBytes = progress.resumedFromBytes,
                            )
                        }
                    }
                }
                _state.update {
                    it.copy(
                        runtime = it.runtime?.copy(
                            installed = true,
                            installedBytes = installed.installedBytes,
                        ),
                        installationState = PuRuntimeInstallationState.INSTALLED,
                        downloadedBytes = runtimeDescriptor.compressedBytes,
                        errorMessage = null,
                    )
                }
                _events.send(PuRuntimeDownloadEvent.Installed)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Runtime installation failed", e)
                if (cancellation.get()) {
                    _state.update {
                        it.copy(
                            installationState = PuRuntimeInstallationState.CANCELLED,
                            errorMessage = null,
                        )
                    }
                } else {
                    _state.update {
                        it.copy(
                            installationState = PuRuntimeInstallationState.FAILED,
                            errorMessage = "The package could not be downloaded or verified. PU rejected it and kept the existing runtime unchanged.",
                        )
                    }
                }
            } finally {
                if (downloadCancellation === cancellation) downloadCancellation = null
            }
        }
    }

    private fun cancel() {
        if (_state.value.installationState != PuRuntimeInstallationState.DOWNLOADING) return
        downloadCancellation?.set(true)
        _state.update {
            it.copy(
                installationState = PuRuntimeInstallationState.CANCELLED,
                errorMessage = null,
            )
        }
    }

    private fun remove() {
        if (_state.value.installationState != PuRuntimeInstallationState.INSTALLED) return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { runtimeLibrary.remove(runtimeId, revision) }
                _state.update {
                    it.copy(
                        runtime = it.runtime?.copy(installed = false),
                        installationState = PuRuntimeInstallationState.NOT_INSTALLED,
                        downloadedBytes = 0,
                        resumedFromBytes = 0,
                        errorMessage = null,
                    )
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.w(TAG, "Runtime removal failed", e)
                _state.update {
                    it.copy(errorMessage = "PU could not remove this runtime from this device.")
                }
            }
        }
    }

    override fun onCleared() {
        downloadCancellation?.set(true)
        super.onCleared()
    }

    private companion object {
        const val TAG = "PuRuntimeDownload"
    }
}
