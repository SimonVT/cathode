package net.simonvt.cathode.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import javax.inject.Inject
import javax.inject.Provider

class CathodeViewModelFactory @Inject constructor(
  private val creators: Map<Class<out ViewModel>, @JvmSuppressWildcards Provider<ViewModel>>
) : ViewModelProvider.Factory {

  @Suppress("UNCHECKED_CAST")
  override fun <T : ViewModel?> create(modelClass: Class<T>): T {
    val foundEntry = creators.entries.find { modelClass.isAssignableFrom(it.key) }
    val provider = foundEntry?.value
      ?: throw IllegalArgumentException("unknown ViewModel class: ${modelClass.simpleName}")
    return provider.get() as T
  }
}
