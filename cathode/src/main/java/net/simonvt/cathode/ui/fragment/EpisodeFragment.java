package net.simonvt.cathode.ui.fragment;

import android.content.res.Configuration;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.RatingBar;
import android.widget.TextView;
import butterknife.InjectView;
import butterknife.Optional;
import butterknife.Views;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.scheduler.EpisodeTaskScheduler;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.FragmentContract;
import net.simonvt.cathode.ui.dialog.RatingDialog;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.widget.ObservableScrollView;
import net.simonvt.cathode.widget.OverflowView;
import net.simonvt.cathode.widget.RemoteImageView;

public class EpisodeFragment extends DialogFragment implements FragmentContract {

  private static final String TAG = "EpisodeFragment";

  private static final String ARG_EPISODEID =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.episodeId";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.showTitle";

  private static final String DIALOG_RATING =
      "net.simonvt.cathode.ui.fragment.EpisodeFragment.ratingDialog";

  private static final int STATE_NONE = -1;
  private static final int STATE_PROGRESS_VISIBLE = 0;
  private static final int STATE_CONTENT_VISIBLE = 1;

  private long episodeId;

  @Inject EpisodeTaskScheduler episodeScheduler;

  @InjectView(R.id.title) TextView title;
  @InjectView(R.id.screen) RemoteImageView screen;
  @InjectView(R.id.overview) TextView overview;
  @InjectView(R.id.firstAired) TextView firstAired;

  @InjectView(R.id.ratingContainer) View ratingContainer;
  @InjectView(R.id.rating) RatingBar rating;
  @InjectView(R.id.allRatings) TextView allRatings;

  @InjectView(R.id.isWatched) View watchedView;
  @InjectView(R.id.inCollection) View inCollectionView;
  @InjectView(R.id.inWatchlist) View inWatchlistView;

  @InjectView(R.id.contentContainer) ObservableScrollView content;

  @InjectView(R.id.progressContainer) View progress;

  @InjectView(R.id.overflow) @Optional OverflowView overflow;

  private boolean animating;

  private boolean wait;

  private int currentState = STATE_PROGRESS_VISIBLE;
  private int pendingStateChange = STATE_NONE;

  private String showTitle;

  private int currentRating;

  private boolean loaded;

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private boolean isTablet;

  public static Bundle getArgs(long episodeId, String showTitle) {
    Bundle args = new Bundle();
    args.putLong(ARG_EPISODEID, episodeId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    return args;
  }

  @Override
  public void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    CathodeApp.inject(getActivity(), this);

    isTablet = getResources().getBoolean(R.bool.isTablet);

    Bundle args = getArguments();
    episodeId = args.getLong(ARG_EPISODEID);
    showTitle = args.getString(ARG_SHOW_TITLE);
    getLoaderManager().initLoader(BaseActivity.LOADER_EPISODE, null, episodeCallbacks);

    setHasOptionsMenu(true);

    if (getShowsDialog()) {
      setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }
  }

  @Override
  public String getTitle() {
    return showTitle;
  }

  @Override
  public String getSubtitle() {
    return null;
  }

  @Override
  public boolean onBackPressed() {
    return false;
  }

  @Override
  public View onCreateView(LayoutInflater inflater, ViewGroup container,
      Bundle savedInstanceState) {
    return inflater.inflate(R.layout.fragment_episode, container, false);
  }

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    Views.inject(this, view);

    wait = true;
    view.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            wait = false;

            if (currentState == STATE_CONTENT_VISIBLE) {
              content.setAlpha(1.0f);
              content.setVisibility(View.VISIBLE);
              progress.setVisibility(View.GONE);
            } else {
              content.setVisibility(View.GONE);
              progress.setAlpha(1.0f);
              progress.setVisibility(View.VISIBLE);
            }

