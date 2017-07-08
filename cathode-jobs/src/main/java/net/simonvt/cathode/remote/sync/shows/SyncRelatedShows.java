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
package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentProviderOperation;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncRelatedShows extends CallJob<List<Show>> {

  private static final int RELATED_COUNT = 50;

  @Inject transient ShowsService showsService;

  @Inject transient ShowDatabaseHelper showHelper;

  private long traktId;

  public SyncRelatedShows(long traktId) {
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncRelatedShows" + "&traktId=" + traktId;
  }

  @Override public int getPriority() {
    return JobPriority.EXTRAS;
  }

  @Override public Call<List<Show>> getCall() {
    return showsService.getRelated(traktId, RELATED_COUNT, Extended.FULL);
  }

  @Override public boolean handleResponse(List<Show> shows) {
    final long showId = showHelper.getId(traktId);

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    List<Long> relatedIds = new ArrayList<>();

    Cursor related = getContentResolver().query(RelatedShows.fromShow(showId), new String[] {
        Tables.SHOW_RELATED + "." + RelatedShowsColumns.ID,
    }, null, null, null);
    while (related.moveToNext()) {
      final long relatedShowId = Cursors.getLong(related, RelatedShowsColumns.ID);
      relatedIds.add(relatedShowId);
    }
    related.close();

    int relatedIndex = 0;
    for (Show show : shows) {
      final long traktId = show.getIds().getTrakt();
      final int tmdbId = show.getIds().getTmdb();
      final long relatedShowId = showHelper.partialUpdate(show);

      ContentProviderOperation op = ContentProviderOperation.newInsert(RelatedShows.RELATED)
          .withValue(RelatedShowsColumns.SHOW_ID, showId)
          .withValue(RelatedShowsColumns.RELATED_SHOW_ID, relatedShowId)
          .withValue(RelatedShowsColumns.RELATED_INDEX, relatedIndex)
          .build();
      ops.add(op);

      relatedIndex++;
    }

    for (Long id : relatedIds) {
      ops.add(ContentProviderOperation.newDelete(RelatedShows.withId(id)).build());
    }

    return applyBatch(ops);
  }
}
