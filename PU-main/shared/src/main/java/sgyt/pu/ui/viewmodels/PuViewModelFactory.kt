package sgyt.pu.ui.viewmodels

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider

/**
 * Minimal factory for the project's constructor-injected ViewModels.
 *
 * The project has no annotation-processing DI framework, so each screen builds
 * its ViewModel with this factory instead of a framework-specific injector.
 */
class PuViewModelFactory(private val creator: () -> ViewModel) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = creator() as T
}
