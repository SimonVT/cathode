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
package net.simonvt.cathode.scheduler;

import android.content.Context;
import android.support.annotation.NonNull;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;

public class BaseTaskScheduler {

  private static final Executor EXECUTOR = new Executor() {
    @Override public void execute(@NonNull Runnable r) {
      new Thread(r).start();
    }
  };

  @Inject JobManager jobManager;

  protected Context context;

  public BaseTaskScheduler(Context context) {
    Injector.inject(this);
    this.context = context;
  }

  protected final void queue(final Job task) {
    jobManager.addJob(task);
  }

  protected void execute(Runnable r) {
    SERIAL_EXECUTOR.execute(r);
  }

  private static final SerialExecutor SERIAL_EXECUTOR = new SerialExecutor();

  private static class SerialExecutor implements Executor {

    final Queue<Runnable> tasks = new ArrayDeque<>();
    Runnable active;

    public synchronized void execute(@NonNull final Runnable r) {
      tasks.offer(new Runnable() {
        @Override public void run() {
          try {
            r.run();
          } finally {
            scheduleNext();
          }
        }
      });
      if (active == null) {
        scheduleNext();
      }
    }

    protected synchronized void scheduleNext() {
      if ((active = tasks.poll()) != null) {
        EXECUTOR.execute(active);
      }
    }
  }
}
