package net.simonvt.cathode.ui

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

abstract class RefreshableViewModel : ViewModel() {

  private val _loading = MutableLiveData<Boolean>()
  val loading: LiveData<Boolean> get() = _loading

  fun refresh() {
    viewModelScope.launch {
      _loading.postValue(true)
      withContext(Dispatchers.IO) {
        try {
          onRefresh()
        } catch (t: Throwable) {
          Timber.d(t)
          // TODO: Notify Fragment
        }
      }
      _loading.postValue(false)
    }
  }

  protected abstract suspend fun onRefresh()
}
