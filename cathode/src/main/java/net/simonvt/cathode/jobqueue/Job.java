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

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.Context;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.util.MainHandler;
import timber.log.Timber;

public abstract class Job {

  public interface OnDoneListener {

    void onDone(Job job);
  }

  public static final int PRIORITY_ACTIONS = 10;

  public static final int PRIORITY_USER_DATA = 9;

  public static final int PRIORITY_SEASONS = 6;

  public static final int PRIORITY_SHOWS = 5;

  public static final int PRIORITY_MOVIES = 4;

  public static final int PRIORITY_SUGGESTIONS = 3;

  public static final int PRIORITY_EXTRAS = 2;

  public static final int PRIORITY_UPDATED = 1;

  public static final int PRIORITY_PURGE = 0;

  @Inject transient JobManager jobManager;

  @Inject transient Context context;

  private int flags;

  private transient boolean checkedOut;

  private transient List<WeakReference<OnDoneListener>> onDoneRefs;

  protected Job() {
    this(0);
  }

  protected Job(int flags) {
    this.flags = flags;
  }

  public abstract String key();

  public abstract int getPriority();

  public abstract void perform();

  public final void done() {
    if (onDoneRefs != null) {
      MainHandler.post(new Runnable() {
        @Override public void run() {
          for (WeakReference<OnDoneListener> ref : onDoneRefs) {
            OnDoneListener listener = ref.get();
            if (listener != null) {
              listener.onDone(Job.this);
              ref.clear();
            }
          }
        }
      });
    }
  }

  public boolean allowDuplicates() {
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

  protected final void applyBatch(ArrayList<ContentProviderOperation> ops) {
    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e);
    } catch (OperationApplicationException e) {
      Timber.e(e);
    }
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

  public boolean hasFlags(int flags) {
    return (this.flags & flags) == flags;
  }

  public void registerOnDoneListener(OnDoneListener listener) {
    if (onDoneRefs == null) {
      onDoneRefs = new ArrayList<>();
    }

    onDoneRefs.add(new WeakReference<>(listener));
  }

  public List<WeakReference<OnDoneListener>> getOnDoneRefs() {
    return onDoneRefs;
  }

  final void setCheckedOut(boolean checkedOut) {
    this.checkedOut = checkedOut;
  }

  final boolean isCheckedOut() {
    return checkedOut;
  }
}
