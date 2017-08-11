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
package net.simonvt.cathode.ui.movie;

import android.app.Activity;
import android.database.Cursor;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.OnClick;
import java.util.Locale;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.Intents;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieGenreColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedMoviesColumns;
import net.simonvt.cathode.provider.DatabaseContract.UserColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.MovieCast;
import net.simonvt.cathode.provider.ProviderSchematic.MovieGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedMovies;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.cathode.widget.CheckInDrawable;
import net.simonvt.cathode.widget.CircleTransformation;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class MovieFragment extends RefreshableAppBarFragment
    implements LoaderManager.LoaderCallbacks<SimpleCursor> {

  private static final String TAG = "net.simonvt.cathode.ui.movie.MovieFragment";

  private static final int LOADER_MOVIE = 1;
  private static final int LOADER_MOVIE_GENRES = 2;
  private static final int LOADER_MOVIE_CAST = 3;
  private static final int LOADER_MOVIE_USER_COMMENTS = 4;
  private static final int LOADER_MOVIE_COMMENTS = 5;
  private static final int LOADER_RELATED = 6;

  private static final String ARG_ID = "net.simonvt.cathode.ui.movie.MovieFragment.id";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.movie.MovieFragment.title";
  private static final String ARG_OVERVIEW = "net.simonvt.cathode.ui.movie.MovieFragment.overview";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.movie.MovieFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.movie.MovieFragment.listsAddDialog";

  @Inject MovieTaskScheduler movieScheduler;
  @Inject PersonTaskScheduler personScheduler;

  @BindView(R.id.year) TextView year;
  @BindView(R.id.certification) TextView certification;
  //@BindView(R.id.poster) RemoteImageView poster;
  @BindView(R.id.overview) TextView overview;

  @BindView(R.id.genresTitle) View genresTitle;
  @BindView(R.id.genres) TextView genres;

  @BindView(R.id.checkmarks) View checkmarks;
  @BindView(R.id.isWatched) TextView isWatched;
  @BindView(R.id.inCollection) TextView collection;
  @BindView(R.id.inWatchlist) TextView watchlist;
  @BindView(R.id.rating) CircularProgressIndicator rating;

  @BindView(R.id.castParent) View castParent;
  @BindView(R.id.castHeader) View castHeader;
  @BindView(R.id.cast) LinearLayout cast;
  @BindView(R.id.castContainer) LinearLayout castContainer;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  @BindView(R.id.relatedParent) View relatedParent;
  @BindView(R.id.relatedHeader) View relatedHeader;
  @BindView(R.id.related) LinearLayout related;
  @BindView(R.id.relatedContainer) LinearLayout relatedContainer;

  @BindView(R.id.trailer) TextView trailer;
  @BindView(R.id.website) TextView website;
  @BindView(R.id.viewOnTrakt) TextView viewOnTrakt;
  @BindView(R.id.viewOnImdb) TextView viewOnImdb;
  @BindView(R.id.viewOnTmdb) TextView viewOnTmdb;

  private Cursor userComments;
  private Cursor comments;

  private long movieId;

  private String movieTitle;

  private String movieOverview;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private boolean watching;

  private boolean checkedIn;

  private NavigationListener navigationListener;

  private CheckInDrawable checkInDrawable;

  public static String getTag(long movieId) {
    return TAG + "/" + movieId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long movieId, String movieTitle, String overview) {
    Preconditions.checkArgument(movieId >= 0, "movieId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_ID, movieId);
    args.putString(ARG_TITLE, movieTitle);
    args.putString(ARG_OVERVIEW, overview);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);

    Bundle args = getArguments();
    movieId = args.getLong(ARG_ID);
    movieTitle = args.getString(ARG_TITLE);
    movieOverview = args.getString(ARG_OVERVIEW);

    setTitle(movieTitle);
  }

  public long getMovieId() {
    return movieId;
  }

  @Override public View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_movie, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    Drawable linkDrawable =
        VectorDrawableCompat.create(getResources(), R.drawable.ic_link_black_24dp, null);
    website.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnTrakt.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnImdb.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnTmdb.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);

    Drawable playDrawable =
        VectorDrawableCompat.create(getResources(), R.drawable.ic_play_arrow_black_24dp, null);
    trailer.setCompoundDrawablesWithIntrinsicBounds(playDrawable, null, null, null);

    overview.setText(movieOverview);

    getLoaderManager().initLoader(LOADER_MOVIE, null, this);
    getLoaderManager().initLoader(LOADER_MOVIE_GENRES, null, genresLoader);
    getLoaderManager().initLoader(LOADER_MOVIE_CAST, null, castLoader);
    getLoaderManager().initLoader(LOADER_MOVIE_USER_COMMENTS, null, userCommentsLoader);
    getLoaderManager().initLoader(LOADER_MOVIE_COMMENTS, null, commentsLoader);
    getLoaderManager().initLoader(LOADER_RELATED, null, relatedLoader);
  }

  @OnClick(R.id.rating) void onRatingClick() {
    RatingDialog.newInstance(RatingDialog.Type.MOVIE, movieId, currentRating)
        .show(getFragmentManager(), DIALOG_RATING);
  }

  @OnClick(R.id.castHeader) void onDisplayCast() {
    navigationListener.onDisplayCredits(ItemType.MOVIE, movieId, movieTitle);
  }

  @OnClick(R.id.commentsHeader) void onShowComments() {
    navigationListener.onDisplayComments(ItemType.MOVIE, movieId);
  }

  @OnClick(R.id.relatedHeader) void onShowRelated() {
    navigationListener.onDisplayRelatedMovies(movieId, movieTitle);
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    movieScheduler.sync(movieId, onDoneListener);
  }

  @Override public void createMenu(Toolbar toolbar) {
    if (loaded) {
      Menu menu = toolbar.getMenu();

      if (checkInDrawable == null) {
        checkInDrawable = new CheckInDrawable(toolbar.getContext());
        checkInDrawable.setWatching(watching || checkedIn);
        checkInDrawable.setId(movieId);
      }

      MenuItem checkInItem;

      if (watching || checkedIn) {
        checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin_cancel);
      } else {
        checkInItem = menu.add(0, R.id.action_checkin, 1, R.string.action_checkin);
      }

      checkInItem.setIcon(checkInDrawable).setShowAsAction(MenuItem.SHOW_AS_ACTION_IF_ROOM);

      if (watching) {
        checkInItem.setEnabled(false);
      } else {
        checkInItem.setEnabled(true);
      }

      menu.add(0, R.id.action_history_add, 3, R.string.action_history_add);

      if (watched) {
        menu.add(0, R.id.action_history_remove, 4, R.string.action_history_remove);
      } else {
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

      menu.add(0, R.id.action_list_add, 9, R.string.action_list_add);
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_history_add:
        AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.MOVIE, movieId, movieTitle)
            .show(getFragmentManager(), AddToHistoryDialog.TAG);
        return true;

      case R.id.action_history_remove:
        RemoveFromHistoryDialog.newInstance(RemoveFromHistoryDialog.Type.MOVIE, movieId, movieTitle)
            .show(getFragmentManager(), RemoveFromHistoryDialog.TAG);
        return true;

      case R.id.action_checkin:
        if (!watching) {
          if (checkedIn) {
            movieScheduler.cancelCheckin();
            if (checkInDrawable != null) {
              checkInDrawable.setWatching(false);
            }
          } else {
            if (!CheckInDialog.showDialogIfNecessary(getActivity(), Type.MOVIE, movieTitle,
                movieId)) {
              checkInDrawable.setWatching(true);
            }
          }
        }
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

      case R.id.action_list_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.MOVIE, movieId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  private void updateView(final Cursor cursor) {
    if (cursor == null || !cursor.moveToFirst()) return;
    loaded = true;

    final long traktId = Cursors.getLong(cursor, MovieColumns.TRAKT_ID);
    final String title = Cursors.getString(cursor, MovieColumns.TITLE);
    if (title != null && !title.equals(movieTitle)) {
      movieTitle = title;
      setTitle(movieTitle);
    }
    final int year = Cursors.getInt(cursor, MovieColumns.YEAR);
    final String certification = Cursors.getString(cursor, MovieColumns.CERTIFICATION);

    final String backdropUri = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.BACKDROP, movieId);
    setBackdrop(backdropUri, true);

    currentRating = Cursors.getInt(cursor, MovieColumns.USER_RATING);
    final float ratingAll = Cursors.getFloat(cursor, MovieColumns.RATING);
    rating.setValue(ratingAll);

    movieOverview = Cursors.getString(cursor, MovieColumns.OVERVIEW);
    watched = Cursors.getBoolean(cursor, MovieColumns.WATCHED);
    collected = Cursors.getBoolean(cursor, MovieColumns.IN_COLLECTION);
    inWatchlist = Cursors.getBoolean(cursor, MovieColumns.IN_WATCHLIST);
    watching = Cursors.getBoolean(cursor, MovieColumns.WATCHING);
    checkedIn = Cursors.getBoolean(cursor, MovieColumns.CHECKED_IN);

    if (checkInDrawable != null) {
      checkInDrawable.setWatching(watching || checkedIn);
    }

    final boolean hasCheckmark = watched || collected || inWatchlist;
    checkmarks.setVisibility(hasCheckmark ? View.VISIBLE : View.GONE);
    isWatched.setVisibility(watched ? View.VISIBLE : View.GONE);
    collection.setVisibility(collected ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    this.year.setText(String.valueOf(year));
    this.certification.setText(certification);
    this.overview.setText(movieOverview);

    final String trailer = Cursors.getString(cursor, MovieColumns.TRAILER);
    if (!TextUtils.isEmpty(trailer)) {
      this.trailer.setVisibility(View.VISIBLE);
      this.trailer.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getActivity(), trailer);
        }
      });
    } else {
      this.trailer.setVisibility(View.GONE);
    }

    final boolean needsSync = Cursors.getBoolean(cursor, MovieColumns.NEEDS_SYNC);
    if (needsSync) {
      movieScheduler.sync(movieId);
    }

    final long lastSync = Cursors.getLong(cursor, MovieColumns.LAST_SYNC);
    final long lastCommentSync = Cursors.getLong(cursor, MovieColumns.LAST_COMMENT_SYNC);
    if (TraktTimestamps.shouldSyncComments(lastCommentSync)) {
      movieScheduler.syncComments(movieId);
    }

    final long lastCreditsSync = Cursors.getLong(cursor, MovieColumns.LAST_CREDITS_SYNC);
    if (lastSync > lastCreditsSync) {
      movieScheduler.syncCredits(movieId, null);
    }

    final long lastRelatedSync = Cursors.getLong(cursor, MovieColumns.LAST_RELATED_SYNC);
    if (lastSync > lastRelatedSync) {
      movieScheduler.syncRelated(movieId, null);
    }

    final String website = Cursors.getString(cursor, MovieColumns.HOMEPAGE);
    if (!TextUtils.isEmpty(website)) {
      this.website.setVisibility(View.VISIBLE);
      this.website.setText(website);
      this.website.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), website);
        }
      });
    } else {
      this.website.setVisibility(View.GONE);
    }

    final String imdbId = Cursors.getString(cursor, MovieColumns.IMDB_ID);
    final int tmdbId = Cursors.getInt(cursor, MovieColumns.TMDB_ID);

    viewOnTrakt.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intents.openUrl(getContext(), TraktUtils.getTraktMovieUrl(traktId));
      }
    });

    final boolean hasImdbId = !TextUtils.isEmpty(imdbId);
    if (hasImdbId) {
      viewOnImdb.setVisibility(View.VISIBLE);
      viewOnImdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), TraktUtils.getImdbUrl(imdbId));
        }
      });
    } else {
      viewOnImdb.setVisibility(View.GONE);
    }

    if (tmdbId > 0) {
      viewOnTmdb.setVisibility(View.VISIBLE);
      viewOnTmdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(getContext(), TraktUtils.getTmdbMovieUrl(tmdbId));
        }
      });
    } else {
      viewOnTmdb.setVisibility(View.GONE);
    }

    invalidateMenu();
  }

  @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
    return new SimpleCursorLoader(getActivity(), Movies.withId(movieId), null, null, null, null);
  }

  @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor cursor) {
    updateView(cursor);
  }

  @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
  }

  private void updateGenreViews(final Cursor cursor) {
    if (cursor.getCount() > 0) {
      StringBuilder sb = new StringBuilder();
      final int genreColumnIndex = cursor.getColumnIndex(MovieGenreColumns.GENRE);

      cursor.moveToPosition(-1);

      while (cursor.moveToNext()) {
        sb.append(cursor.getString(genreColumnIndex));
        if (!cursor.isLast()) sb.append(", ");
      }

      genresTitle.setVisibility(View.VISIBLE);
      genres.setVisibility(View.VISIBLE);
      genres.setText(sb.toString());
    } else {
      genresTitle.setVisibility(View.GONE);
      genres.setVisibility(View.GONE);
    }
  }

  private void updateCast(Cursor c) {
    castContainer.removeAllViews();

    final int count = c.getCount();
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    castParent.setVisibility(visibility);

    int index = 0;

    c.moveToPosition(-1);
    while (c.moveToNext() && index < 3) {
      View v =
          LayoutInflater.from(getActivity()).inflate(R.layout.item_person, castContainer, false);

      final long personId = Cursors.getLong(c, MovieCastColumns.PERSON_ID);

      final String headshotUri = ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, personId);

      RemoteImageView headshot = v.findViewById(R.id.headshot);
      headshot.addTransformation(new CircleTransformation());
      headshot.setImage(headshotUri);

      TextView name = v.findViewById(R.id.person_name);
      name.setText(Cursors.getString(c, PersonColumns.NAME));

      TextView character = v.findViewById(R.id.person_job);
      character.setText(Cursors.getString(c, MovieCastColumns.CHARACTER));

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          navigationListener.onDisplayPerson(personId);
        }
      });

      castContainer.addView(v);

      index++;
    }
  }

  private void updateRelatedView(Cursor related) {
    relatedContainer.removeAllViews();

    final int count = related.getCount();
    final int visibility = count > 0 ? View.VISIBLE : View.GONE;
    relatedParent.setVisibility(visibility);

    int index = 0;

    related.moveToPosition(-1);
    while (related.moveToNext() && index < 3) {
      View v = LayoutInflater.from(getActivity())
          .inflate(R.layout.item_related, relatedContainer, false);

      final long relatedMovieId = Cursors.getLong(related, RelatedMoviesColumns.RELATED_MOVIE_ID);
      final String title = Cursors.getString(related, MovieColumns.TITLE);
      final String overview = Cursors.getString(related, MovieColumns.OVERVIEW);
      final float rating = Cursors.getFloat(related, MovieColumns.RATING);
      final int votes = Cursors.getInt(related, MovieColumns.VOTES);

      final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, relatedMovieId);

      RemoteImageView posterView = v.findViewById(R.id.related_poster);
      posterView.addTransformation(new CircleTransformation());
      posterView.setImage(poster);

      TextView titleView = v.findViewById(R.id.related_title);
      titleView.setText(title);

      final String formattedRating = String.format(Locale.getDefault(), "%.1f", rating);

      String ratingText;
      if (votes >= 1000) {
        final float convertedVotes = votes / 1000.0f;
        final String formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes);
        ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes);
      } else {
        ratingText = getString(R.string.related_rating, formattedRating, votes);
      }

      TextView ratingView = v.findViewById(R.id.related_rating);
      ratingView.setText(ratingText);

      v.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          navigationListener.onDisplayMovie(relatedMovieId, title, overview);
        }
      });
      relatedContainer.addView(v);

      index++;
    }
  }

  private void updateComments() {
    LinearCommentsAdapter.updateComments(getContext(), commentsContainer, userComments, comments);
    commentsParent.setVisibility(View.VISIBLE);
  }

  private static final String[] GENRES_PROJECTION = new String[] {
      MovieGenreColumns.GENRE,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> genresLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), MovieGenres.fromMovie(movieId),
              GENRES_PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateGenreViews(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private static final String[] CAST_PROJECTION = new String[] {
      Tables.MOVIE_CAST + "." + MovieCastColumns.ID,
      Tables.MOVIE_CAST + "." + MovieCastColumns.PERSON_ID,
      Tables.MOVIE_CAST + "." + MovieCastColumns.CHARACTER,
      Tables.PEOPLE + "." + PersonColumns.NAME,
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> castLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int i, Bundle bundle) {
          return new SimpleCursorLoader(getActivity(),
              MovieCast.fromMovie(movieId), CAST_PROJECTION,
              Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null, null);
        }

        @Override
        public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor cursor) {
          updateCast(cursor);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };

  private static final String[] COMMENTS_PROJECTION = new String[] {
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.ID),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.COMMENT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.SPOILER),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.REVIEW),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.CREATED_AT),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.LIKES),
      SqlColumn.table(Tables.COMMENTS).column(CommentColumns.USER_RATING),
      SqlColumn.table(Tables.USERS).column(UserColumns.USERNAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.NAME),
      SqlColumn.table(Tables.USERS).column(UserColumns.AVATAR),
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> userCommentsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), Comments.fromMovie(movieId),
              COMMENTS_PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          userComments = data;
          updateComments();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private LoaderManager.LoaderCallbacks<SimpleCursor> commentsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), Comments.fromMovie(movieId),
              COMMENTS_PROJECTION,
              CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
              CommentColumns.LIKES + " DESC LIMIT 3");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          comments = data;
          updateComments();
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private static final String[] RELATED_PROJECTION = new String[] {
      SqlColumn.table(Tables.MOVIE_RELATED).column(RelatedMoviesColumns.RELATED_MOVIE_ID),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.RATING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.VOTES),
  };

  private LoaderManager.LoaderCallbacks<SimpleCursor> relatedLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), RelatedMovies.fromMovie(movieId),
              RELATED_PROJECTION, null, null, RelatedMoviesColumns.RELATED_INDEX + " ASC LIMIT 3");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          updateRelatedView(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };
}
