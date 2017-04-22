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
package net.simonvt.cathode.provider;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.database.BaseAsyncLoader;
import net.simonvt.cathode.database.DatabaseUtils;
import net.simonvt.cathode.database.SimpleMergeCursor;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;

public class CollectLoader extends BaseAsyncLoader<SimpleMergeCursor> {

  private long showId;

  private String[] projection;

  public CollectLoader(Context context, long showId, String[] projection) {
    super(context);
    this.showId = showId;
    this.projection = projection;
  }

  @Override public SimpleMergeCursor loadInBackground() {
    clearNotificationUris();

    Cursor toCollect = getContext().getContentResolver()
        .query(Episodes.fromShow(showId), projection, EpisodeColumns.IN_COLLECTION
                + "=0 AND "
                + EpisodeColumns.FIRST_AIRED
                + " IS NOT NULL"
                + " AND "
                + EpisodeColumns.SEASON
                + ">0", null,
            EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1");

    if (toCollect.getCount() == 0) {
      SimpleMergeCursor cursor = new SimpleMergeCursor(toCollect);
      toCollect.close();
      return cursor;
    }

    Cursor lastCollected = getContext().getContentResolver()
        .query(Episodes.fromShow(showId), projection, EpisodeColumns.IN_COLLECTION + "=1", null,
            EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1");

    lastCollected.getCount();

    Uri notiUri = DatabaseUtils.getNotificationUri(toCollect);
    addNotificationUri(notiUri);
    notiUri = DatabaseUtils.getNotificationUri(lastCollected);
    addNotificationUri(notiUri);

    return new SimpleMergeCursor(toCollect, lastCollected);
  }
}
