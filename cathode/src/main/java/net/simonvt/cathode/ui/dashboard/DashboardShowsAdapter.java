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
package net.simonvt.cathode.ui.dashboard;

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
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.Show;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;

public class DashboardShowsAdapter extends BaseAdapter<Show, DashboardShowsAdapter.ViewHolder> {

  private DashboardFragment.OverviewCallback callback;

  public DashboardShowsAdapter(Context context, DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull Show oldItem, @NonNull Show newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_show, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Show show = getList().get(position);
          callback.onDisplayShow(show.getId(), show.getTitle(), show.getOverview());
        }
      }
    });

    return holder;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Show show = getList().get(position);
    final String poster = ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, show.getId());
    holder.poster.setImage(poster);
    holder.title.setText(show.getTitle());
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;

    ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }
}
