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
package net.simonvt.cathode.ui.show;

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
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import dagger.android.support.AndroidSupportInjection;
import java.util.List;
import java.util.Locale;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.enumeration.ShowStatus;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.common.entity.CastMember;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Season;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.DateStringUtils;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.Intents;
import net.simonvt.cathode.common.util.Joiner;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.common.widget.CircleTransformation;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.PersonTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.widget.AdapterCountDataObserver;

public class ShowFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.ShowFragment";

  private static final String ARG_SHOWID = "net.simonvt.cathode.ui.show.ShowFragment.showId";
  private static final String ARG_TITLE = "net.simonvt.cathode.ui.show.ShowFragment.title";
  private static final String ARG_OVERVIEW = "net.simonvt.cathode.ui.show.ShowFragment.overview";
  private static final String ARG_TYPE = "net.simonvt.cathode.ui.show.ShowFragment.type";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.show.ShowFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.show.ShowFragment.listsAddDialog";

  private static final long SYNC_INTERVAL = 2 * DateUtils.DAY_IN_MILLIS;

  @Inject PersonTaskScheduler personScheduler;

  private NavigationListener navigationListener;

  private long showId;

  private ShowViewModel viewModel;

  private Show show;
  private List<String> genres;
  private List<CastMember> cast;
  private List<Comment> userComments;
  private List<Comment> comments;
  private List<Show> related;
  private Episode toWatch;
  private Episode lastWatched;
  private Episode toCollect;
  private Episode lastCollected;

  @BindView(R.id.seasonsDivider) View seasonsDivider;
  @BindView(R.id.seasonsTitle) View seasonsTitle;
  @BindView(R.id.seasons) RecyclerView seasonsView;
  private SeasonsAdapter seasonsAdapter;

  @BindView(R.id.rating) CircularProgressIndicator rating;
  @BindView(R.id.airtime) TextView airTime;
  @BindView(R.id.status) TextView status;
  @BindView(R.id.overview) TextView overview;

  @BindView(R.id.genresDivider) View genresDivider;
  @BindView(R.id.genresTitle) View genresTitle;
  @BindView(R.id.genres) TextView genresView;

  @BindView(R.id.checkmarks) View checkmarks;
  @BindView(R.id.isWatched) TextView watched;
  @BindView(R.id.inCollection) TextView collection;
  @BindView(R.id.inWatchlist) TextView watchlist;

  @BindView(R.id.castParent) View castParent;
  @BindView(R.id.castHeader) View castHeader;
  @BindView(R.id.castContainer) LinearLayout castContainer;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  @BindView(R.id.relatedParent) View relatedParent;
  @BindView(R.id.relatedHeader) View relatedHeader;
  @BindView(R.id.relatedContainer) LinearLayout relatedContainer;

  @BindView(R.id.trailer) TextView trailer;
  @BindView(R.id.website) TextView website;
  @BindView(R.id.viewOnTrakt) TextView viewOnTrakt;
  @BindView(R.id.viewOnImdb) TextView viewOnImdb;
  @BindView(R.id.viewOnTvdb) TextView viewOnTvdb;
  @BindView(R.id.viewOnTmdb) TextView viewOnTmdb;

  @BindView(R.id.episodes) LinearLayout episodes;

  @BindView(R.id.toWatch) View toWatchView;
  private EpisodeHolder toWatchHolder;
  private long toWatchId = -1;
  private String toWatchTitle;

  @BindView(R.id.lastWatched) @Nullable View lastWatchedView;
  private EpisodeHolder lastWatchedHolder;
  private long lastWatchedId = -1;

  @BindView(R.id.toCollect) View toCollectView;
  private EpisodeHolder toCollectHolder;
  private long toCollectId = -1;

  @BindView(R.id.lastCollected) @Nullable View lastCollectedView;
  private EpisodeHolder lastCollectedHolder;
  private long lastCollectedId = -1;

  static class EpisodeHolder {

    @BindView(R.id.episodeScreenshot) RemoteImageView episodeScreenshot;
    @BindView(R.id.episodeTitle) TextView episodeTitle;
    @BindView(R.id.episodeAirTime) TextView episodeAirTime;
    @BindView(R.id.episodeEpisode) TextView episodeEpisode;
    @BindView(R.id.episodeOverflow) OverflowView episodeOverflow;

    EpisodeHolder(View v) {
      ButterKnife.bind(this, v);
    }
  }

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  private String showTitle;

  private String showOverview;

  private boolean inWatchlist;

  private int currentRating;

  private boolean calendarHidden;

  private LibraryType type;

  public static String getTag(long showId) {
    return TAG + "/" + showId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long showId, String title, String overview, LibraryType type) {
    Preconditions.checkArgument(showId >= 0, "showId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_SHOWID, showId);
    args.putString(ARG_TITLE, title);
    args.putString(ARG_OVERVIEW, overview);
    args.putSerializable(ARG_TYPE, type);
    return args;
  }

  public long getShowId() {
    return showId;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    showId = args.getLong(ARG_SHOWID);
    showTitle = args.getString(ARG_TITLE);
    showOverview = args.getString(ARG_OVERVIEW);
    type = (LibraryType) args.getSerializable(ARG_TYPE);

    setTitle(showTitle);

    seasonsAdapter = new SeasonsAdapter(requireActivity(), new SeasonsAdapter.SeasonClickListener() {
      @Override
      public void onSeasonClick(long showId, long seasonId, String showTitle, int seasonNumber) {
        navigationListener.onDisplaySeason(showId, seasonId, showTitle, seasonNumber, type);
      }
    }, type);

    viewModel = ViewModelProviders.of(this).get(ShowViewModel.class);
    viewModel.setShowId(showId);
    viewModel.getShow().observe(this, new Observer<Show>() {
      @Override public void onChanged(Show show) {
        ShowFragment.this.show = show;
        updateShowView();
      }
    });
    viewModel.getGenres().observe(this, new Observer<List<String>>() {
      @Override public void onChanged(List<String> genres) {
        ShowFragment.this.genres = genres;
        updateGenreViews();
      }
    });
    viewModel.getSeasons().observe(this, new Observer<List<Season>>() {
      @Override public void onChanged(List<Season> seasons) {
        seasonsAdapter.setList(seasons);
      }
    });
    viewModel.getCast().observe(this, new Observer<List<CastMember>>() {
      @Override public void onChanged(List<CastMember> castMembers) {
        cast = castMembers;
        updateCastViews();
      }
    });
    viewModel.getUserComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> userComments) {
        ShowFragment.this.userComments = userComments;
        updateComments();
      }
    });
    viewModel.getComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> comments) {
        ShowFragment.this.comments = comments;
        updateComments();
      }
    });
    viewModel.getRelated().observe(this, new Observer<List<Show>>() {
      @Override public void onChanged(List<Show> relatedShows) {
        related = relatedShows;
        updateRelatedView();
      }
    });
    viewModel.getToWatch().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        toWatch = episode;
        updateToWatch();
      }
    });
    viewModel.getLastWatched().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        lastWatched = episode;
        updateLastWatched();
      }
    });
    viewModel.getToCollect().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        toCollect = episode;
        updateToCollect();
      }
    });
    viewModel.getLastCollected().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        lastCollected = episode;
        updateLastCollected();
      }
    });
  }

  @Override public View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_show, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    Drawable linkDrawable =
        VectorDrawableCompat.create(getResources(), R.drawable.ic_link_black_24dp, null);
    website.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnTrakt.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnImdb.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnTmdb.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);
    viewOnTvdb.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);

    Drawable playDrawable =
        VectorDrawableCompat.create(getResources(), R.drawable.ic_play_arrow_black_24dp, null);
    trailer.setCompoundDrawablesWithIntrinsicBounds(playDrawable, null, null, null);

    overview.setText(showOverview);

    DividerItemDecoration decoration =
        new DividerItemDecoration(requireContext(), LinearLayoutManager.HORIZONTAL);
    decoration.setDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.divider_4dp));
    seasonsView.addItemDecoration(decoration);
    seasonsView.setLayoutManager(
        new LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false));
    seasonsView.setAdapter(seasonsAdapter);
    ((DefaultItemAnimator) seasonsView.getItemAnimator()).setSupportsChangeAnimations(false);
    seasonsAdapter.registerAdapterDataObserver(new AdapterCountDataObserver(seasonsAdapter) {
      @Override public void onCountChanged(int itemCount) {
        if (seasonsTitle != null) {
          if (itemCount == 0) {
            seasonsDivider.setVisibility(View.GONE);
            seasonsTitle.setVisibility(View.GONE);
            seasonsView.setVisibility(View.GONE);
          } else {
            seasonsDivider.setVisibility(View.VISIBLE);
            seasonsTitle.setVisibility(View.VISIBLE);
            seasonsView.setVisibility(View.VISIBLE);
          }
        }
      }
    });
    if (seasonsAdapter.getItemCount() > 0) {
      seasonsDivider.setVisibility(View.VISIBLE);
      seasonsTitle.setVisibility(View.VISIBLE);
      seasonsView.setVisibility(View.VISIBLE);
    } else {
      seasonsDivider.setVisibility(View.GONE);
      seasonsTitle.setVisibility(View.GONE);
      seasonsView.setVisibility(View.GONE);
    }

    if (TraktLinkSettings.isLinked(requireContext())) {
      rating.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          RatingDialog.newInstance(RatingDialog.Type.SHOW, showId, currentRating)
              .show(getFragmentManager(), DIALOG_RATING);
        }
      });
    }

    castHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayCredits(ItemType.SHOW, showId, showTitle);
      }
    });

    commentsHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayComments(ItemType.SHOW, showId);
      }
    });

    relatedHeader.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        navigationListener.onDisplayRelatedShows(showId, showTitle);
      }
    });

    toWatchHolder = new EpisodeHolder(toWatchView);
    toWatchView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (toWatchId != -1) navigationListener.onDisplayEpisode(toWatchId, showTitle);
      }
    });

    toWatchHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_checkin:
            if (toWatchId != -1) {
              CheckInDialog.showDialogIfNecessary(requireActivity(), Type.SHOW, toWatchTitle,
                  toWatchId);
            }
            break;
          case R.id.action_checkin_cancel:
            if (toWatchId != -1) {
              episodeScheduler.cancelCheckin();
            }
            break;
          case R.id.action_history_add:
            if (toWatchId != -1) {
              AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, toWatchId,
                  toWatchTitle).show(getFragmentManager(), AddToHistoryDialog.TAG);
            }
            break;
        }
      }
    });

    if (lastWatchedView != null) {
      lastWatchedHolder = new EpisodeHolder(lastWatchedView);
      lastWatchedView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          if (lastWatchedId != -1) {
            navigationListener.onDisplayEpisode(lastWatchedId, showTitle);
          }
        }
      });
    }

    toCollectHolder = new EpisodeHolder(toCollectView);
    toCollectView.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        if (toCollectId != -1) navigationListener.onDisplayEpisode(toCollectId, showTitle);
      }
    });

    toCollectHolder.episodeOverflow.addItem(R.id.action_collection_add,
        R.string.action_collection_add);
    toCollectHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        switch (action) {
          case R.id.action_collection_add:
            if (toCollectId != -1) {
              episodeScheduler.setIsInCollection(toCollectId, true);
            }
            break;
        }
      }
    });

    if (lastCollectedView != null) {
      lastCollectedHolder = new EpisodeHolder(lastCollectedView);
      lastCollectedView.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          if (lastCollectedId != -1) {
            navigationListener.onDisplayEpisode(lastCollectedId, showTitle);
          }
        }
      });

      lastCollectedHolder.episodeOverflow.addItem(R.id.action_collection_remove,
          R.string.action_collection_remove);
      lastCollectedHolder.episodeOverflow.setListener(new OverflowView.OverflowActionListener() {
        @Override public void onPopupShown() {
        }

        @Override public void onPopupDismissed() {
        }

        @Override public void onActionSelected(int action) {
          switch (action) {
            case R.id.action_collection_add:
              if (lastCollectedId != -1) {
                episodeScheduler.setIsInCollection(lastCollectedId, true);
              }
              break;
          }
        }
      });
    }

    updateShowView();
    updateGenreViews();
    updateCastViews();
    updateRelatedView();
    updateToWatch();
    updateLastWatched();
    updateToCollect();
    updateLastCollected();
    updateComments();
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    showScheduler.sync(showId, onDoneListener);
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    Menu menu = toolbar.getMenu();

    if (inWatchlist) {
      menu.add(0, R.id.action_watchlist_remove, 1, R.string.action_watchlist_remove);
    } else {
      menu.add(0, R.id.action_watchlist_add, 1, R.string.action_watchlist_add);
    }

    if (calendarHidden) {
      menu.add(0, R.id.action_calendar_unhide, 2, R.string.action_calendar_unhide);
    } else {
      menu.add(0, R.id.action_calendar_hide, 2, R.string.action_calendar_hide);
    }

    menu.add(0, R.id.action_list_add, 3, R.string.action_list_add);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_watchlist_remove:
        showScheduler.setIsInWatchlist(showId, false);
        return true;

      case R.id.action_watchlist_add:
        showScheduler.setIsInWatchlist(showId, true);
        return true;

      case R.id.action_list_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.SHOW, showId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;

      case R.id.action_calendar_hide:
        showScheduler.hideFromCalendar(showId, true);
        return true;

      case R.id.action_calendar_unhide:
        showScheduler.hideFromCalendar(showId, false);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  private void updateShowView() {
    if (getView() == null || show == null) {
      return;
    }

    showTitle = show.getTitle();
    setTitle(show.getTitle());
    final String backdropUri = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.BACKDROP, showId);
    setBackdrop(backdropUri, true);
    showOverview = show.getOverview();

    currentRating = show.getUserRating();
    rating.setValue(show.getRating());

    calendarHidden = show.getHiddenCalendar();

    final boolean isWatched = show.getWatchedCount() > 0;
    final boolean isCollected = show.getInCollectionCount() > 0;
    inWatchlist = show.getInWatchlist();
    final boolean hasCheckmark = isWatched || isCollected || inWatchlist;
    checkmarks.setVisibility(hasCheckmark ? View.VISIBLE : View.GONE);
    watched.setVisibility(isWatched ? View.VISIBLE : View.GONE);
    collection.setVisibility(isCollected ? View.VISIBLE : View.GONE);
    watchlist.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    String airDay = show.getAirDay();
    String airTime = show.getAirTime();
    String network = show.getNetwork();
    String certification = show.getCertification();

    String airTimeString = null;
    if (airDay != null && airTime != null) {
      airTimeString = airDay + " " + airTime;
    }
    if (network != null) {
      if (airTimeString != null) {
        airTimeString += ", " + network;
      } else {
        airTimeString = network;
      }
    }
    if (certification != null) {
      if (airTimeString != null) {
        airTimeString += ", " + certification;
      } else {
        airTimeString = certification;
      }
    }

    this.airTime.setText(airTimeString);

    ShowStatus status = show.getStatus();
    String statusString = null;
    if (status != null) {
      switch (status) {
        case ENDED:
          statusString = getString(R.string.show_status_ended);
          break;

        case RETURNING:
          statusString = getString(R.string.show_status_returning);
          break;

        case CANCELED:
          statusString = getString(R.string.show_status_canceled);
          break;

        case IN_PRODUCTION:
          statusString = getString(R.string.show_status_in_production);
          break;

        case PLANNED:
          statusString = getString(R.string.show_status_planned);
          break;
      }
    }

    this.status.setText(statusString);

    this.overview.setText(showOverview);

    final String trailer = show.getTrailer();
    if (!TextUtils.isEmpty(trailer)) {
      this.trailer.setVisibility(View.VISIBLE);
      this.trailer.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(requireContext(), trailer);
        }
      });
    } else {
      this.trailer.setVisibility(View.GONE);
    }

    final long lastSync = show.getLastSync();
    if (show.getNeedsSync() || System.currentTimeMillis() > lastSync + SYNC_INTERVAL) {
      showScheduler.sync(showId);
    }

    if (TraktTimestamps.shouldSyncComments(show.getLastCommentSync())) {
      showScheduler.syncComments(showId);
    }

    if (lastSync > show.getLastCreditsSync()) {
      showScheduler.syncCredits(showId, null);
    }

    if (lastSync > show.getLastRelatedSync()) {
      showScheduler.syncRelated(showId, null);
    }

    String website = show.getHomepage();
    if (!TextUtils.isEmpty(website)) {
      this.website.setVisibility(View.VISIBLE);
      this.website.setText(website);
      this.website.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(requireContext(), website);
        }
      });
    } else {
      this.website.setVisibility(View.GONE);
    }

    Long traktId = show.getTraktId();
    String imdbId = show.getImdbId();
    Integer tvdbId = show.getTvdbId();
    Integer tmdbId = show.getTmdbId();

    viewOnTrakt.setVisibility(View.VISIBLE);
    viewOnTrakt.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intents.openUrl(requireContext(), TraktUtils.getTraktShowUrl(traktId));
      }
    });

    final boolean hasImdbId = !TextUtils.isEmpty(imdbId);
    if (hasImdbId) {
      viewOnImdb.setVisibility(View.VISIBLE);
      viewOnImdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(requireContext(), TraktUtils.getImdbUrl(imdbId));
        }
      });
    } else {
      viewOnImdb.setVisibility(View.GONE);
    }

    if (tvdbId > 0) {
      viewOnTvdb.setVisibility(View.VISIBLE);
      viewOnTvdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(requireContext(), TraktUtils.getTvdbUrl(tvdbId));
        }
      });
    } else {
      viewOnTvdb.setVisibility(View.GONE);
    }

    if (tmdbId > 0) {
      viewOnTmdb.setVisibility(View.VISIBLE);
      viewOnTmdb.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          Intents.openUrl(requireContext(), TraktUtils.getTmdbTvUrl(tmdbId));
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
      genresDivider.setVisibility(View.GONE);
      genresTitle.setVisibility(View.GONE);
      genresView.setVisibility(View.GONE);
    } else {
      genresDivider.setVisibility(View.VISIBLE);
      genresTitle.setVisibility(View.VISIBLE);
      genresView.setVisibility(View.VISIBLE);
      genresView.setText(Joiner.on(", ").join(genres));
    }
  }

  private void updateCastViews() {
    if (getView() == null) {
      return;
    }

    castContainer.removeAllViews();
    if (cast == null || cast.size() == 0) {
      castParent.setVisibility(View.GONE);
    } else {
      castParent.setVisibility(View.VISIBLE);

      for (CastMember castMember : cast) {
        View v =
            LayoutInflater.from(requireContext()).inflate(R.layout.section_people_item, castContainer, false);

        final long personId = castMember.getPerson().getId();
        final String headshotUrl = ImageUri.create(ImageUri.ITEM_PERSON, ImageType.PROFILE, personId);

        RemoteImageView headshot = v.findViewById(R.id.headshot);
        headshot.addTransformation(new CircleTransformation());
        headshot.setImage(headshotUrl);

        TextView name = v.findViewById(R.id.person_name);
        name.setText(castMember.getPerson().getName());

        TextView character = v.findViewById(R.id.person_job);
        character.setText(castMember.getCharacter());

        v.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            navigationListener.onDisplayPerson(personId);
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
    if (related == null) {
      relatedParent.setVisibility(View.GONE);
    } else {
      relatedParent.setVisibility(View.VISIBLE);

      for (Show show : related) {
        View v = LayoutInflater.from(requireContext())
            .inflate(R.layout.section_related_item, this.relatedContainer, false);

        final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());

        RemoteImageView posterView = v.findViewById(R.id.related_poster);
        posterView.addTransformation(new CircleTransformation());
        posterView.setImage(poster);

        TextView titleView = v.findViewById(R.id.related_title);
        titleView.setText(show.getTitle());

        final String formattedRating = String.format(Locale.getDefault(), "%.1f", show.getRating());

        String ratingText;
        if (show.getVotes() >= 1000) {
          final float convertedVotes = show.getVotes() / 1000.0f;
          final String formattedVotes = String.format(Locale.getDefault(), "%.1f", convertedVotes);
          ratingText = getString(R.string.related_rating_thousands, formattedRating, formattedVotes);
        } else {
          ratingText = getString(R.string.related_rating, formattedRating, show.getVotes());
        }

        TextView ratingView = v.findViewById(R.id.related_rating);
        ratingView.setText(ratingText);

        v.setOnClickListener(new View.OnClickListener() {
          @Override public void onClick(View v) {
            navigationListener.onDisplayShow(show.getId(), show.getTitle(), show.getOverview(),
                LibraryType.WATCHED);
          }
        });
        relatedContainer.addView(v);
      }
    }
  }

  private void updateEpisodesContainer() {
    if (getView() == null) {
      return;
    }

    if (toWatchId == -1 && lastWatchedId == -1 && toCollectId == -1 && lastCollectedId == -1) {
      episodes.setVisibility(View.GONE);
    } else {
      episodes.setVisibility(View.VISIBLE);
    }
  }

  private void updateToWatch() {
    if (getView() == null) {
      return;
    }

    if (toWatch == null) {
      toWatchView.setVisibility(View.GONE);
      toWatchId = -1;
    } else {
      toWatchView.setVisibility(View.VISIBLE);
      toWatchId = toWatch.getId();

      toWatchTitle =
          DataHelper.getEpisodeTitle(requireContext(), toWatch.getTitle(), toWatch.getSeason(),
              toWatch.getEpisode(), toWatch.getWatched());
      final String toWatchEpisodeText =
          getString(R.string.season_x_episode_y, toWatch.getSeason(), toWatch.getEpisode());

      toWatchHolder.episodeTitle.setText(toWatchTitle);
      toWatchHolder.episodeEpisode.setText(toWatchEpisodeText);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toWatchId);
      toWatchHolder.episodeScreenshot.setImage(screenshotUri);

      String firstAiredString =
          DateStringUtils.getAirdateInterval(requireContext(), toWatch.getFirstAired(), false);

      toWatchHolder.episodeOverflow.removeItems();
      if (toWatch.getCheckedIn()) {
        toWatchHolder.episodeOverflow.addItem(R.id.action_checkin_cancel,
            R.string.action_checkin_cancel);
        firstAiredString = getResources().getString(R.string.show_watching);
      } else if (!toWatch.getWatching()) {
        toWatchHolder.episodeOverflow.addItem(R.id.action_checkin, R.string.action_checkin);
        toWatchHolder.episodeOverflow.addItem(R.id.action_history_add, R.string.action_history_add);
      }

      toWatchHolder.episodeAirTime.setText(firstAiredString);
    }

    updateEpisodesContainer();
  }

  private void updateLastWatched() {
    if (lastWatchedView == null) {
      return;
    }

    if (lastWatched != null) {
      lastWatchedView.setVisibility(View.VISIBLE);
      lastWatchedId = lastWatched.getId();

      final String title =
          DataHelper.getEpisodeTitle(requireContext(), lastWatched.getTitle(), lastWatched.getSeason(),
              lastWatched.getEpisode(), lastWatched.getWatched());
      lastWatchedHolder.episodeTitle.setText(title);

      final String firstAiredString =
          DateStringUtils.getAirdateInterval(requireContext(), lastWatched.getFirstAired(), false);
      lastWatchedHolder.episodeAirTime.setText(firstAiredString);

      final String lastWatchedEpisodeText =
          getString(R.string.season_x_episode_y, lastWatched.getSeason(), lastWatched.getEpisode());
      lastWatchedHolder.episodeEpisode.setText(lastWatchedEpisodeText);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastWatchedId);
      lastWatchedHolder.episodeScreenshot.setImage(screenshotUri);
    } else {
      lastWatchedView.setVisibility(toWatchId == -1 ? View.GONE : View.INVISIBLE);
      lastWatchedId = -1;
    }

    updateEpisodesContainer();
  }

  private void updateToCollect() {
    if (getView() == null) {
      return;
    }

    if (toCollect == null) {
      toCollectView.setVisibility(View.GONE);
      toCollectId = -1;
    } else {
      toCollectView.setVisibility(View.VISIBLE);
      toCollectId = toCollect.getId();

      final String title =
          DataHelper.getEpisodeTitle(requireContext(), toCollect.getTitle(), toCollect.getSeason(),
              toCollect.getEpisode(), toCollect.getWatched());
      toCollectHolder.episodeTitle.setText(title);

      final String firstAiredString =
          DateStringUtils.getAirdateInterval(requireContext(), toCollect.getFirstAired(), false);
      toCollectHolder.episodeAirTime.setText(firstAiredString);

      final String toCollectEpisodeText =
          getString(R.string.season_x_episode_y, toCollect.getSeason(), toCollect.getEpisode());
      toCollectHolder.episodeEpisode.setText(toCollectEpisodeText);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, toCollectId);
      toCollectHolder.episodeScreenshot.setImage(screenshotUri);
    }

    updateEpisodesContainer();
  }

  private void updateLastCollected() {
    if (lastCollectedView == null) {
      return;
    }

    if (lastCollected == null) {
      lastCollectedId = -1;
      lastCollectedView.setVisibility(View.INVISIBLE);
    } else {
      lastCollectedView.setVisibility(View.VISIBLE);
      lastCollectedId = lastCollected.getId();

      final String title = DataHelper.getEpisodeTitle(requireContext(), lastCollected.getTitle(),
          lastCollected.getSeason(), lastCollected.getEpisode(), lastCollected.getWatched());
      lastCollectedHolder.episodeTitle.setText(title);

      final String firstAiredString =
          DateStringUtils.getAirdateInterval(requireContext(), lastCollected.getFirstAired(), false);
      lastCollectedHolder.episodeAirTime.setText(firstAiredString);

      final String lastCollectedEpisodeText =
          getString(R.string.season_x_episode_y, lastCollected.getSeason(),
              lastCollected.getEpisode());
      lastCollectedHolder.episodeEpisode.setText(lastCollectedEpisodeText);

      final String screenshotUri =
          ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, lastCollectedId);
      lastCollectedHolder.episodeScreenshot.setImage(screenshotUri);
    }

    updateEpisodesContainer();
  }

  private void updateComments() {
    if (getView() == null) {
      return;
    }

    LinearCommentsAdapter.updateComments(requireContext(), commentsContainer, userComments,
        comments);
    commentsParent.setVisibility(View.VISIBLE);
  }
}
