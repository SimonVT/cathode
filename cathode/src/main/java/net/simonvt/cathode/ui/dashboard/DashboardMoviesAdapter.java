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
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.ui.adapter.BaseAdapter;
import net.simonvt.cathode.common.widget.RemoteImageView;
import net.simonvt.cathode.entity.Movie;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;

public class DashboardMoviesAdapter extends BaseAdapter<Movie, DashboardMoviesAdapter.ViewHolder> {

  private DashboardFragment.OverviewCallback callback;

  public DashboardMoviesAdapter(Context context, DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;
    setHasStableIds(true);
  }

  @Override public long getItemId(int position) {
    return getList().get(position).getId();
  }

  @Override protected boolean areItemsTheSame(@NonNull Movie oldItem, @NonNull Movie newItem) {
    return oldItem.getId() == newItem.getId();
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_movie, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Movie movie = getList().get(position);
          callback.onDisplayMovie(holder.getItemId(), movie.getTitle(), movie.getOverview());
        }
      }
    });

    return holder;
  }

  @Override public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
    Movie movie = getList().get(position);
    final String poster = ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, movie.getId());
    holder.poster.setImage(poster);
    holder.title.setText(movie.getTitle());
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    RemoteImageView poster;
    TextView title;

    ViewHolder(View itemView) {
      super(itemView);
      poster = itemView.findViewById(R.id.poster);
      title = itemView.findViewById(R.id.title);
    }
  }
}
