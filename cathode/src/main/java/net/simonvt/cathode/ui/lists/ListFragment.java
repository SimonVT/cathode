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
import android.os.Bundle;
import android.view.MenuItem;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.support.AndroidSupportInjection;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.ListItem;
import net.simonvt.cathode.common.entity.UserList;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.common.util.guava.Preconditions;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.helper.ListWrapper;
import net.simonvt.cathode.remote.sync.lists.SyncList;
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler;
import net.simonvt.cathode.ui.LibraryType;
import net.simonvt.cathode.ui.NavigationListener;

public class ListFragment extends ToolbarSwipeRefreshRecyclerFragment<ListAdapter.ListViewHolder>
    implements ListAdapter.ListListener {

  public static final String TAG = "net.simonvt.cathode.ui.lists.ListFragment";

  private static final String ARG_LIST_ID = "net.simonvt.cathode.ui.lists.ListFragment.lidtId";
  private static final String ARG_LIST_NAME = "net.simonvt.cathode.ui.lists.ListFragment.listName";

  static final String DIALOG_UPDATE = "net.simonvt.cathode.ui.lists.ListFragment.updateListsDialog";
  static final String DIALOG_DELETE = "net.simonvt.cathode.ui.lists.ListFragment.deleteListsDialog";

  @Inject ListsTaskScheduler listScheduler;

  @Inject JobManager jobManager;

  private NavigationListener navigationListener;

  private long listId;

  private ListViewModel viewModel;

  private ListAdapter adapter;

  private int columnCount;

  private UserList listInfo;

  public static Bundle getArgs(long listId, String listName) {
    Preconditions.checkArgument(listId >= 0, "listId must be >= 0");

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
    AndroidSupportInjection.inject(this);

    Bundle args = getArguments();
    listId = args.getLong(ARG_LIST_ID);
    String listName = args.getString(ARG_LIST_NAME);
    setTitle(listName);

    columnCount = getResources().getInteger(R.integer.listColumns);

    viewModel = ViewModelProviders.of(this).get(ListViewModel.class);
    viewModel.setListId(listId);
    viewModel.getList().observe(this, new Observer<UserList>() {
      @Override public void onChanged(UserList userList) {
        listInfo = userList;
        setTitle(userList.getName());
      }
    });
    viewModel.getListItems().observe(this, new Observer<List<ListItem>>() {
      @Override public void onChanged(List<ListItem> listItems) {
        setListItems(listItems);
      }
    });
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
        final long traktId = ListWrapper.getTraktId(requireContext().getContentResolver(), listId);
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
        if (listInfo != null) {
          UpdateListFragment updateFragment = new UpdateListFragment();
          updateFragment.setArguments(
              UpdateListFragment.getArgs(listId, listInfo.getName(), listInfo.getDescription(),
                  listInfo.getPrivacy(), listInfo.getDisplayNumbers(),
                  listInfo.getAllowComments()));
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

  @Override public void onEpisodeClick(long id) {
    navigationListener.onDisplayEpisode(id, null);
  }

  @Override public void onMovieClicked(long movieId, String title, String overview) {
    navigationListener.onDisplayMovie(movieId, title, overview);
  }

  @Override public void onPersonClick(long personId) {
    navigationListener.onDisplayPerson(personId);
  }

  @Override public void onRemoveItem(int position, ListItem listItem) {
    int itemType;
    switch (listItem.getType()) {
      case SHOW:
        itemType = DatabaseContract.ItemType.SHOW;
        break;
      case SEASON:
        itemType = DatabaseContract.ItemType.SEASON;
        break;
      case EPISODE:
        itemType = DatabaseContract.ItemType.EPISODE;
        break;
      case MOVIE:
        itemType = DatabaseContract.ItemType.MOVIE;
        break;
      case PERSON:
        itemType = DatabaseContract.ItemType.PERSON;
        break;
      default:
        throw new IllegalStateException("Unknown item type: " + listItem.getType().toString());
    }

    listScheduler.removeItem(listId, itemType, listItem.getListItemId());
    adapter.removeItem(listItem);
  }

  private void setListItems(List<ListItem> items) {
    if (adapter == null) {
      adapter = new ListAdapter(requireContext(), this);
      setAdapter(adapter);
    }

    adapter.setList(items);
  }
}
