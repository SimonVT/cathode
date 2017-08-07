/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode.jobqueue;

import android.content.Context;
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.common.util.MainHandler;
import timber.log.Timber;

public class JobHandler {

  /**
   * Time to wait before stopping execution after all listeners are removed.
   */
  private static final long STOP_DELAY = 2 * DateUtils.SECOND_IN_MILLIS;

  /**
   * Execution is restarted after this delay on failure.
   */
  private static final long FAILURE_DELAY = 30 * DateUtils.SECOND_IN_MILLIS;

  public interface JobHandlerListener {

    void onQueueEmpty();

    void onQueueFailed();
  }

  @Inject Context context;
  @Inject JobManager jobManager;

  private JobExecutor executor;

  private final List<JobHandlerListener> listeners = new ArrayList<>();

  private boolean started = false;
  private boolean resumed = false;

  public JobHandler(int withFlags, int withoutFlags, int threadCount) {
    Injector.inject(this);
    executor = new JobExecutor(jobManager, executorListener, threadCount, withFlags, withoutFlags);
  }

  private JobExecutor.JobExecutorListener executorListener = new JobExecutor.JobExecutorListener() {

    @Override public void onStartJob(Job job) {
      resume();
    }

    @Override public void onQueueEmpty() {
      Timber.d("%s queue empty", getClass().getSimpleName());
      pause();
      dispatchQueueEmpty();
    }

    @Override public void onQueueFailed() {
      Timber.d("%s queue failed", getClass().getSimpleName());
      pause();
      MainHandler.postDelayed(new Runnable() {
        @Override public void run() {
          executor.unhalt();
        }
      }, FAILURE_DELAY);
      dispatchQueueFailed();
    }
  };

  public boolean hasJobs() {
    return executor.hasJobs();
  }

  private void dispatchQueueEmpty() {
    synchronized (listeners) {
      for (int i = listeners.size() - 1; i >= 0; i--) {
        JobHandlerListener listener = listeners.get(i);
        listener.onQueueEmpty();
      }
    }
  }

  private void dispatchQueueFailed() {
    synchronized (listeners) {
      for (int i = listeners.size() - 1; i >= 0; i--) {
        JobHandlerListener listener = listeners.get(i);
        listener.onQueueFailed();
      }
    }
  }

  private void start() {
    Timber.d("[start] %b", started);
    if (!started) {
      started = true;
      executor.start();
    }
  }

  private void stop() {
    Timber.d("[stop]");
    if (started) {
      started = false;
      executor.stop();
      onStop();
    }
  }

  private void resume() {
    if (!resumed) {
      resumed = true;
      onResume();
    }
  }

  private void pause() {
    if (resumed) {
      resumed = false;
      onPause();
    }
  }

  protected void onResume() {
  }

  protected void onPause() {
  }

  protected void onStop() {
  }

  public void registerListener(JobHandlerListener listener) {
    synchronized (listeners) {
      listeners.add(listener);
      MainHandler.removeCallbacks(stopRunnable);
      start();
      Timber.d("%d listeners, resuming", listeners.size());
    }
  }

  public void unregisterListener(JobHandlerListener listener) {
    synchronized (listeners) {
      listeners.remove(listener);

      if (listeners.isEmpty()) {
        Timber.d("No more listeners, posting stop");
        MainHandler.postDelayed(stopRunnable, STOP_DELAY);
      } else {
        Timber.d("%d listeners", listeners.size());
      }
    }
  }

  private Runnable stopRunnable = new Runnable() {
    @Override public void run() {
      stop();
    }
  };
}
