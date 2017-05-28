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
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.R;
import net.simonvt.cathode.images.ImageType;
import net.simonvt.cathode.images.ImageUri;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic;
import net.simonvt.cathode.scheduler.ShowTaskScheduler;
import net.simonvt.cathode.ui.adapter.RecyclerCursorAdapter;
import net.simonvt.cathode.widget.RemoteImageView;
import net.simonvt.schematic.Cursors;

public class DashboardShowsAdapter extends RecyclerCursorAdapter<DashboardShowsAdapter.ViewHolder> {

  static final String[] PROJECTION = new String[] {
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.ID,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.TITLE,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.OVERVIEW,
      DatabaseSchematic.Tables.SHOWS + "." + ShowColumns.LAST_MODIFIED,
  };

  @Inject ShowTaskScheduler showScheduler;

  private DashboardFragment.OverviewCallback callback;

  public DashboardShowsAdapter(Context context, DashboardFragment.OverviewCallback callback) {
    super(context);
    this.callback = callback;

    Injector.obtain().inject(this);
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    final View view = LayoutInflater.from(parent.getContext())
        .inflate(R.layout.list_row_dashboard_show, parent, false);
    final ViewHolder holder = new ViewHolder(view);

    view.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Cursor cursor = getCursor(position);
          final String title = Cursors.getString(cursor, ShowColumns.TITLE);
          final String overview = Cursors.getString(cursor, ShowColumns.OVERVIEW);
          callback.onDisplayShow(holder.getItemId(), title, overview);
        }
      }
    });

    return holder;
  }

  @Override protected void onBindViewHolder(ViewHolder holder, Cursor cursor, int position) {
    final long id = Cursors.getLong(cursor, ShowColumns.ID);
    final String poster =
        ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, id);

    holder.poster.setImage(poster);
    holder.title.setText(Cursors.getString(cursor, ShowColumns.TITLE));
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
