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

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import butterknife.BindView;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.entity.UserList;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;

public class ListsAdapter extends BaseAdapter<UserList, ListsAdapter.ViewHolder> {

  public interface OnListClickListener {

    void onListClicked(long listId, String listName);
  }

  private OnListClickListener listener;

  public ListsAdapter(OnListClickListener listener, Context context) {
    super(context);
    this.listener = listener;
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override
  protected boolean areItemsTheSame(@NonNull UserList oldItem, @NonNull UserList newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(R.layout.list_row_list, parent, false);
    final ViewHolder vh = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        listener.onListClicked(vh.getItemId(), vh.name.getText().toString());
      }
    });

    return vh;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    UserList userList = getList().get(position);

    holder.name.setText(userList.getName());
    holder.description.setText(userList.getDescription());
  }

  public static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.name) TextView name;

    @BindView(R.id.description) TextView description;

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
