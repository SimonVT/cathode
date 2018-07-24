/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.ui.stats;

import android.content.Context;
import android.database.Cursor;
import androidx.annotation.Nullable;
import net.simonvt.cathode.common.data.AsyncLiveData;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.schematic.Cursors;

public class StatsLiveData extends AsyncLiveData<Stats> {

  private Context context;

  public StatsLiveData(Context context) {
    this.context = context;
  }

  @Nullable @Override protected Stats loadInBackground() {
    Cursor shows = context.getContentResolver().query(Shows.SHOWS_WATCHED, new String[] {
        ShowColumns.WATCHED_COUNT, ShowColumns.RUNTIME,
    }, null, null, null);

    final int showCount = shows.getCount();
    int episodeCount = 0;
    long episodeTime = 0L;
    while (shows.moveToNext()) {
      final int watchedCount = Cursors.getInt(shows, ShowColumns.WATCHED_COUNT);
      final int runtime = Cursors.getInt(shows, ShowColumns.RUNTIME);

      episodeCount += watchedCount;
      episodeTime += (watchedCount * runtime);
    }

    shows.close();

    Cursor movies = context.getContentResolver().query(Movies.MOVIES_WATCHED, new String[] {
        MovieColumns.RUNTIME,
    }, null, null, null);

    final int movieCount = movies.getCount();
    long moviesTime = 0L;

    while (movies.moveToNext()) {
      final int runtime = Cursors.getInt(movies, MovieColumns.RUNTIME);
      moviesTime += runtime;
    }

    movies.close();

    return new Stats(episodeTime, episodeCount, showCount, moviesTime, movieCount);
  }
}
