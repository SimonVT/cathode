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

import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.Executor;

final class SerialExecutor {

  public static SerialExecutor newInstance() {
    return new SerialExecutor();
  }

  private SerialExecutor() {
  }

  public void execute(Runnable r) {
    serialExecutor.execute(r);
  }

  private final SerialExecutorImpl serialExecutor = new SerialExecutorImpl();

  private static class SerialExecutorImpl implements Executor {

    final Queue<Runnable> tasks = new ArrayDeque<Runnable>();

    private Runnable active;

    private Executor executor = new Executor() {
      @Override public void execute(Runnable r) {
        new Thread(r).start();
      }
    };

    public synchronized void execute(final Runnable r) {
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
        executor.execute(active);
      }
    }
  }
}
