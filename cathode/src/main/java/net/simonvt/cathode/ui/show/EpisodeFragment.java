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
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.util.TraktUtils;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment;
import net.simonvt.cathode.common.util.DateStringUtils;
import net.simonvt.cathode.common.util.Ids;
import net.simonvt.cathode.common.util.Intents;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.common.widget.CircularProgressIndicator;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.settings.TraktLinkSettings;
import net.simonvt.cathode.settings.TraktTimestamps;
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.comments.LinearCommentsAdapter;
import net.simonvt.cathode.ui.dialog.CheckInDialog;
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.cathode.widget.CheckInDrawable;

public class EpisodeFragment extends RefreshableAppBarFragment {

  private static final String TAG = "net.simonvt.cathode.ui.show.EpisodeFragment";

  private static final String ARG_EPISODEID =
      "net.simonvt.cathode.ui.show.EpisodeFragment.episodeId";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.cathode.ui.show.EpisodeFragment.showTitle";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.show.EpisodeFragment.ratingDialog";
  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.show.EpisodeFragment.listsAddDialog";

  @Inject ShowTaskScheduler showScheduler;
  @Inject EpisodeTaskScheduler episodeScheduler;

  @BindView(R.id.title) TextView title;
  @BindView(R.id.overview) TextView overview;
  @BindView(R.id.firstAired) TextView firstAired;

  @BindView(R.id.rating) CircularProgressIndicator rating;

  @BindView(R.id.checkmarks) View checkmarks;
  @BindView(R.id.isWatched) View watchedView;
  @BindView(R.id.inCollection) View inCollectionView;
  @BindView(R.id.inWatchlist) View inWatchlistView;

  @BindView(R.id.commentsParent) View commentsParent;
  @BindView(R.id.commentsHeader) View commentsHeader;
  @BindView(R.id.commentsContainer) LinearLayout commentsContainer;

  @BindView(R.id.viewOnTrakt) TextView viewOnTrakt;

  private EpisodeViewModel viewModel;

  private List<Comment> userComments;
  private List<Comment> comments;

  private long episodeId;
  private Episode episode;

  private String episodeTitle;

  private String showTitle;
  private long showId = -1L;
  private int season = -1;
  private long seasonId = -1L;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private boolean watching;

  private boolean checkedIn;

  private NavigationListener navigationListener;

  private CheckInDrawable checkInDrawable;

  public static String getTag(long episodeId) {
    return TAG + "/" + episodeId + "/" + Ids.newId();
  }

