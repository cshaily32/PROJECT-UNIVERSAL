package sgyt.pu.ui.viewmodels

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
import sgyt.pu.core.runtime.PuRuntimeLibrary
import sgyt.pu.ui.model.PuRuntimeFilter
import sgyt.pu.ui.model.PuRuntimeInfo
import sgyt.pu.ui.model.toRuntimeInfo

data class PuRuntimesState(
    val screenState: ScreenState = ScreenState.Loading,
    val runtimes: List<PuRuntimeInfo> = emptyList(),
    val filter: PuRuntimeFilter = PuRuntimeFilter.ALL,
    val isRefreshingCatalog: Boolean = false,
    val errorMessage: String? = null,
) {
    sealed interface ScreenState {
        data object Loading : ScreenState
        data object Success : ScreenState
        data object Error : ScreenState
    }

    val visibleRuntimes: List<PuRuntimeInfo>
        get() = runtimes.filter { runtime ->
            when (filter) {
                PuRuntimeFilter.ALL -> true
                PuRuntimeFilter.RENPY -> runtime.engine == "Ren'Py"
                PuRuntimeFilter.RPG_MAKER -> runtime.engine.startsWith("RPG Maker")
                PuRuntimeFilter.INSTALLED -> runtime.installed
            }
        }
}

sealed interface PuRuntimesAction {
    data object OnRefresh : PuRuntimesAction

    data object OnRefreshCatalog : PuRuntimesAction

    data class OnFilterSelected(val filter: PuRuntimeFilter) : PuRuntimesAction

    data class OnRemove(val runtimeId: String, val revision: Long) : PuRuntimesAction
}

sealed interface PuRuntimesEvent {
    data object CatalogUpdated : PuRuntimesEvent
    data class ShowMessage(val message: String) : PuRuntimesEvent
}

/** Presents the signed runtime catalog and the runtimes installed on this phone. */
class PuRuntimesViewModel(
    private val runtimeLibrary: PuRuntimeLibrary,
    private val deviceProbe: PuDeviceProbe,
) : ViewModel() {

    private val _state = MutableStateFlow(PuRuntimesState())
    val state = _state.asStateFlow()

    private val _events = Channel<PuRuntimesEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        refresh()
    }

    fun onAction(action: PuRuntimesAction) {
        when (action) {
            PuRuntimesAction.OnRefresh -> refresh()
            PuRuntimesAction.OnRefreshCatalog -> refreshCatalog()
            is PuRuntimesAction.OnFilterSelected -> _state.update { it.copy(filter = action.filter) }
            is PuRuntimesAction.OnRemove -> remove(action.runtimeId, action.revision)
        }
    }

    private fun refresh() {
        viewModelScope.launch { load(showLoading = true) }
    }

    private suspend fun load(showLoading: Boolean) {
        if (showLoading) {
            _state.update { it.copy(screenState = PuRuntimesState.ScreenState.Loading, errorMessage = null) }
        }
        try {
            val snapshot = withContext(Dispatchers.Default) { deviceProbe.snapshot() }
            val runtimes = withContext(Dispatchers.IO) {
                val catalog = runtimeLibrary.listCatalog()
                val installed = runtimeLibrary.listInstalled()
                catalog.runtimes.map { descriptor ->
                    val installation = installed.firstOrNull { record ->
                        record.runtimeId == descriptor.id && record.revision == descriptor.catalogRevision
                    }?.takeIf { runtimeLibrary.isInstalled(descriptor) }
                    descriptor.toRuntimeInfo(snapshot, installation)
                }
            }
            _state.update {
                it.copy(
                    screenState = PuRuntimesState.ScreenState.Success,
                    runtimes = runtimes,
                    errorMessage = null,
                )
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            _state.update {
                it.copy(
                    screenState = PuRuntimesState.ScreenState.Error,
                    errorMessage = "PU could not read the runtime catalog on this device.",
                )
            }
        }
    }

    private fun refreshCatalog() {
        if (_state.value.isRefreshingCatalog) return
        _state.update { it.copy(isRefreshingCatalog = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { runtimeLibrary.refreshCatalog() }
                load(showLoading = false)
                _events.send(PuRuntimesEvent.CatalogUpdated)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _events.send(
                    PuRuntimesEvent.ShowMessage(
                        "The runtime catalog could not be updated. Check the API URL and signing key.",
                    ),
                )
            } finally {
                _state.update { it.copy(isRefreshingCatalog = false) }
            }
        }
    }

    private fun remove(runtimeId: String, revision: Long) {
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { runtimeLibrary.remove(runtimeId, revision) }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                _state.update {
                    it.copy(
                        screenState = PuRuntimesState.ScreenState.Error,
                        errorMessage = "PU could not remove this runtime.",
                    )
                }
                return@launch
            }
            load(showLoading = false)
        }
    }
}
