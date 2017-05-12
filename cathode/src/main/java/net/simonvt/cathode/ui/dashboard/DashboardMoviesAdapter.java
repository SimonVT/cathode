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
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.scheduler.MovieTaskScheduler;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class DashboardMoviesAdapter
    extends RecyclerCursorAdapter<DashboardMoviesAdapter.ViewHolder> {

  public static final String[] PROJECTION = new String[] {
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.ID,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.TITLE,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.OVERVIEW,
      DatabaseSchematic.Tables.MOVIES + "." + MovieColumns.LAST_MODIFIED,
  };

  @Inject MovieTaskScheduler movieScheduler;

  private DashboardFragment.OverviewCallback callback;

  public DashboardMoviesAdapter(Context context, DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;

    CathodeApp.inject(context, this);
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_movie, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final String title = Cursors.getString(cursor, MovieColumns.TITLE);
          final String overview = Cursors.getString(cursor, MovieColumns.OVERVIEW);
          callback.onDisplayMovie(holder.getItemId(), title, overview);
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, MovieColumns.ID);
    final String poster =
        ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, id);

    holder.poster.setImage(poster);
    holder.title.setText(Cursors.getString(cursor, MovieColumns.TITLE));
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
