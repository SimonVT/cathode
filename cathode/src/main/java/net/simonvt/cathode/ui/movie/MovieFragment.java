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
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.TextUtils;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.OnClick;
import dagger.android.support.AndroidSupportInjection;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.common.entity.CastMember;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.Movie;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.Intents;
import net.simonvt.cathode.common.util.Joiner;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.common.widget.CircleTransformation;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.sync.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.widget.CheckInDrawable;

public class MovieFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.movie.MovieFragment";

  private static final String ARG_ID = "net.simonvt.cathode.ui.movie.MovieFragment.id";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.movie.MovieFragment.title";
  private static final String ARG_OVERVIEW = "net.simonvt.cathode.ui.movie.MovieFragment.overview";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.movie.MovieFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.movie.MovieFragment.listsAddDialog";

  private static final long SYNC_INTERVAL = 2 * DateUtils.DAY_IN_MILLIS;

  @Inject MovieTaskScheduler movieScheduler;
  @Inject PersonTaskScheduler personScheduler;

  private MovieViewModel movieViewModel;

  private Movie movie;
  private List<String> genres;
  private List<CastMember> cast;
  private List<Comment> userComments;
  private List<Comment> comments;
  private List<Movie> related;

  @BindView(R.id.year) TextView year;
  @BindView(R.id.certification) TextView certification;
  //@BindView(R.id.poster) RemoteImageView poster;
  @BindView(R.id.overview) TextView overview;

  @BindView(R.id.genresTitle) View genresTitle;
  @BindView(R.id.genres) TextView genresView;

  @BindView(R.id.checkmarks) View checkmarks;
  @BindView(R.id.isWatched) TextView isWatched;
  @BindView(R.id.inCollection) TextView collection;
  @BindView(R.id.inWatchlist) TextView watchlist;
  @BindView(R.id.rating) CircularProgressIndicator rating;

  @BindView(R.id.castParent) View castParent;
  @BindView(R.id.castHeader) View castHeader;
  @BindView(R.id.cast) LinearLayout castView;
  @BindView(R.id.castContainer) LinearLayout castContainer;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  @BindView(R.id.relatedParent) View relatedParent;
  @BindView(R.id.relatedHeader) View relatedHeader;
  @BindView(R.id.related) LinearLayout relatedView;
  @BindView(R.id.relatedContainer) LinearLayout relatedContainer;

  @BindView(R.id.trailer) TextView trailer;
  @BindView(R.id.website) TextView website;
  @BindView(R.id.viewOnTrakt) TextView viewOnTrakt;
  @BindView(R.id.viewOnImdb) TextView viewOnImdb;
  @BindView(R.id.viewOnTmdb) TextView viewOnTmdb;

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
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    movieId = args.getLong(ARG_ID);
    movieTitle = args.getString(ARG_TITLE);
    movieOverview = args.getString(ARG_OVERVIEW);

    setTitle(movieTitle);

    movieViewModel = ViewModelProviders.of(this).get(MovieViewModel.class);
    movieViewModel.setMovieId(movieId);
    movieViewModel.getMovie().observe(this, new Observer<Movie>() {
      @Override public void onChanged(Movie movie) {
        MovieFragment.this.movie = movie;
        updateView();
      }
    });
    movieViewModel.getGenres().observe(this, new Observer<List<String>>() {
      @Override public void onChanged(List<String> genres) {
        MovieFragment.this.genres = genres;
        updateGenreViews();
      }
    });
    movieViewModel.getCast().observe(this, new Observer<List<CastMember>>() {
      @Override public void onChanged(List<CastMember> castMembers) {
        cast = castMembers;
        updateCast();
      }
    });
    movieViewModel.getUserComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> userComments) {
        MovieFragment.this.userComments = userComments;
        updateComments();
      }
    });
    movieViewModel.getComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> comments) {
        MovieFragment.this.comments = comments;
        updateComments();
      }
    });
    movieViewModel.getRelatedMovies().observe(this, new Observer<List<Movie>>() {
      @Override public void onChanged(List<Movie> movies) {
        related = movies;
        updateRelatedView();
      }
    });
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

    updateView();
    updateGenreViews();
    updateCast();
    updateRelatedView();
    updateComments();
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
              movieScheduler.checkin(movieId, null, false, false, false);
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

  private void updateView() {
    if (getView() == null || movie == null) {
      return;
    }

    loaded = true;

    final long traktId = movie.getTraktId();
    final String title = movie.getTitle();
    if (title != null && !title.equals(movieTitle)) {
      movieTitle = title;
      setTitle(movieTitle);
    }

    final String backdropUri = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.BACKDROP, movieId);
    setBackdrop(backdropUri, true);

    currentRating = movie.getUserRating();
    rating.setValue(movie.getRating());

    movieOverview = movie.getOverview();
    watched = movie.getWatched();
    collected = movie.getInCollection();
    inWatchlist = movie.getInWatchlist();
    watching = movie.getWatching();
    checkedIn = movie.getCheckedIn();

    if (checkInDrawable != null) {
      checkInDrawable.setWatching(watching || checkedIn);
    }

    final boolean hasCheckmark = watched || collected || inWatchlist;
    checkmarks.setVisibility(hasCheckmark ? View.VISIBLE : View.GONE);
    isWatched.setVisibility(watched ? View.VISIBLE : View.GONE);
    collection.setVisibility(collected ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    this.year.setText(String.valueOf(movie.getYear()));
    this.certification.setText(movie.getCertification());
    this.overview.setText(movieOverview);

    final String trailer = movie.getTrailer();
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

    final boolean needsSync = movie.getNeedsSync();
    final long lastSync = movie.getLastSync();
    if (needsSync || System.currentTimeMillis() > lastSync + SYNC_INTERVAL) {
      movieScheduler.sync(movieId);
    }

    final long lastCommentSync = movie.getLastCommentSync();
    if (TraktTimestamps.shouldSyncComments(lastCommentSync)) {
      movieScheduler.syncComments(movieId);
    }

    final long lastCreditsSync = movie.getLastCreditsSync();
    if (lastSync > lastCreditsSync) {
      movieScheduler.syncCredits(movieId, null);
    }

    final long lastRelatedSync = movie.getLastRelatedSync();
    if (lastSync > lastRelatedSync) {
      movieScheduler.syncRelated(movieId, null);
    }

    final String website = movie.getHomepage();
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

    final String imdbId = movie.getImdbId();
    final int tmdbId = movie.getTmdbId();

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

  private void updateGenreViews() {
    if (getView() == null) {
      return;
    }

    if (genres == null || genres.size() == 0) {
      genresTitle.setVisibility(View.GONE);
      genresView.setVisibility(View.GONE);
    } else {
      final String joinedGenres = Joiner.on(", ").join(genres);

      genresTitle.setVisibility(View.VISIBLE);
      genresView.setVisibility(View.VISIBLE);
      genresView.setText(joinedGenres);
    }
  }

  private void updateCast() {
    if (getView() == null) {
      return;
    }

    castContainer.removeAllViews();
    if (cast == null) {
      castParent.setVisibility(View.GONE);
    } else {
      castParent.setVisibility(View.VISIBLE);

      for (CastMember castMember : cast) {
        View v =
            LayoutInflater.from(getActivity()).inflate(R.layout.item_person, castContainer, false);

        final String headshotUri =
            ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, castMember.getPerson().getId());

        RemoteImageView headshot = v.findViewById(R.id.headshot);
        headshot.addTransformation(new CircleTransformation());
        headshot.setImage(headshotUri);

        TextView name = v.findViewById(R.id.person_name);
        name.setText(castMember.getPerson().getName());

        TextView character = v.findViewById(R.id.person_job);
        character.setText(castMember.getCharacter());

        v.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            navigationListener.onDisplayPerson(castMember.getPerson().getId());
          }
        });

        castContainer.addView(v);
      }
    }
  }

  private void updateRelatedView() {
    if (getView() == null) {
      return;
    }

    relatedContainer.removeAllViews();
    if (related == null || related.size() == 0) {
      relatedParent.setVisibility(View.GONE);
    } else {
      relatedParent.setVisibility(View.VISIBLE);

      for (Movie movie : related) {
        View v = LayoutInflater.from(getActivity())
            .inflate(R.layout.item_related, relatedContainer, false);

        final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.getId());

        RemoteImageView posterView = v.findViewById(R.id.related_poster);
        posterView.addTransformation(new CircleTransformation());
        posterView.setImage(poster);

        TextView titleView = v.findViewById(R.id.related_title);
        titleView.setText(movie.getTitle());

        final String formattedRating = String.format(Locale.getDefault(), "%.1f", movie.getRating());

        String ratingText;
        if (movie.getVotes() >= 1000) {
          final float convertedVotes = movie.getVotes() / 1000.0f;
          final String formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes);
          ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes);
        } else {
          ratingText = getString(R.string.related_rating, formattedRating, movie.getVotes());
        }

        TextView ratingView = v.findViewById(R.id.related_rating);
        ratingView.setText(ratingText);

        v.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            navigationListener.onDisplayMovie(movie.getId(), movie.getTitle(), movie.getOverview());
          }
        });
        relatedContainer.addView(v);
      }
    }
  }

  private void updateComments() {
    if (getView() == null) {
      return;
    }

    LinearCommentsAdapter.updateComments(getContext(), commentsContainer, userComments, comments);
    commentsParent.setVisibility(View.VISIBLE);
  }
}
