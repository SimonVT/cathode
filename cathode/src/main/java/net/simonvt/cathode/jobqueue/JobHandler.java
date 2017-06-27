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
import net.simonvt.cathode.util.MainHandler;
import timber.log.Timber;

public class JobHandler {

  /**
   * Time to wait before stopping execution after all listeners are removed.
   */
  private static final long STOP_DELAY = 2 * DateUtils.SECOND_IN_MILLIS;

  /**
   * Execution is restarted after this delay on failure.
   */
  private static final long FAILURE_DELAY = DateUtils.MINUTE_IN_MILLIS;

  public interface JobHandlerListener {

    void onQueueEmpty();

    void onQueueFailed();
  }

  @Inject Context context;
  @Inject JobManager jobManager;

  private JobExecutor executor;

  private List<JobHandlerListener> listeners = new ArrayList<>();

  private boolean running = false;

  public JobHandler(int withFlags, int withoutFlags, int threadCount) {
    Injector.obtain().inject(this);
    executor = new JobExecutor(jobManager, executorListener, threadCount, withFlags, withoutFlags);
    executor.start();
    running = executor.hasJobs();
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
      dispatchQueueFailed();
    }
  };

  public boolean hasJobs() {
    return executor.hasJobs();
  }

  private void dispatchQueueEmpty() {
    for (JobHandlerListener listener : listeners) {
      listener.onQueueEmpty();
    }
  }

  private void dispatchQueueFailed() {
    MainHandler.postDelayed(resumeRunnable, FAILURE_DELAY);

    for (JobHandlerListener listener : listeners) {
      listener.onQueueFailed();
    }
  }

  private void start() {
    Timber.d("[start]");
    executor.start();
    onStart();
  }

  private void resume() {
    Timber.d("[resume] %b", running);
    if (!running) {
      running = true;
      MainHandler.removeCallbacks(resumeRunnable);
      MainHandler.removeCallbacks(stopRunnable);
      onResume();
    }
  }

  private void pause() {
    Timber.d("[pause] %b", running);
    if (running) {
      running = false;
      MainHandler.removeCallbacks(resumeRunnable);
      MainHandler.removeCallbacks(stopRunnable);
      onPause();
    }
  }

  private void stop() {
    Timber.d("[stop]");
    executor.stop();
    pause();
    onStop();
  }

  protected void onStart() {
  }

  protected void onResume() {
  }

  protected void onPause() {
  }

  protected void onStop() {
  }

  public void registerListener(JobHandlerListener listener) {
    listeners.add(listener);
    MainHandler.removeCallbacks(resumeRunnable);
    MainHandler.removeCallbacks(stopRunnable);
    if (listeners.size() == 1) {
      start();
    }

    Timber.d("%d listeners, resuming", listeners.size());
  }

  public void unregisterListener(JobHandlerListener listener) {
    listeners.remove(listener);

    if (listeners.isEmpty()) {
      Timber.d("No more listeners, posting stop");
      MainHandler.postDelayed(stopRunnable, STOP_DELAY);
    } else {
      Timber.d("%d listeners", listeners.size());
    }
  }

  private Runnable resumeRunnable = new Runnable() {
    @Override public void run() {
      resume();
    }
  };

  private Runnable stopRunnable = new Runnable() {
    @Override public void run() {
      stop();
    }
  };
}
