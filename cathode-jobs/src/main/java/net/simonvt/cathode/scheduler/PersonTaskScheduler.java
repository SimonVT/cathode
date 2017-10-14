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

package net.simonvt.cathode.scheduler;

import android.content.ContentValues;
import android.content.Context;
import javax.inject.Inject;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.ProviderSchematic.People;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.remote.sync.people.SyncPersonMovieCredits;
import net.simonvt.cathode.remote.sync.people.SyncPersonShowCredits;
import net.simonvt.cathode.tmdb.api.people.SyncPersonBackdrop;
import net.simonvt.cathode.tmdb.api.people.SyncPersonHeadshot;

public class PersonTaskScheduler extends BaseTaskScheduler {

  @Inject transient PersonDatabaseHelper personHelper;

  public PersonTaskScheduler(Context context) {
    super(context);
  }

  public void sync(final long personId) {
    sync(personId, null);
  }

  public void sync(final long personId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        ContentValues values = new ContentValues();
        values.put(PersonColumns.LAST_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(People.withId(personId), values, null, null);

        final long traktId = personHelper.getTraktId(personId);
        final int tmdbId = personHelper.getTmdbId(personId);

        queue(new SyncPersonHeadshot(tmdbId));
        queue(new SyncPersonBackdrop(tmdbId));
        queue(new SyncPersonShowCredits(traktId));
        Job job = new SyncPersonMovieCredits(traktId);
        if (onDoneListener != null) {
          job.registerOnDoneListener(onDoneListener);
        }
        queue(job);
      }
    });
  }
}
