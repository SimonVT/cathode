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
import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import net.simonvt.cathode.jobqueue.database.JobDatabase;
import net.simonvt.cathode.jobqueue.internal.JobColumns;
import net.simonvt.cathode.jobqueue.internal.JobDatabase.Tables;
import timber.log.Timber;

public final class JobManager {

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  private Context context;

  private JobInjector jobInjector;

  private JobDatabase database;

  private Converter converter;

  private final ArrayList<Job> jobs = new ArrayList<>();

  private final SerialExecutor serialExecutor;

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

  private void loadJobs() {
    synchronized (jobs) {
      SQLiteDatabase db = database.getReadableDatabase();
      Cursor c = db.query(Tables.JOBS, null, null, null, null, null, null);

      while (c.moveToNext()) {
        byte[] bytes = c.getBlob(c.getColumnIndex(JobColumns.JOB));
        Job job = converter.from(bytes);
        addJobInternal(job);
      }

      c.close();

      if (jobs.size() > 0) {
        startService();
      }

      Timber.d("Loaded " + jobs.size() + " jobs");
    }
  }

  private boolean isMoreImportantThan(Job job1, Job job2) {
    if (job1.requiresWakelock() && !job2.requiresWakelock()) {
      return true;
    }

    if (!job1.requiresWakelock() && job2.requiresWakelock()) {
      return false;
    }

    return job1.getPriority() > job2.getPriority();
  }

  public void addJob(final Job job) {
    if (job.requiresWakelock()) {
      JobService.acquireLock(context);
    }

    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        Timber.d("Adding job: " + job.getClass().getSimpleName());

        synchronized (jobs) {
          final String key = job.key();
          for (Job existingJob : jobs) {
            if (key.equals(existingJob.key())) {
              Timber.d("Job " + key + " matched " + existingJob.key());
              return;
            }
          }

          addJobInternal(job);

          Timber.d("Added job " + job.getClass().getSimpleName() + ", now there's " + jobs.size());
        }

        startService();

        persistJob(job);
      }
    });
  }

  private void addJobInternal(Job job) {
    boolean added = false;

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

  private synchronized void startService() {
    MAIN_HANDLER.post(new Runnable() {
      @Override public void run() {
        Intent intent = new Intent(context, JobService.class);
        context.startService(intent);
      }
    });
  }

  private void persistJob(Job job) {
    ContentValues values = new ContentValues();
    values.put(JobColumns.KEY, job.key());
    byte[] bytes = converter.to(job);
    values.put(JobColumns.JOB, bytes);
    values.put(JobColumns.JOB_NAME, job.getClass().getName());
    values.put(JobColumns.FLAGS, job.getFlags());

    SQLiteDatabase db = this.database.getWritableDatabase();
    db.insert(Tables.JOBS, null, values);
  }

  void jobDone(final Job job) {
    synchronized (jobs) {
      jobs.remove(job);
      Timber.d("Finished job " + job.getClass().getSimpleName() + ", " + jobs.size() + " left");
    }
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

  void jobFailed(Job job) {
    Timber.d("Job failed: " + job.getClass().getSimpleName());
  }

  boolean hasJobs() {
    synchronized (jobs) {
      return jobs.size() > 0;
    }
  }

  Job nextJob() {
    synchronized (jobs) {
      if (jobs.size() > 0) {
        Job job = jobs.get(0);
        jobInjector.injectInto(job);
        return job;
      }

      return null;
    }
  }

  public void removeJobsWithFlag(final int flag) {
    serialExecutor.execute(new Runnable() {
      @Override public void run() {
        synchronized (jobs) {
          for (int i = jobs.size() - 1; i >= 0; i--) {
            Job job = jobs.get(i);
            if (job.hasFlag(flag)) {
              Timber.d("Removing job: " + job.key());
              jobs.remove(i);
            }
          }
        }
      }
    });
  }
}
