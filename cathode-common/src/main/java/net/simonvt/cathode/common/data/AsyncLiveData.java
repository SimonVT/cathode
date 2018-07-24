package net.simonvt.cathode.common.data;

import android.os.AsyncTask;
import androidx.annotation.Nullable;
import androidx.core.os.OperationCanceledException;
import androidx.lifecycle.LiveData;

public abstract class AsyncLiveData<D> extends LiveData<D> {

  @Override protected void onActive() {
    loadData();
  }

  private LoadTask mTask;
  private LoadTask mPendingTask;

  public void loadData() {
    if (mTask != null) {
      mTask.cancel(false);
      mTask = null;
    }

    mTask = new LoadTask();
    mTask.execute();
  }

  void postResult(LoadTask task, D data) {
    if (task != mTask) {
      throw new RuntimeException("Old task tried to post data");
    }

    postValue(data);
  }

  final class LoadTask extends AsyncTask<Void, Void, D> {

    @Override protected D doInBackground(Void... params) {
      try {
        D data = loadInBackground();
        return data;
      } catch (OperationCanceledException ex) {
        return null;
      }
    }

    @Override protected void onPostExecute(D data) {
      if (!isCancelled()) {
        postResult(this, data);
      }
    }
  }

  @Nullable protected abstract D loadInBackground();
}