            if (!isTablet
                && getResources().getConfiguration().orientation
                == Configuration.ORIENTATION_LANDSCAPE) {
              content.setScrollY(screen.getHeight() / 2);
            }
          }
        });

    ratingContainer.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        RatingDialog.newInstance(RatingDialog.Type.EPISODE, episodeId, currentRating)
            .show(getFragmentManager(), DIALOG_RATING);
      }
    });

    if (!isTablet) {
      content.setListener(new ObservableScrollView.ScrollListener() {
        @Override
        public void onScrollChanged(int l, int t) {
          final int offset = (int) (t / 2.0f);
          screen.setTranslationY(offset);
        }
      });
    }

    if (overflow != null) {
      overflow.setListener(new OverflowView.OverflowActionListener() {
        @Override
        public void onPopupShown() {
        }

        @Override
        public void onPopupDismissed() {
        }

        @Override
        public void onActionSelected(int action) {
          onActionSelected(action);
        }
      });
    }
  }

  private void populateOverflow() {
    overflow.setVisibility(View.VISIBLE);
    if (watched) {
      overflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
    } else {
      overflow.addItem(R.id.action_watched, R.string.action_watched);
      if (inWatchlist) {
        overflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
      } else {
        overflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
      }
    }

    if (collected) {
      overflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
    } else {
      overflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
    }
  }

  @Override
  public void onDestroyView() {
    Views.reset(this);
    super.onDestroyView();
  }

  @Override
  public void onDestroy() {
    if (getActivity().isFinishing() || isRemoving()) {
      getLoaderManager().destroyLoader(BaseActivity.LOADER_EPISODE);
    }
    super.onDestroy();
  }

  @Override
  public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
    if (loaded) {
      if (watched) {
        menu.add(0, R.id.action_unwatched, 1, R.string.action_unwatched);
      } else {
        menu.add(0, R.id.action_watched, 2, R.string.action_watched);
        if (inWatchlist) {
          menu.add(0, R.id.action_watchlist_remove, 5, R.string.action_watchlist_remove);
        } else {
          menu.add(0, R.id.action_watchlist_add, 6, R.string.action_watchlist_add);
        }
      }

      if (collected) {
        menu.add(0, R.id.action_collection_remove, 3, R.string.action_collection_remove);
      } else {
        menu.add(0, R.id.action_collection_add, 4, R.string.action_collection_add);
      }
    }
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    return onActionSelected(item.getItemId());
  }

  private boolean onActionSelected(int action) {
    switch (action) {
      case R.id.action_watched:
        episodeScheduler.setWatched(episodeId, true);
        return true;

      case R.id.action_unwatched:
        episodeScheduler.setWatched(episodeId, false);
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
    }

    return false;
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    Animation animation = null;
    if (nextAnim != 0) {
      animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
      animation.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
          animating = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
          animating = false;
          if (pendingStateChange != STATE_NONE) {
            changeState(pendingStateChange, true);
            pendingStateChange = STATE_NONE;
          }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
      });
    }

    return animation;
  }

  protected void setContentVisible(boolean contentVisible) {
    if (getView() == null) {
      currentState = contentVisible ? STATE_CONTENT_VISIBLE : STATE_PROGRESS_VISIBLE;
      return;
    }

    if (contentVisible) {
      changeState(STATE_CONTENT_VISIBLE, true);
    } else {
      changeState(STATE_PROGRESS_VISIBLE, true);
    }
  }

  private void changeState(final int newState, final boolean animate) {
    if (newState == currentState) {
      return;
    }

    if (animating) {
      pendingStateChange = newState;
      return;
    }

    currentState = newState;

    if (wait || progress == null) {
      return;
    }

    if (newState == STATE_PROGRESS_VISIBLE && content.getVisibility() != View.VISIBLE) {
      return;
    }

    if (newState == STATE_CONTENT_VISIBLE && !animate) {
      content.setVisibility(View.VISIBLE);
      progress.setVisibility(View.GONE);
    } else if (newState == STATE_PROGRESS_VISIBLE && !animate) {
      content.setVisibility(View.GONE);
      progress.setVisibility(View.VISIBLE);
    } else {
      content.setVisibility(View.VISIBLE);
      progress.setVisibility(View.VISIBLE);

      if (newState == STATE_CONTENT_VISIBLE) {
        progress.animate().alpha(0.0f);
        if (content.getAlpha() == 1.0f) content.setAlpha(0.0f);
        content.animate().alpha(1.0f).withEndAction(new Runnable() {
          @Override
          public void run() {
            if (progress == null) {
              // In case fragment is removed before animation is done
              return;
            }
            progress.setVisibility(View.GONE);
          }
        });
      } else {
        if (progress.getAlpha() == 1.0f) progress.setAlpha(0.0f);
        progress.animate().alpha(1.0f);
        content.animate().alpha(0.0f).withEndAction(new Runnable() {
          @Override
          public void run() {
            if (progress == null) {
              // In case fragment is removed before animation is done
              return;
            }
            content.setVisibility(View.GONE);
          }
        });
      }
    }
  }

  private void updateEpisodeViews(final Cursor cursor) {
    if (cursor.moveToFirst()) {
      loaded = true;

      title.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Episodes.TITLE)));
      overview.setText(cursor.getString(cursor.getColumnIndex(CathodeContract.Episodes.OVERVIEW)));
      screen.setImage(cursor.getString(cursor.getColumnIndex(CathodeContract.Episodes.SCREEN)));
      firstAired.setText(DateUtils.millisToString(getActivity(),
          cursor.getLong(cursor.getColumnIndex(CathodeContract.Episodes.FIRST_AIRED)), true));

      watched = cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.WATCHED)) == 1;
      collected = cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.IN_COLLECTION)) == 1;
      inWatchlist = cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.IN_WATCHLIST)) == 1;

      watchedView.setVisibility(watched ? View.VISIBLE : View.GONE);
      inCollectionView.setVisibility(collected ? View.VISIBLE : View.GONE);
      inWatchlistView.setVisibility(inWatchlist ? View.VISIBLE : View.GONE);

      currentRating = cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.RATING));
      final int ratingAll =
          cursor.getInt(cursor.getColumnIndex(CathodeContract.Episodes.RATING_PERCENTAGE));
      rating.setRating(currentRating);
      allRatings.setText(ratingAll + "%");

      setContentVisible(true);
      getActivity().invalidateOptionsMenu();

      if (getShowsDialog()) populateOverflow();
    }
  }

  private static final String[] EPISODE_PROJECTION = new String[] {
      CathodeContract.Episodes.TITLE, CathodeContract.Episodes.SCREEN, CathodeContract.Episodes.OVERVIEW,
      CathodeContract.Episodes.FIRST_AIRED, CathodeContract.Episodes.WATCHED,
      CathodeContract.Episodes.IN_COLLECTION, CathodeContract.Episodes.IN_WATCHLIST,
      CathodeContract.Episodes.RATING, CathodeContract.Episodes.RATING_PERCENTAGE,
  };

  private LoaderManager.LoaderCallbacks<Cursor> episodeCallbacks =
      new LoaderManager.LoaderCallbacks<Cursor>() {
        @Override
        public Loader<Cursor> onCreateLoader(int id, Bundle args) {
          CursorLoader cl =
              new CursorLoader(getActivity(), CathodeContract.Episodes.buildFromId(episodeId),
                  EPISODE_PROJECTION, null, null, null);
          cl.setUpdateThrottle(2 * android.text.format.DateUtils.SECOND_IN_MILLIS);
          return cl;
        }

        @Override
        public void onLoadFinished(Loader<Cursor> cursorLoader, Cursor data) {
          updateEpisodeViews(data);
        }

        @Override
        public void onLoaderReset(Loader<Cursor> cursorLoader) {
        }
      };
}
