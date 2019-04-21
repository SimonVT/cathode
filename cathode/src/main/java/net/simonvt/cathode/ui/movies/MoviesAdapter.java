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
package net.simonvt.cathode.ui.movies;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.widget.OverflowView;
import net.simonvt.cathode.entity.Movie;

public class MoviesAdapter extends BaseMoviesAdapter<BaseMoviesAdapter.ViewHolder> {

  private int rowLayout;

  public MoviesAdapter(FragmentActivity activity, Callbacks callbacks) {
    this(activity, callbacks, R.layout.list_row_movie);
  }

  public MoviesAdapter(FragmentActivity activity, Callbacks callbacks, int rowLayout) {
    super(activity, callbacks);
    this.rowLayout = rowLayout;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(rowLayout, parent, false);
    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          Movie movie = getList().get(position);
          callbacks.onMovieClicked(movie.getId(), movie.getTitle(), movie.getOverview());
        }
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
      }

      @Override public void onPopupDismissed() {
      }

      @Override public void onActionSelected(int action) {
        final int position = holder.getAdapterPosition();
        if (position != RecyclerView.NO_POSITION) {
          onOverflowActionSelected(holder.itemView, holder.getItemId(), action, position,
              holder.title.getText().toString());
        }
      }
    });

    return holder;
  }
}
