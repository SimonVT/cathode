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
package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.format.DateUtils;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.ui.Loaders;

public class MovieWatchlistFragment extends MoviesFragment {

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    setEmptyText(R.string.empty_movie_watchlist);
    setTitle(R.string.title_movies_watchlist);
  }

  @Override protected int getLoaderId() {
    return Loaders.LOADER_MOVIES_WATCHLIST;
  }

  @Override public Loader<Cursor> onCreateLoader(int i, Bundle bundle) {
    CursorLoader loader = new CursorLoader(getActivity(), Movies.MOVIES, null,
        MovieColumns.IN_WATCHLIST + "=1 AND " + MovieColumns.NEEDS_SYNC + "=0", null,
        Movies.DEFAULT_SORT);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }
}
