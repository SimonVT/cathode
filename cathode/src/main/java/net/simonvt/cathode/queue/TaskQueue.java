/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.queue;

import android.os.Looper;
import com.squareup.tape.ObjectQueue;

public class TaskQueue<T extends Task> implements ObjectQueue<T> {

  private final ObjectQueue<T> delegate;

  public TaskQueue(ObjectQueue<T> delegate) {
    this.delegate = delegate;
  }

  public void add(final T entry) {
    if (Looper.myLooper() == Looper.getMainLooper()) {
      new Thread(new Runnable() {
        @Override public void run() {
          addInternal(entry);
        }
      }).start();
    } else {
      addInternal(entry);
    }
  }

  protected void addInternal(T entry) {
    synchronized (this) {
      delegate.add(entry);
    }
  }

  public T peek() {
    synchronized (this) {
      return delegate.peek();
    }
  }

  @Override public int size() {
    synchronized (this) {
      return delegate.size();
    }
  }

  @Override public void remove() {
    synchronized (this) {
      delegate.remove();
    }
  }

  public void clear() {
    synchronized (this) {
      while (peek() != null) {
        remove();
      }
    }
  }

  @Override public void setListener(final Listener<T> listener) {
    if (listener != null) {
      // Intercept event delivery to pass the correct TaskQueue instance to listener.
      delegate.setListener(new Listener<T>() {
        @Override
        public void onAdd(ObjectQueue<T> queue, T entry) {
          listener.onAdd(TaskQueue.this, entry);
        }

        @Override
        public void onRemove(ObjectQueue<T> queue) {
          listener.onRemove(TaskQueue.this);
        }
      });
    } else {
      delegate.setListener(null);
    }
  }
}