  public static Bundle getArgs(long episodeId, String showTitle) {
    Preconditions.checkArgument(episodeId >= 0, "episodeId must be >= 0, was " + episodeId);

    Bundle args = new Bundle();
    args.putLong(ARG_EPISODEID, episodeId);
    args.putString(ARG_SHOW_TITLE, showTitle);
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
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    setTitle(showTitle);

    viewModel = ViewModelProviders.of(this).get(EpisodeViewModel.class);
    viewModel.setEpisodeId(episodeId);
    viewModel.getEpisode().observe(this, new Observer<Episode>() {
      @Override public void onChanged(Episode episode) {
        updateEpisodeViews(episode);
      }
    });
    viewModel.getUserComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> userComments) {
        EpisodeFragment.this.userComments = userComments;
        updateComments();
      }
    });
    viewModel.getComments().observe(this, new Observer<List<Comment>>() {
      @Override public void onChanged(List<Comment> comments) {
        EpisodeFragment.this.comments = comments;
        updateComments();
      }
    });
  }

  public long getEpisodeId() {
    return episodeId;
  }

  @Override public boolean onBackPressed() {
    return false;
  }

  @Override protected void onHomeClicked() {
    if (showId >= 0L && seasonId >= 0L) {
      navigationListener.upFromEpisode(showId, showTitle, seasonId);
    } else {
      navigationListener.onHomeClicked();
    }
  }

  @Override public View createView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_episode, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    Drawable linkDrawable =
        VectorDrawableCompat.create(getResources(), R.drawable.ic_link_black_24dp, null);
    viewOnTrakt.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null);

    if (TraktLinkSettings.isLinked(requireContext())) {
      rating.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          RatingDialog.newInstance(RatingDialog.Type.EPISODE, episodeId, currentRating)
              .show(getFragmentManager(), DIALOG_RATING);
        }
      });
    }
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    episodeScheduler.sync(episodeId, onDoneListener);
  }

  @OnClick(R.id.commentsHeader) void onShowComments() {
    navigationListener.onDisplayComments(ItemType.EPISODE, episodeId);
  }

  @Override public void createMenu(Toolbar toolbar) {
    if (loaded) {
      Menu menu = toolbar.getMenu();
      if (TraktLinkSettings.isLinked(requireContext())) {
        if (checkInDrawable == null) {
          checkInDrawable = new CheckInDrawable(toolbar.getContext());
          checkInDrawable.setWatching(watching || checkedIn);
          checkInDrawable.setId(episodeId);
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
      }

      menu.add(0, R.id.action_history_add, 3, R.string.action_history_add);

      if (watched) {
        menu.add(0, R.id.action_history_remove, 4, R.string.action_history_remove);
      } else {
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 5, R.string.action_watchlist_remove);
        } else {
          menu.add(0, R.id.action_watchlist_add, 6, R.string.action_watchlist_add);
        }
      }

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 7, R.string.action_collection_remove);
      } else {
        menu.add(0, R.id.action_collection_add, 8, R.string.action_collection_add);
      }

      menu.add(0, R.id.action_list_add, 9, R.string.action_list_add);
    }
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_history_add:
        AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.EPISODE, episodeId, episodeTitle)
            .show(getFragmentManager(), AddToHistoryDialog.TAG);
        return true;

      case R.id.action_history_remove:
        if (TraktLinkSettings.isLinked(requireContext())) {
          RemoveFromHistoryDialog.newInstance(RemoveFromHistoryDialog.Type.EPISODE, episodeId,
              episodeTitle, showTitle).show(getFragmentManager(), RemoveFromHistoryDialog.TAG);
        } else {
          episodeScheduler.removeFromHistory(episodeId);
        }
        return true;

      case R.id.action_checkin:
        if (!watching) {
          if (checkedIn) {
            episodeScheduler.cancelCheckin();
            if (checkInDrawable != null) {
              checkInDrawable.setWatching(false);
            }
          } else {
            if (!CheckInDialog.showDialogIfNecessary(requireActivity(), Type.SHOW, episodeTitle,
                episodeId)) {
              episodeScheduler.checkin(episodeId, null, false, false, false);
              checkInDrawable.setWatching(true);
            }
          }
        }
        return true;

      case R.id.action_checkin_cancel:
        episodeScheduler.cancelCheckin();
        return true;

      case R.id.action_collection_add:
        episodeScheduler.setIsInCollection(episodeId, true);
        return true;

      case R.id.action_collection_remove:
        episodeScheduler.setIsInCollection(episodeId, false);
        return true;

      case R.id.action_watchlist_add:
        episodeScheduler.setIsInWatchlist(episodeId, true);
        return true;

      case R.id.action_watchlist_remove:
        episodeScheduler.setIsInWatchlist(episodeId, false);
        return true;

      case R.id.action_list_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.EPISODE, episodeId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;

      default:
        return super.onMenuItemClick(item);
    }
  }

  private void updateEpisodeViews(Episode episode) {
    this.episode = episode;

    loaded = true;

    showId = episode.getShowId();
    seasonId = episode.getSeasonId();
    showTitle = episode.getShowTitle();

    watched = episode.getWatched();
    collected = episode.getInCollection();
    inWatchlist = episode.getInWatchlist();
    watching = episode.getWatching();
    checkedIn = episode.getCheckedIn();

    season = episode.getSeason();
    episodeTitle = DataHelper.getEpisodeTitle(requireContext(), episode.getTitle(), season,
        episode.getEpisode(), episode.getWatched());

    title.setText(episodeTitle);

    overview.setText(episode.getOverview());

    final String screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, ImageType.STILL, episodeId);

    setBackdrop(screenshotUri, true);

    firstAired.setText(
        DateStringUtils.getAirdateInterval(requireContext(), episode.getFirstAired(), true));

    if (checkInDrawable != null) {
      checkInDrawable.setWatching(watching || checkedIn);
    }

    final boolean hasCheckmark = watched || collected || inWatchlist;
    checkmarks.setVisibility(hasCheckmark ? View.VISIBLE : View.GONE);
    watchedView.setVisibility(watched ? View.VISIBLE : View.GONE);
    inCollectionView.setVisibility(collected ? View.VISIBLE : View.GONE);
    inWatchlistView.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

    currentRating = episode.getUserRating();
    final float ratingAll = episode.getRating();
    rating.setValue(ratingAll);

    viewOnTrakt.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intents.openUrl(requireContext(), TraktUtils.getTraktEpisodeUrl(episode.getTraktId()));
      }
    });

    if (TraktTimestamps.shouldSyncComments(episode.getLastCommentSync())) {
      episodeScheduler.syncComments(episodeId);
    }

    invalidateMenu();
  }

  private void updateComments() {
    LinearCommentsAdapter.updateComments(requireContext(), commentsContainer, userComments, comments);
    commentsParent.setVisibility(View.VISIBLE);
  }
}
