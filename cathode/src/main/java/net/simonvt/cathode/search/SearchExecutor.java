/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.search;

import java.util.concurrent.Executor;

public class SearchExecutor implements Executor {

  ExecutorRunnable active;
  ExecutorRunnable next;

  public void execute(final Runnable r) {
    synchronized (this) {
      if (next != null) {
        next.runnable.cancel();
      }

      next = new ExecutorRunnable((SearchHandler.SearchRunnable) r);

      if (active == null) {
        scheduleNext();
      }
    }
  }

  public void cancelRunning() {
    synchronized (this) {
      if (active != null) {
        active.runnable.cancel();
        active = null;
      }

      if (next != null) {
        next.runnable.cancel();
        next = null;
      }
    }
  }

  private class ExecutorRunnable implements Runnable {

    SearchHandler.SearchRunnable runnable;

    ExecutorRunnable(SearchHandler.SearchRunnable runnable) {
      this.runnable = runnable;
    }

    @Override public void run() {
      try {
        runnable.run();
      } finally {
        scheduleNext();
      }
    }
  }

  protected void scheduleNext() {
    synchronized (this) {
      active = next;
      next = null;
      if (active != null) {
        new Thread(active).start();
      }
    }
  }
}
