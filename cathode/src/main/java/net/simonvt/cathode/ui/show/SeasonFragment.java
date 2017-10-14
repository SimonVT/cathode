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
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.Injector;
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.scheduler.SeasonTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.ShowsNavigationListener;
import net.simonvt.cathode.ui.history.AddToHistoryDialog;
import net.simonvt.cathode.ui.history.RemoveFromHistoryDialog;
import net.simonvt.cathode.ui.listener.EpisodeClickListener;
import net.simonvt.cathode.ui.lists.ListsDialog;
import net.simonvt.schematic.Cursors;

public class SeasonFragment extends ToolbarGridFragment<SeasonAdapter.ViewHolder>
    implements EpisodeClickListener {

  public static final String TAG = "net.simonvt.cathode.ui.show.SeasonFragment";

  private static final int LOADER_SEASON = 1;

  private static final String ARG_SHOW_ID = "net.simonvt.cathode.ui.show.SeasonFragment.showId";
  private static final String ARG_SEASONID = "net.simonvt.cathode.ui.show.SeasonFragment.seasonId";
  private static final String ARG_SHOW_TITLE =
      "net.simonvt.cathode.ui.show.SeasonFragment.showTitle";
  private static final String ARG_SEASON_NUMBER =
      "net.simonvt.cathode.ui.show.SeasonFragment.seasonNumber";
  private static final String ARG_TYPE = "net.simonvt.cathode.ui.show.SeasonFragment.type";

  private static final String DIALOG_LISTS_ADD =
      "net.simonvt.cathode.ui.show.SeasonFragment.listsAddDialog";

  @Inject SeasonTaskScheduler seasonScheduler;

  private long showId;

  private long seasonId;

  private LibraryType type;

  private String title;

  private int seasonNumber = -1;

  private SeasonAdapter episodeAdapter;

  private ShowsNavigationListener navigationListener;

  private int columnCount;

  private int count = -1;
  private int watchedCount = -1;
  private int collectedCount = -1;

  public static Bundle getArgs(long showId, long seasonId, String showTitle, int seasonNumber,
      LibraryType type) {
    Preconditions.checkArgument(showId >= 0, "showId must be >= 0");
    Preconditions.checkArgument(seasonId >= 0, "seasonId must be >= 0");

    Bundle args = new Bundle();
    args.putLong(ARG_SHOW_ID, showId);
    args.putLong(ARG_SEASONID, seasonId);
    args.putString(ARG_SHOW_TITLE, showTitle);
    args.putInt(ARG_SEASON_NUMBER, seasonNumber);
    args.putSerializable(ARG_TYPE, type);
    return args;
  }

  public long getSeasonId() {
    return seasonId;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (ShowsNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    Injector.inject(this);

    Bundle args = getArguments();
    showId = args.getLong(ARG_SHOW_ID);
    seasonId = args.getLong(ARG_SEASONID);
    title = args.getString(ARG_SHOW_TITLE);
    seasonNumber = args.getInt(ARG_SEASON_NUMBER);
    type = (LibraryType) args.getSerializable(ARG_TYPE);

    setTitle(title);
    updateSubtitle();

    getLoaderManager().initLoader(LOADER_SEASON, null, episodesLoader);

    columnCount = getResources().getInteger(R.integer.episodesColumns);
  }

  public void updateSubtitle() {
    if (seasonNumber == -1) {
      return;
    }
    String subtitle;
    if (seasonNumber == 0) {
      subtitle = getResources().getString(R.string.season_special);
    } else {
      subtitle = getResources().getString(R.string.season_x, seasonNumber);
    }

    setSubtitle(subtitle);
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    Menu menu = toolbar.getMenu();

    if (count > 0) {
      menu.add(0, R.id.action_history_add, 0, R.string.action_history_add);
      if (watchedCount > 0) {
        menu.add(0, R.id.action_history_remove, 0, R.string.action_history_remove);
      }
      if (collectedCount < count) {
        menu.add(0, R.id.action_collection_add, 0, R.string.action_collection_add);
      }
      if (collectedCount > 0) {
        menu.add(0, R.id.action_collection_remove, 0, R.string.action_collection_remove);
      }
    }

    menu.add(0, R.id.action_list_add, 0, R.string.action_list_add);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.action_list_add:
        ListsDialog.newInstance(DatabaseContract.ItemType.SEASON, seasonId)
            .show(getFragmentManager(), DIALOG_LISTS_ADD);
        return true;

      case R.id.action_history_add:
        AddToHistoryDialog.newInstance(AddToHistoryDialog.Type.SEASON, seasonId,
            getString(R.string.season_x, seasonNumber))
            .show(getFragmentManager(), AddToHistoryDialog.TAG);
        return true;

      case R.id.action_history_remove:
        RemoveFromHistoryDialog.newInstance(RemoveFromHistoryDialog.Type.SEASON, seasonId,
            getContext().getString(R.string.season_x, seasonNumber))
            .show(getFragmentManager(), RemoveFromHistoryDialog.TAG);
        return true;

      case R.id.action_collection_add:
        seasonScheduler.setInCollection(seasonId, true);
        return true;

      case R.id.action_collection_remove:
        seasonScheduler.setInCollection(seasonId, false);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  @Override public boolean displaysMenuIcon() {
    return false;
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    getToolbar().setNavigationOnClickListener(navigationClickListener);
  }

  private View.OnClickListener navigationClickListener = new View.OnClickListener() {
    @Override public void onClick(View v) {
      navigationListener.onHomeClicked();
    }
  };

  @Override public void onEpisodeClick(long id) {
    navigationListener.onDisplayEpisode(id, title);
  }

  private void setCursor(Cursor cursor) {
    if (cursor != null) {
      cursor.moveToPosition(-1);
      final int count = cursor.getCount();
      int watchedCount = 0;
      int collectedCount = 0;
      while (cursor.moveToNext()) {
        final boolean watched = Cursors.getBoolean(cursor, EpisodeColumns.WATCHED);
        final boolean collected = Cursors.getBoolean(cursor, EpisodeColumns.IN_COLLECTION);

        if (watched) {
          watchedCount++;
        }
        if (collected) {
          collectedCount++;
        }
      }

      if (count != this.count
          || watchedCount != this.watchedCount
          || collectedCount != this.collectedCount) {
        this.count = count;
        this.watchedCount = watchedCount;
        this.collectedCount = collectedCount;

        invalidateMenu();
      }
    } else {
      count = -1;
      watchedCount = -1;
      collectedCount = -1;
    }

    if (episodeAdapter == null) {
      episodeAdapter = new SeasonAdapter(getActivity(), this, cursor, type);
      setAdapter(episodeAdapter);
      return;
    }

    episodeAdapter.changeCursor(cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> episodesLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), Episodes.fromSeason(seasonId),
              SeasonAdapter.PROJECTION, null, null, EpisodeColumns.EPISODE + " ASC");
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> cursorLoader, SimpleCursor data) {
          setCursor(data);
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> cursorLoader) {
        }
      };
}
