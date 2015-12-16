/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.simonvt.cathode.util.MainHandler;
import timber.log.Timber;

public class JobExecutor {

  public interface JobExecutorListener {

    void onQueueEmpty();

    void onQueueFailed();
  }

  private JobManager jobManager;

  private JobExecutorListener executorListener;

  private int threadCount;

  private int withFlags;

  private int withoutFlags;

  private ExecutorService executor;

  private boolean paused;

  private boolean destroyed;

  private boolean halt;

  private final Object lock;

  private int runningJobCount;

  public JobExecutor(JobManager jobManager, JobExecutorListener executorListener, int threadCount,
      int withFlags, int withoutFlags) {
    this.jobManager = jobManager;
    this.executorListener = executorListener;
    this.threadCount = threadCount;
    this.withFlags = withFlags;
    this.withoutFlags = withoutFlags;

    executor = Executors.newFixedThreadPool(threadCount);

    lock = new Object();

    jobManager.addJobListener(jobListener);

    startJobs();
  }

  public void pause() {
    synchronized (lock) {
      paused = true;
    }
  }

  public void resume() {
    synchronized (lock) {
      paused = false;
      startJobs();
    }
  }

  public void destroy() {
    synchronized (lock) {
      destroyed = true;
      executor.shutdown();
    }
  }

  private JobListener jobListener = new JobListener() {
    @Override public void onJobsLoaded(JobManager jobManager) {
    }

    @Override public void onJobAdded(JobManager jobManager, Job job) {
      if (!destroyed) {
        startJobs();
      }
    }

    @Override public void onJobRemoved(JobManager jobManager, Job job) {
    }
  };

  private void postQueueEmpty() {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        if (destroyed) {
          return;
        }

        // Jobs might have been posted since postQueueEmpty was called,
        // can happen if last job in the queue posts additional jobs.
        if (jobManager.hasJobs(withFlags, withoutFlags)) {
          startJobs();
        } else if (executorListener != null) {
          executorListener.onQueueEmpty();
        }
      }
    });
  }

  private void postQueueFailed() {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        if (executorListener != null) {
          executorListener.onQueueFailed();
        }
      }
    });
  }

  private void startJobs() {
    synchronized (lock) {
      if (!paused) {
        if (!halt) {
          while (runningJobCount < threadCount) {
            Job job = jobManager.checkoutJob(withFlags, withoutFlags);
            if (job != null) {
              Timber.d("Queueing job: %s", job.key());
              executor.execute(new JobRunnable(job));
              runningJobCount++;
            } else {
              break;
            }
          }
        } else {
          Timber.d("Queue is halted, not starting");
        }
      } else {
        Timber.d("Queue paused, not starting");
      }
    }
  }

  private void jobSucceeded(Job job) {
    synchronized (lock) {
      Timber.d("Job succeded: %s", job.key());

      jobManager.removeJob(job);
      runningJobCount--;

      if (destroyed) {
        return;
      }

      if (halt) {
        halt = false;
      }

      if (jobManager.hasJobs(withFlags, withoutFlags)) {
        startJobs();
      } else if (runningJobCount == 0) {
        postQueueEmpty();
      }
    }
  }

  private void jobFailed(Job job, Throwable t) {
    synchronized (lock) {
      if (!(t instanceof JobFailedException)) {
        Timber.e(t, "Job failed: %s", job.key());
      } else {
        Timber.d(t, "Job failed: %s", job.key());
      }

      halt = true;
      jobManager.checkinJob(job);
      runningJobCount--;

      if (destroyed) {
        return;
      }

      if (runningJobCount == 0) {
        postQueueFailed();
      }
    }
  }

  private class JobRunnable implements Runnable {

    private Job job;

    public JobRunnable(Job job) {
      this.job = job;
    }

    @Override public void run() {
      try {
        Timber.d("Executing job: %s", job.key());
        job.perform();
        jobSucceeded(job);
      } catch (Throwable t) {
        jobFailed(job, t);
      } finally {
        job.done();
      }
    }
  }
}
