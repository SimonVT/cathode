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
import android.database.MergeCursor;
import android.support.v4.content.AsyncTaskLoader;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.util.DateUtils;

public class CollectLoader extends AsyncTaskLoader<Cursor> {

  private static final String TAG = "CollectLoader";

  final ForceLoadContentObserver observer;

  private long showId;

  Cursor cursor;

  public CollectLoader(Context context, long showId) {
    super(context);
    this.showId = showId;
    this.observer = new ForceLoadContentObserver();
  }

  @Override public Cursor loadInBackground() {
    Cursor toCollect = getContext().getContentResolver()
        .query(Episodes.fromShow(showId), null, EpisodeColumns.IN_COLLECTION
                + "=0 AND "
                + EpisodeColumns.FIRST_AIRED
                + ">"
                + DateUtils.YEAR_IN_SECONDS
                + " AND "
                + EpisodeColumns.SEASON
                + ">0", null,
            EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1"
        );
    toCollect.registerContentObserver(observer);
    if (toCollect.getCount() == 0) {
      return toCollect;
    }

    Cursor lastCollected = getContext().getContentResolver()
        .query(Episodes.fromShow(showId), null, EpisodeColumns.IN_COLLECTION + "=1", null,
            EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1"
        );
    lastCollected.registerContentObserver(observer);
    lastCollected.getCount();

    return new MergeCursor(new Cursor[] {
        toCollect, lastCollected,
    });
  }

  @Override public void deliverResult(Cursor cursor) {
    if (isReset()) {
      if (cursor != null) {
        cursor.close();
      }
      return;
    }
    Cursor oldCursor = this.cursor;
    this.cursor = cursor;

    if (isStarted()) {
      super.deliverResult(cursor);
    }

    if (oldCursor != null && oldCursor != cursor && !oldCursor.isClosed()) {
      oldCursor.close();
    }
  }

  @Override protected void onStartLoading() {
    if (cursor != null) {
      deliverResult(cursor);
    }
    if (takeContentChanged() || cursor == null) {
      forceLoad();
    }
  }

  @Override protected void onStopLoading() {
    // Attempt to cancel the current load task if possible.
    cancelLoad();
  }

  @Override public void onCanceled(Cursor cursor) {
    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
  }

  @Override protected void onReset() {
    super.onReset();

    // Ensure the loader is stopped
    onStopLoading();

    if (cursor != null && !cursor.isClosed()) {
      cursor.close();
    }
    cursor = null;
  }
}
