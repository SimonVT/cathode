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
package net.simonvt.cathode.remote.sync.shows;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.service.RecommendationsService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;

public class SyncShowRecommendations extends CallJob<List<Show>> {

  private static final int LIMIT = 50;

  @Inject transient RecommendationsService recommendationsService;

  @Inject transient ShowDatabaseHelper showHelper;

  public SyncShowRecommendations() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncShowRecommendations";
  }

  @Override public int getPriority() {
    return JobPriority.SUGGESTIONS;
  }

  @Override public Call<List<Show>> getCall() {
    return recommendationsService.shows(LIMIT, Extended.FULL);
  }

  @Override public boolean handleResponse(List<Show> shows) {
    ContentResolver resolver = getContentResolver();

    List<Long> showIds = new ArrayList<>();
    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    Cursor c = resolver.query(Shows.SHOWS_RECOMMENDED, null, null, null, null);
    while (c.moveToNext()) {
      showIds.add(Cursors.getLong(c, ShowColumns.ID));
    }
    c.close();

    for (int index = 0, count = shows.size(); index < count; index++) {
      Show show = shows.get(index);
      final long showId = showHelper.partialUpdate(show);

      showIds.remove(showId);

      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
          .withValue(ShowColumns.RECOMMENDATION_INDEX, index)
          .build();
      ops.add(op);
    }

    for (Long id : showIds) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(id))
          .withValue(ShowColumns.RECOMMENDATION_INDEX, -1)
          .build();
      ops.add(op);
    }

    return applyBatch(ops);
  }
}
