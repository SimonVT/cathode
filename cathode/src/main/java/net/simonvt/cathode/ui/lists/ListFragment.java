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

package net.simonvt.cathode.ui.lists;

import android.app.Activity;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.database.SimpleCursor;
import net.simonvt.cathode.database.SimpleCursorLoader;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.LastModifiedColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ListWrapper;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.remote.sync.lists.SyncList;
import net.simonvt.cathode.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;
import net.simonvt.cathode.ui.shows.ShowClickListener;
import net.simonvt.cathode.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.ui.listener.EpisodeClickListener;
import net.simonvt.cathode.ui.listener.MovieClickListener;
import net.simonvt.cathode.ui.listener.SeasonClickListener;
import net.simonvt.cathode.util.SqlCoalesce;
import net.simonvt.cathode.util.SqlColumn;
import net.simonvt.schematic.Cursors;

public class ListFragment extends ToolbarSwipeRefreshRecyclerFragment<ListAdapter.ListViewHolder>
    implements ShowClickListener, SeasonClickListener, EpisodeClickListener, MovieClickListener,
    ListAdapter.OnRemoveItemListener {

  public static final String TAG = "net.simonvt.cathode.ui.lists.ListFragment";

  private static final String ARG_LIST_ID = "net.simonvt.cathode.ui.lists.ListFragment.lidtId";
  private static final String ARG_LIST_NAME = "net.simonvt.cathode.ui.lists.ListFragment.listName";

  static final String DIALOG_UPDATE = "net.simonvt.cathode.ui.lists.ListFragment.updateListsDialog";
  static final String DIALOG_DELETE = "net.simonvt.cathode.ui.lists.ListFragment.deleteListsDialog";

  private static final int LOADER_LIST_INFO = 1;
  private static final int LOADER_LIST_ITEMS = 2;

  @Inject ListsTaskScheduler listScheduler;

  @Inject JobManager jobManager;

  private NavigationListener navigationListener;

  private long listId;

  private ListAdapter adapter;

  private int columnCount;

  private Cursor listInfo;

  public static Bundle getArgs(long listId, String listName) {
    Bundle args = new Bundle();
    args.putLong(ARG_LIST_ID, listId);
    args.putString(ARG_LIST_NAME, listName);
    return args;
  }

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    navigationListener = (NavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);

    Bundle args = getArguments();
    listId = args.getLong(ARG_LIST_ID);
    String listName = args.getString(ARG_LIST_NAME);
    setTitle(listName);

    columnCount = getResources().getInteger(R.integer.listColumns);

    getLoaderManager().initLoader(LOADER_LIST_ITEMS, null, itemsLoader);
    getLoaderManager().initLoader(LOADER_LIST_INFO, null, infoLoader);
  }

  @Override protected int getColumnCount() {
    return columnCount;
  }

  public long getListId() {
    return listId;
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    new Thread(new Runnable() {
      @Override public void run() {
        final long traktId = ListWrapper.getTraktId(getActivity().getContentResolver(), listId);
        Job job = new SyncList(traktId);
        job.registerOnDoneListener(onDoneListener);
        jobManager.addJob(job);
      }
    }).start();
  }

  @Override public void createMenu(Toolbar toolbar) {
    toolbar.inflateMenu(R.menu.fragment_list);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_list_edit:
        if (listInfo != null && listInfo.moveToFirst()) {
          final String name = Cursors.getString(listInfo, ListsColumns.NAME);
          final String description = Cursors.getString(listInfo, ListsColumns.DESCRIPTION);
          final Privacy privacy =
              Privacy.fromValue(Cursors.getString(listInfo, ListsColumns.PRIVACY));
          final boolean displayNumbers = Cursors.getBoolean(listInfo, ListsColumns.DISPLAY_NUMBERS);
          final boolean allowComments = Cursors.getBoolean(listInfo, ListsColumns.ALLOW_COMMENTS);
          UpdateListFragment updateFragment = new UpdateListFragment();
          updateFragment.setArguments(
              UpdateListFragment.getArgs(listId, name, description, privacy, displayNumbers,
                  allowComments));
          updateFragment.show(getFragmentManager(), DIALOG_UPDATE);
        }
        return true;

      case R.id.menu_list_delete:
        DeleteListDialog.newInstance(listId).show(getFragmentManager(), DIALOG_DELETE);
        return true;
    }
    return super.onMenuItemClick(item);
  }

  @Override public void onShowClick(long showId, String title, String overview) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED);
  }

  @Override
  public void onSeasonClick(long showId, long seasonId, String showTitle, int seasonNumber) {
    navigationListener.onDisplaySeason(showId, seasonId, showTitle, seasonNumber,
        LibraryType.WATCHED);
  }

  @Override public void onEpisodeClick(View view, int position, long id) {
    navigationListener.onDisplayEpisode(id, null);
  }

  @Override public void onMovieClicked(View v, int position, long id) {
    Cursor cursor = adapter.getCursor(position);
    final String title = Cursors.getString(cursor, MovieColumns.TITLE);
    final String overview = Cursors.getString(cursor, MovieColumns.OVERVIEW);
    navigationListener.onDisplayMovie(id, title, overview);
  }

  @Override public void onRemoveItem(int position, long id) {
    Loader loader = getLoaderManager().getLoader(LOADER_LIST_ITEMS);
    if (loader != null) {
      ((SimpleCursorLoader) loader).throttle(SimpleCursorLoader.DEFAULT_THROTTLE);
    }

    final SimpleCursor cursor = (SimpleCursor) adapter.getCursor(position);

    final long itemId = Cursors.getLong(cursor, ListItemColumns.ITEM_ID);
    final int itemType = Cursors.getInt(cursor, ListItemColumns.ITEM_TYPE);
    listScheduler.removeItem(listId, itemType, itemId);

    cursor.remove(id);
    adapter.notifyChanged();
  }

  private void setCursor(SimpleCursor cursor) {
    if (adapter == null) {
      adapter = new ListAdapter(getActivity(), this, this, this, this, this);
      setAdapter(adapter);
    }

    adapter.changeCursor(cursor);
  }

  private LoaderManager.LoaderCallbacks<SimpleCursor> infoLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getContext(), ProviderSchematic.Lists.withId(listId),
              new String[] {
                  ListsColumns.ID, ListsColumns.NAME, ListsColumns.DESCRIPTION,
                  ListsColumns.PRIVACY, ListsColumns.DISPLAY_NUMBERS, ListsColumns.ALLOW_COMMENTS,
              }, null, null, null);
        }

        @Override public void onLoadFinished(Loader<SimpleCursor> loader, SimpleCursor data) {
          listInfo = data;
          if (listInfo.moveToFirst()) {
            final String name = Cursors.getString(listInfo, ListsColumns.NAME);
            setTitle(name);
          }
        }

        @Override public void onLoaderReset(Loader<SimpleCursor> loader) {
        }
      };

  private static final String[] PROJECTION = {
      SqlColumn.table(Tables.LIST_ITEMS).column(ListItemColumns.ID), ListItemColumns.ITEM_TYPE,
      ListItemColumns.ITEM_ID,

      SqlCoalesce.coaloesce(SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
          SqlColumn.table(Tables.MOVIES).column(MovieColumns.OVERVIEW)).as(
          ListItemColumns.OVERVIEW),

      SqlCoalesce.coaloesce(SqlColumn.table(Tables.SHOWS).column(ShowColumns.POSTER),
          SqlColumn.table(Tables.MOVIES).column(MovieColumns.POSTER)).as(ListItemColumns.POSTER),

      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.WATCHED_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_COLLECTION_COUNT),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.IN_WATCHLIST),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
      SqlColumn.table(Tables.SHOWS).column(LastModifiedColumns.LAST_MODIFIED),

      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SEASON),
      SqlColumn.table(Tables.SEASONS).column(SeasonColumns.SHOW_ID),
      SqlColumn.table(Tables.SEASONS).column(LastModifiedColumns.LAST_MODIFIED), "(SELECT "
      + ShowColumns.TITLE
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.SEASONS
      + "."
      + SeasonColumns.SHOW_ID
      + ") AS seasonShowTitle", "(SELECT "
      + ShowColumns.POSTER
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.SEASONS
      + "."
      + SeasonColumns.SHOW_ID
      + ") AS seasonShowPoster",

      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.TITLE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SEASON),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.EPISODE),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.WATCHED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.FIRST_AIRED),
      SqlColumn.table(Tables.EPISODES).column(EpisodeColumns.SCREENSHOT),
      SqlColumn.table(Tables.EPISODES).column(LastModifiedColumns.LAST_MODIFIED), "(SELECT "
      + ShowColumns.TITLE
      + " FROM "
      + Tables.SHOWS
      + " WHERE "
      + Tables.SHOWS
      + "."
      + ShowColumns.ID
      + "="
      + Tables.EPISODES
      + "."
      + EpisodeColumns.SHOW_ID
      + ") AS episodeShowTitle",

      SqlColumn.table(Tables.MOVIES).column(MovieColumns.TITLE),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHED),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_COLLECTION),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.IN_WATCHLIST),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.WATCHING),
      SqlColumn.table(Tables.MOVIES).column(MovieColumns.CHECKED_IN),
      SqlColumn.table(Tables.MOVIES).column(LastModifiedColumns.LAST_MODIFIED),

      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.HEADSHOT),
      SqlColumn.table(Tables.PEOPLE).column(PersonColumns.NAME),
      SqlColumn.table(Tables.PEOPLE).column(LastModifiedColumns.LAST_MODIFIED),
  };

  LoaderManager.LoaderCallbacks<SimpleCursor> itemsLoader =
      new LoaderManager.LoaderCallbacks<SimpleCursor>() {
        @Override public Loader<SimpleCursor> onCreateLoader(int id, Bundle args) {
          return new SimpleCursorLoader(getActivity(), ProviderSchematic.ListItems.inList(listId),
              PROJECTION, null, null, null);
        }

        @Override public void onLoadFinished(Loader loader, SimpleCursor data) {
          setCursor(data);
        }

        @Override public void onLoaderReset(Loader loader) {
        }
      };
}
