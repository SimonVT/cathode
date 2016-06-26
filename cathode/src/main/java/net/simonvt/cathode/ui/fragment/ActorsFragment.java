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

package net.simonvt.cathode.ui.fragment;

import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.RecyclerView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.ui.adapter.MovieActorsAdapter;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.ui.adapter.ShowActorsAdapter;

public class ActorsFragment extends ToolbarGridFragment<RecyclerView.ViewHolder>
    implements LoaderManager.LoaderCallbacks<SimpleCursor> {

  public static final String TAG = "net.simonvt.cathode.ui.fragment.ActorsFragment";

  private enum Type {
    SHOW,
    MOVIE
  }

  private static final String ARG_TYPE = "net.simonvt.cathode.ui.fragment.ActorsFragment.type";
  private static final String ARG_ID = "net.simonvt.cathode.ui.fragment.ActorsFragment.id";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.fragment.ActorsFragment.title";

  private static final int LOADER_ACTORS = 1;

  private Type type;

  private long id;

  private String title;

  private RecyclerCursorAdapter adapter;

  private Cursor cursor;

  public static Bundle forShow(long showId, String title) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, Type.SHOW);
    args.putLong(ARG_ID, showId);
    args.putString(ARG_TITLE, title);
    return args;
  }

  public static Bundle forMovie(long movieId, String title) {
    Bundle args = new Bundle();
    args.putSerializable(ARG_TYPE, Type.MOVIE);
    args.putLong(ARG_ID, movieId);
    args.putString(ARG_TITLE, title);
    return args;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Bundle args = getArguments();
    type = (Type) args.getSerializable(ARG_TYPE);
    id = args.getLong(ARG_ID);
    title = args.getString(ARG_TITLE);

    setTitle(title);

    getLoaderManager().initLoader(LOADER_ACTORS, null, this);
  }

  @Override protected int getColumnCount() {
    return getResources().getInteger(R.integer.actorColumns);
  }

  private void setCursor(Cursor cursor) {
    this.cursor = cursor;

    if (adapter == null) {
      if (type == Type.MOVIE) {
        adapter = new MovieActorsAdapter(getActivity(), cursor);
      } else {
        adapter = new ShowActorsAdapter(getActivity(), cursor);
      }

      setAdapter(adapter);
    } else {
      adapter.changeCursor(cursor);
    }
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int loaderId, Bundle args) {
    SimpleCursorLoader loader;
    if (type == Type.MOVIE) {
      loader = new SimpleCursorLoader(getActivity(), ProviderSchematic.MovieCast.fromMovie(id),
          MovieActorsAdapter.PROJECTION,
          DatabaseSchematic.Tables.PEOPLE + "." + DatabaseContract.PersonColumns.NEEDS_SYNC + "=0",
          null, null);
    } else {
      loader = new SimpleCursorLoader(getActivity(), ProviderSchematic.ShowCharacters.fromShow(id),
          ShowActorsAdapter.PROJECTION,
          DatabaseSchematic.Tables.PEOPLE + "." + DatabaseContract.PersonColumns.NEEDS_SYNC + "=0",
          null, null);
    }

    return loader;
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
    setCursor(data);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> loader) {

  }
}
