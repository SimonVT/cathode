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

import android.content.Context;
import android.content.Intent;
import com.google.gson.Gson;
import com.squareup.tape.FileObjectQueue;
import com.squareup.tape.ObjectQueue;
import com.squareup.tape.TaskQueue;
import java.io.File;
import java.io.IOException;

public final class TraktTaskQueue extends TaskQueue<TraktTask> {

  private final Context context;

  private TraktTaskQueue(ObjectQueue<TraktTask> delegate, Context context) {
    super(delegate);
    this.context = context;

    if (size() > 0) {
      startService();
    }
  }

  private void startService() {
    context.startService(new Intent(context, TraktTaskService.class));
  }

  @Override public void add(TraktTask entry) {
    synchronized (this) {
      super.add(entry);
      startService();
    }
  }

  @Override public TraktTask peek() {
    synchronized (this) {
      return super.peek();
    }
  }

  @Override public int size() {
    synchronized (this) {
      return super.size();
    }
  }

  @Override public void remove() {
    synchronized (this) {
      super.remove();
    }
  }

  public void clear() {
    synchronized (this) {
      while (peek() != null) {
        remove();
      }
    }
  }

  public static TraktTaskQueue create(Context context, Gson gson, String tag) {
    FileObjectQueue.Converter<TraktTask> converter =
        new GsonConverter<TraktTask>(gson, TraktTask.class);
    File queueFile = new File(context.getFilesDir(), tag);
    FileObjectQueue<TraktTask> delegate;
    try {
      delegate = new FileObjectQueue<TraktTask>(queueFile, converter);
    } catch (IOException e) {
      throw new RuntimeException("Unable to create file queue.", e);
    }
    return new TraktTaskQueue(delegate, context);
  }
}
