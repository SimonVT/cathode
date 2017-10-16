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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.util.MainHandler;
import net.simonvt.cathode.jobqueue.JobDatabaseSchematic.Tables;
import net.simonvt.cathode.jobqueue.database.JobDatabase;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public final class JobManager {

  private Context context;

  private JobInjector jobInjector;

  private JobDatabase database;

  private Converter converter;

  private final List<Job> jobs = new ArrayList<>();
  private final List<String> checkedOutKeys = new ArrayList<>();

  private final SerialExecutor serialExecutor;

  private List<WeakReference<JobListener>> jobListeners = new ArrayList<>();

  public JobManager(Context context, JobInjector jobInjector) {
    this.context = context.getApplicationContext();
    this.jobInjector = jobInjector;

    database = JobDatabase.getInstance(this.context);
    converter = new Converter();

    serialExecutor = SerialExecutor.newInstance();
    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        loadJobs();
      }
    });
  }

  public void addJobListener(JobListener listener) {
    WeakReference<JobListener> jobListener = new WeakReference<>(listener);
    jobListeners.add(jobListener);
  }

  public void removeJobListener(JobListener listener) {
    for (int i = jobListeners.size() - 1; i >= 0; i--) {
      WeakReference<JobListener> listenerRef = jobListeners.get(i);
      JobListener jobListener = listenerRef.get();
      if (jobListener == null || listener == jobListener) {
        jobListeners.remove(listenerRef);
      }
    }
  }

  private synchronized void postOnJobsLoaded() {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        for (int i = jobListeners.size() - 1; i >= 0; i--) {
          WeakReference<JobListener> listenerRef = jobListeners.get(i);
          JobListener jobListener = listenerRef.get();

          if (jobListener == null) {
            jobListeners.remove(listenerRef);
          } else {
            jobListener.onJobsLoaded(JobManager.this);
          }
        }
      }
    });
  }

  private synchronized void postOnJobAdded(final Job job) {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        for (int i = jobListeners.size() - 1; i >= 0; i--) {
          WeakReference<JobListener> listenerRef = jobListeners.get(i);
          JobListener jobListener = listenerRef.get();

          if (jobListener == null) {
            jobListeners.remove(listenerRef);
          } else {
            jobListener.onJobAdded(JobManager.this, job);
          }
        }
      }
    });
  }

  private synchronized void postOnJobRemoved(final Job job) {
    MainHandler.post(new Runnable() {
      @Override public void run() {
        for (int i = jobListeners.size() - 1; i >= 0; i--) {
          WeakReference<JobListener> listenerRef = jobListeners.get(i);
          JobListener jobListener = listenerRef.get();

          if (jobListener == null) {
            jobListeners.remove(listenerRef);
          } else {
            jobListener.onJobRemoved(JobManager.this, job);
          }
        }
      }
    });
  }

  private void loadJobs() {
    synchronized (jobs) {
      SQLiteDatabase db = database.getReadableDatabase();
      Cursor c = db.query(Tables.JOBS, null, null, null, null, null, null);

      while (c.moveToNext()) {
        byte[] bytes = Cursors.getBlob(c, JobColumns.JOB);
        Job job = converter.from(bytes);
        addJobInternal(job);
      }

      c.close();

      postOnJobsLoaded();

      Timber.d("Loaded %d jobs", jobs.size());
    }
  }

  private boolean isMoreImportantThan(Job job1, Job job2) {
    return job1.getPriority() > job2.getPriority();
  }

  public void addJob(final Job job) {
    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        addJobNow(job);
      }
    });
  }

  public void addJobNow(final Job job) {
    Timber.d("Adding job: %s", job.key());
    postOnJobAdded(job);

    synchronized (jobs) {
      if (!job.allowDuplicates()) {
        final String key = job.key();
        for (Job existingJob : jobs) {
          if (key.equals(existingJob.key())) {
            List<WeakReference<Job.OnDoneListener>> listeners = job.getOnDoneRefs();
            if (listeners != null) {
              for (WeakReference<Job.OnDoneListener> ref : listeners) {
                Job.OnDoneListener listener = ref.get();
                if (listener != null) {
                  existingJob.registerOnDoneListener(listener);
                }
              }
            }
            Timber.d("Job %s matched %s", key, existingJob.key());
            return;
          }
        }
      }

      addJobInternal(job);
    }

    persistJob(job);
  }

  private void addJobInternal(Job job) {
    synchronized (jobs) {
      boolean added = false;

      jobInjector.injectInto(job);

      for (int i = 0; i < jobs.size(); i++) {
        if (isMoreImportantThan(job, jobs.get(i))) {
          added = true;
          jobs.add(i, job);
          break;
        }
      }

      if (!added) {
        jobs.add(job);
      }
    }
  }

  private void persistJob(Job job) {
    ContentValues values = new ContentValues();
    values.put(JobColumns.KEY, job.key());
    byte[] bytes = converter.to(job);
    values.put(JobColumns.JOB, bytes);
    values.put(JobColumns.JOB_NAME, job.getClass().getName());
    values.put(JobColumns.FLAGS, job.getFlags());

    SQLiteDatabase db = database.getWritableDatabase();
    db.insert(Tables.JOBS, null, values);
  }

  public Job checkoutJob(int withFlags, int withoutFlags) {
    synchronized (jobs) {
      for (Job job : jobs) {
        if (!job.isCheckedOut()) {
          if (withFlags != 0 && !job.hasFlags(withFlags)) {
            continue;
          }

          if (withoutFlags != 0 && job.hasFlags(withoutFlags)) {
            continue;
          }

          if (checkedOutKeys.contains(job.key())) {
            continue;
          }

          checkedOutKeys.add(job.key());
          job.setCheckedOut(true);
          return job;
        }
      }

      return null;
    }
  }

  public void checkinJob(Job job) {
    job.setCheckedOut(false);
    checkedOutKeys.remove(job.key());
  }

  public void removeJob(final Job job) {
    synchronized (jobs) {
      Timber.d("Removing job: %s", job.key());
      jobs.remove(job);
      checkedOutKeys.remove(job.key());
      postOnJobRemoved(job);
    }

    removeJobFromDatabase(job);
  }

  private void removeJobFromDatabase(final Job job) {
    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        SQLiteDatabase db = database.getWritableDatabase();
        db.delete(Tables.JOBS, JobColumns.JOB_NAME + "=? AND " + JobColumns.KEY + "=?",
            new String[] {
                job.getClass().getName(), job.key(),
            });
      }
    });
  }

  public boolean hasJobs(int withFlags, int withoutFlags) {
    synchronized (jobs) {
      for (Job job : jobs) {
        if (withFlags != 0 && !job.hasFlags(withFlags)) {
          continue;
        }

        if (withoutFlags != 0 && job.hasFlags(withoutFlags)) {
          continue;
        }

        return true;
      }

      return false;
    }
  }

  public void removeJobsWithFlag(final int flag) {
    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        synchronized (jobs) {
          for (int i = jobs.size() - 1; i >= 0; i--) {
            Job job = jobs.get(i);
            if (job.hasFlags(flag)) {
              jobs.remove(i);
              checkedOutKeys.remove(job.key());
              removeJobFromDatabase(job);
            }
          }
        }
      }
    });
  }

  public int jobCount() {
    synchronized (jobs) {
      return jobs.size();
    }
  }

  public void clear() {
    serialExecutor.clear();
    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        synchronized (jobs) {
          jobs.clear();

          SQLiteDatabase db = database.getWritableDatabase();
          db.delete(Tables.JOBS, null, null);
        }
      }
    });
  }
}
