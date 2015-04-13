/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.adapter;

import android.database.Cursor;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.OverflowView;

public class MoviesAdapter extends BaseMoviesAdapter<BaseMoviesAdapter.ViewHolder> {

  private int rowLayout;

  public MoviesAdapter(FragmentActivity activity, MovieClickListener listener, Cursor c) {
    this(activity, listener, c, R.layout.list_row_movie);
  }

  public MoviesAdapter(FragmentActivity activity, MovieClickListener listener, Cursor c,
      int rowLayout) {
    super(activity, listener, c);
    this.rowLayout = rowLayout;
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View v = LayoutInflater.from(getContext()).inflate(rowLayout, parent, false);
    final ViewHolder holder = new ViewHolder(v);

    v.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        listener.onMovieClicked(holder.itemView, holder.getAdapterPosition(), holder.getItemId());
      }
    });

    holder.overflow.setListener(new OverflowView.OverflowActionListener() {
      @Override public void onPopupShown() {
        holder.setIsRecyclable(false);
      }

      @Override public void onPopupDismissed() {
        holder.setIsRecyclable(false);
      }

      @Override public void onActionSelected(int action) {
        holder.setIsRecyclable(true);
        onOverflowActionSelected(holder.itemView, holder.getItemId(), action,
            holder.getAdapterPosition(), holder.title.getText().toString());
      }
    });

    return holder;
  }
}
