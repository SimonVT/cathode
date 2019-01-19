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
import net.simonvt.cathode.common.entity.UserList;
import net.simonvt.cathode.common.ui.fragment.ToolbarSwipeRefreshRecyclerFragment;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.sync.lists.SyncLists;
import net.simonvt.cathode.ui.ListNavigationListener;

public class ListsFragment extends ToolbarSwipeRefreshRecyclerFragment<ListsAdapter.ViewHolder>
    implements ListsAdapter.OnListClickListener {

  public static final String TAG = "net.simonvt.cathode.ui.lists.ListsFragment";

  static final String DIALOG_LIST_CREATE = "net.simonvt.cathode.ui.HomeActivity.createListFragment";

  @Inject JobManager jobManager;

  private ListNavigationListener listener;

  private ListsViewModel viewModel;

  private ListsAdapter adapter;

  @Override public void onAttach(Activity activity) {
    super.onAttach(activity);
    listener = (ListNavigationListener) activity;
  }

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    AndroidSupportInjection.inject(this);
    setTitle(R.string.navigation_lists);
    setEmptyText(R.string.empty_lists);

    viewModel = ViewModelProviders.of(this).get(ListsViewModel.class);
    viewModel.getLists().observe(this, new Observer<List<UserList>>() {
      @Override public void onChanged(List<UserList> userLists) {
        setLists(userLists);
      }
    });
  }

  @Override protected int getColumnCount() {
    return 1;
  }

  private Job.OnDoneListener onDoneListener = new Job.OnDoneListener() {
    @Override public void onDone(Job job) {
      setRefreshing(false);
    }
  };

  @Override public void onRefresh() {
    Job job = new SyncLists();
    job.registerOnDoneListener(onDoneListener);
    jobManager.addJob(job);
  }

  @Override public void onListClicked(long listId, String listName) {
    listener.onShowList(listId, listName);
  }

  @Override public boolean displaysMenuIcon() {
    return true;
  }

  @Override public void createMenu(Toolbar toolbar) {
    super.createMenu(toolbar);
    toolbar.inflateMenu(R.menu.fragment_lists);
  }

  @Override public boolean onMenuItemClick(MenuItem item) {
    switch (item.getItemId()) {
      case R.id.menu_list_create:
        CreateListFragment dialog = new CreateListFragment();
        dialog.show(getFragmentManager(), DIALOG_LIST_CREATE);
        return true;
    }

    return super.onMenuItemClick(item);
  }

  private void setLists(List<UserList> userLists) {
    if (adapter == null) {
      adapter = new ListsAdapter(this, requireContext());
      setAdapter(adapter);
    }

    adapter.setList(userLists);
  }
}
