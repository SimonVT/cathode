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

import android.content.ContentResolver;
import android.content.Context;
import javax.inject.Inject;

public abstract class Job {

  public static final int PRIORITY_5 = 5;

  public static final int PRIORITY_4 = 4;

  public static final int PRIORITY_3 = 3;

  public static final int PRIORITY_2 = 2;

  public static final int PRIORITY_1 = 1;

  public static final int PRIORITY_0 = 1;

  @Inject transient JobManager jobManager;

  @Inject transient Context context;

  private int flags;

  protected Job() {
    this(0);
  }

  protected Job(int flags) {
    this.flags = flags;
  }

  public abstract String key();

  public int getPriority() {
    return PRIORITY_3;
  }

  public abstract void perform();

  public boolean requiresWakelock() {
    return false;
  }

  public int getFlags() {
    return flags;
  }

  protected final void queue(Job job) {
    jobManager.addJob(job);
  }

  protected final Context getContext() {
    return context;
  }

  protected final ContentResolver getContentResolver() {
    return context.getContentResolver();
  }

  public void setFlags(int flags) {
    this.flags = flags;
  }

  public void addFlag(int flag) {
    flags |= flag;
  }

  public void removeFlag(int flag) {
    flags &= ~flag;
  }

  public boolean hasFlag(int flag) {
    return (flags & flag) == flag;
  }
}
