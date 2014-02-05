/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.remote;

import android.content.ContentResolver;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.queue.Task;
import net.simonvt.cathode.remote.TraktTask.TaskCallback;
import net.simonvt.cathode.util.HttpUtils;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

public abstract class TraktTask extends Task<TaskCallback> {

  public interface TaskCallback {

    void onSuccess();

    void onFailure();
  }

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Inject transient TraktTaskQueue queue;

  @Inject @PriorityQueue transient TraktTaskQueue priorityQueue;

  private transient volatile boolean canceled;

  @Override public void execute(Context context, TaskCallback callback) {
    super.execute(context, callback);

    CathodeApp.inject(context, this);

    try {
      Timber.tag(TraktTask.this.getClass().getSimpleName()).d("doTask");
      doTask();
    } catch (RetrofitError e) {
      e.printStackTrace();
      logError(e);
      postOnFailure();
    }
  }

  protected abstract void doTask();

  protected void logError(RetrofitError e) {
    try {
      String url = e.getUrl();
      Response response = e.getResponse();
      if (response != null) {
        int statusCode = response.getStatus();
        if (statusCode < 500) { // Skip server errors
          String body = HttpUtils.streamToString(response.getBody().in());
          Timber.i("URL: %s", url);
          Timber.i("Status code: %d", statusCode);
          Timber.i("Body:");
          Timber.i(body);
          Timber.e(e, null);
        } else {
          String body = HttpUtils.streamToString(response.getBody().in());
          Timber.d("URL: %s", url);
          Timber.d("Status code: %d", statusCode);
          Timber.d("Body:");
          Timber.d(body);
          Timber.d(e, null);
        }
      }
    } catch (Throwable t) {
      // Ignore
    }
  }

  public void cancel() {
    synchronized (this) {
      canceled = true;
    }
  }

  protected Context getContext() {
    return context;
  }

  protected ContentResolver getContentResolver() {
    return context.getContentResolver();
  }

  protected final void queueTask(final TraktTask task) {
    Timber.d("Queueing task: " + task.getClass().getSimpleName());
    synchronized (this) {
      if (!canceled) queue.add(task);
    }
  }

  protected final void queuePriorityTask(final TraktTask task) {
    Timber.d("Queueing priority task: " + task.getClass().getSimpleName());
    synchronized (this) {
      if (!canceled) priorityQueue.add(task);
    }
  }

  protected final void postOnSuccess() {
    synchronized (this) {
      if (!canceled && callback != null) {
        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            callback.onSuccess();
          }
        });
      }
    }
  }

  protected void postOnFailure() {
    synchronized (this) {
      if (!canceled && callback != null) {
        MAIN_HANDLER.post(new Runnable() {
          @Override public void run() {
            callback.onFailure();
          }
        });
      }
    }
  }
}
