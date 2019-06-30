package net.simonvt.cathode.common.data

import android.os.AsyncTask
import androidx.core.os.OperationCanceledException
import androidx.lifecycle.LiveData

abstract class AsyncLiveData<D> : LiveData<D>() {

  private var mTask: LoadTask? = null

  override fun onActive() {
    loadData()
  }

  fun loadData() {
    if (mTask != null) {
      mTask!!.cancel(false)
      mTask = null
    }

    mTask = LoadTask()
    mTask!!.execute()
  }

  internal fun postResult(task: LoadTask, data: D) {
    if (task != mTask) {
      throw RuntimeException("Old task tried to post data")
    }

    postValue(data)
  }

  internal inner class LoadTask : AsyncTask<Void, Void, D>() {

    override fun doInBackground(vararg params: Void): D? {
      try {
        return loadInBackground()
      } catch (ex: OperationCanceledException) {
        return null
      }
    }

    override fun onPostExecute(data: D) {
      if (!isCancelled) {
        postResult(this, data)
      }
    }
  }

  protected abstract fun loadInBackground(): D?
}
