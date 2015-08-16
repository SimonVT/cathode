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

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.Bind;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.ui.Loaders;
import net.simonvt.cathode.ui.MoviesNavigationListener;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.widget.CircleTransformation;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.RemoteImageView;

public class MovieFragment extends BaseFragment
    implements LoaderManager.LoaderCallbacks<SimpleCursor> {

  private static final String ARG_ID = "net.simonvt.cathode.ui.fragment.MovieFragment.id";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.fragment.MovieFragment.title";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.fragment.MovieFragment.ratingDialog";

  @Inject MovieTaskScheduler movieScheduler;
  @Inject Bus bus;

  @Bind(R.id.year) TextView year;
  @Bind(R.id.certification) TextView certification;
  @Bind(R.id.backdrop) RemoteImageView backdrop;
  //@Bind(R.id.poster) RemoteImageView poster;
  @Bind(R.id.overview) TextView overview;
  @Bind(R.id.isWatched) TextView isWatched;
  @Bind(R.id.inCollection) TextView collection;
  @Bind(R.id.inWatchlist) TextView watchlist;
  @Bind(R.id.rating) CircularProgressIndicator rating;

  @Bind(R.id.actorsParent) View actorsParent;
  @Bind(R.id.actorsHeader) View actorsHeader;
  @Bind(R.id.actors) LinearLayout actors;
  @Bind(R.id.peopleContainer) LinearLayout peopleContainer;

  private long movieId;

  private String movieTitle;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private boolean watching;

  private boolean checkedIn;

  private MoviesNavigationListener navigationListener;

  public static Bundle getArgs(long movieId, String movieTitle) {
    Bundle args = new Bundle();
    args.putLong(ARG_ID, movieId);
    args.putString(ARG_TITLE, movieTitle);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    try {
      navigationListener = (MoviesNavigationListener) activity;
    } catch (ClassCastException e) {
      throw new ClassCastException(
          activity.toString() + " must implement MoviesNavigationListener");
    }
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    movieId = args.getLong(ARG_ID);
    movieTitle = args.getString(ARG_TITLE);

    setTitle(movieTitle);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_movie, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    rating.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.MOVIE, movieId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    actorsHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayMovieActors(movieId, movieTitle);
      }
    });

    getLoaderManager().initLoader(Loaders.MOVIE, null, this);
    getLoaderManager().initLoader(Loaders.MOVIE_ACTORS, null, actorsLoader);
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    Menu menu = toolbar.getMenu();

    if (loaded) {
      if (checkedIn) {
        menu.add(0, R.id.action_checkin_cancel, 1, R.string.action_checkin_cancel)
            .setIcon(R.drawable.ic_action_cancel)
            .setShowAsActionFlags(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
      } else if (!watching) {
        menu.add(0, R.id.action_checkin, 2, R.string.action_checkin)
            .setIcon(R.drawable.ic_action_checkin)
            .setShowAsActionFlags(
                MenuItem.SHOW_AS_ACTION_ALWAYS | MenuItem.SHOW_AS_ACTION_WITH_TEXT);
      }

      if (watched) {
        menu.add(0, R.id.action_unwatched, 3, R.string.action_unwatched);
      } else {
        menu.add(0, R.id.action_watched, 4, R.string.action_watched);
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 7, R.string.action_watchlist_remove);
        } else {
          menu.add(0, R.id.action_watchlist_add, 8, R.string.action_watchlist_add);
        }
      }

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 5, R.string.action_collection_remove);
      } else {
        menu.add(0, R.id.action_collection_add, 6, R.string.action_collection_add);
      }
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_watched:
        movieScheduler.setWatched(movieId, true);
        return true;

      case R.id.action_unwatched:
        movieScheduler.setWatched(movieId, false);
        return true;

      case R.id.action_checkin:
        CheckInDialog.showDialogIfNecessary(getActivity(), Type.MOVIE, movieTitle, movieId);
        return true;

      case R.id.action_checkin_cancel:
        movieScheduler.cancelCheckin();
        return true;

      case R.id.action_watchlist_add:
        movieScheduler.setIsInWatchlist(movieId, true);
        return true;

      case R.id.action_watchlist_remove:
        movieScheduler.setIsInWatchlist(movieId, false);
        return true;

      case R.id.action_collection_add:
        movieScheduler.setIsInCollection(movieId, true);
        return true;

      case R.id.action_collection_remove:
        movieScheduler.setIsInCollection(movieId, false);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  private void updateView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;
    loaded = true;

    final String title = cursor.getString(cursor.getColumnIndex(MovieColumns.TITLE));
    if (!title.equals(movieTitle)) {
      movieTitle = title;
      setTitle(movieTitle);
    }
    final int year = cursor.getInt(cursor.getColumnIndex(MovieColumns.YEAR));
    final String certification =
        cursor.getString(cursor.getColumnIndex(MovieColumns.CERTIFICATION));

    final String fanartUrl = cursor.getString(cursor.getColumnIndex(MovieColumns.FANART));
    backdrop.setImage(fanartUrl, true);
    final String posterUrl = cursor.getString(cursor.getColumnIndex(MovieColumns.POSTER));
    //poster.setImage(posterUrl);

    currentRating = cursor.getInt(cursor.getColumnIndex(MovieColumns.USER_RATING));
    final float ratingAll = cursor.getFloat(cursor.getColumnIndex(MovieColumns.RATING));
    rating.setValue(ratingAll);

    final String overview = cursor.getString(cursor.getColumnIndex(MovieColumns.OVERVIEW));
    watched = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHED)) == 1;
    collected = cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_COLLECTION)) == 1;
    inWatchlist = cursor.getInt(cursor.getColumnIndex(MovieColumns.IN_WATCHLIST)) == 1;
    watching = cursor.getInt(cursor.getColumnIndex(MovieColumns.WATCHING)) == 1;
    checkedIn = cursor.getInt(cursor.getColumnIndex(MovieColumns.CHECKED_IN)) == 1;

    isWatched.setVisibility(watched ? View.VISIBLE : View.GONE);
    collection.setVisibility(collected ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    this.year.setText(String.valueOf(year));
    this.certification.setText(certification);
    this.overview.setText(overview);

    invalidateMenu();
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    SimpleCursorLoader loader =
        new SimpleCursorLoader(getActivity(), Movies.withId(movieId), null, null, null, null);
    loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
    return loader;
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor cursor) {
    updateView(cursor);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
  }

  private void updateActors(Cursor c) {
    peopleContainer.removeAllViews();

    final int count = c.getCount();
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    actorsParent.setVisibility(visibility);

    int index = 0;

    c.moveToPosition(-1);
    while (c.moveToNext() && index < 3) {
      View v = LayoutInflater.from(getActivity()).inflate(R.layout.item_person, actors, false);

      RemoteImageView headshot = (RemoteImageView) v.findViewById(R.id.headshot);
      headshot.addTransformation(new CircleTransformation());
      headshot.setImage(c.getString(c.getColumnIndex(PersonColumns.HEADSHOT)));

      TextView name = (TextView) v.findViewById(R.id.person_name);
      name.setText(c.getString(c.getColumnIndex(PersonColumns.NAME)));

      TextView character = (TextView) v.findViewById(R.id.person_job);
      character.setText(c.getString(c.getColumnIndex(MovieCastColumns.CHARACTER)));

      peopleContainer.addView(v);

      index++;
    }
  }

  private static final String[] CAST_PROJECTION = new String[] {
      Tables.MOVIE_CAST + "." + MovieCastColumns.ID,
      Tables.MOVIE_CAST + "." + MovieCastColumns.PERSON_ID,
      Tables.MOVIE_CAST + "." + MovieCastColumns.CHARACTER,
      Tables.PEOPLE + "." + PersonColumns.NAME, Tables.PEOPLE + "." + PersonColumns.HEADSHOT,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> actorsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
          SimpleCursorLoader loader =
              new SimpleCursorLoader(getActivity(), ProviderSchematic.MovieCast.fromMovie(movieId),
                  CAST_PROJECTION, Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null,
                  null);
          loader.setUpdateThrottle(2 * DateUtils.SECOND_IN_MILLIS);
          return loader;
        }

        @Override
        public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor cursor) {
          updateActors(cursor);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };
}
