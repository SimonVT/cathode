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

package net.simonvt.cathode.search;

import android.graphics.drawable.Animatable;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.util.DiffUtil;
import android.support.v7.widget.AppCompatImageView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.List;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.widget.CircularProgressIndicator;
import net.simonvt.cathode.widget.RemoteImageView;

public class SearchAdapter extends RecyclerView.Adapter<SearchAdapter.ViewHolder> {

  public interface OnResultClickListener {

    void onShowClicked(long showId, String title, String overview);

    void onMovieClicked(long movieId, String title, String overview);

    void onQueryClicked(String query);
  }

  private static final int TYPE_RECENT = 0;
  private static final int TYPE_RESULT = 1;
  private static final int TYPE_SEARCH = 2;
  private static final int TYPE_SEARCHING = 3;
  private static final int TYPE_NO_RESULTS = 4;

  private OnResultClickListener listener;

  private List<String> recentQueries;

  private boolean displaySearching;

  private List<Result> results;

  public SearchAdapter(OnResultClickListener listener) {
    this.listener = listener;
  }

  public void setRecentQueries(List<String> recentQueries) {
    List<String> oldQueries = this.recentQueries;
    this.recentQueries = recentQueries;

    final boolean hadItems = oldQueries != null && oldQueries.size() > 0;
    final boolean hasItems = recentQueries.size() > 0;

    if (hasItems) {
      if (!hadItems) {
        notifyItemInserted(0);
      } else {
        notifyItemChanged(0);
      }
    } else {
      if (hadItems) {
        notifyItemRemoved(0);
      } else {
        notifyItemChanged(0);
      }
    }
  }

  private boolean hasRecentQueries() {
    return recentQueries != null && recentQueries.size() > 0;
  }

  public void setSearching(boolean displaySearching) {
    if (displaySearching != this.displaySearching) {
      this.displaySearching = displaySearching;

      final boolean hasRecentQueries = hasRecentQueries();
      final int offset = hasRecentQueries ? 1 : 0;

      if (displaySearching) {
        if (results != null && results.size() > 0) {
          notifyItemRangeRemoved(offset, results.size());
        } else {
          notifyItemRemoved(offset);
        }

        notifyItemInserted(offset);
      } else {
        notifyItemRemoved(offset);

        if (results != null && results.size() > 0) {
          notifyItemRangeInserted(offset, results.size());
        } else {
          notifyItemInserted(offset);
        }
      }
    }
  }

  public void setResults(final List<Result> results) {
    final List<Result> oldResults = this.results;
    this.results = results;

    final boolean hasRecentQueries = hasRecentQueries();
    final int offset = hasRecentQueries ? 1 : 0;

    if (!displaySearching) {
      if (oldResults == null) {
        if (results != null) {
          notifyItemRemoved(offset);

          if (results.size() == 0) {
            notifyItemInserted(offset);
          } else {
            notifyItemRangeInserted(offset, results.size());
          }
        }
      } else if (oldResults.size() == 0) {
        if (results == null) {
          notifyItemRemoved(offset);
          notifyItemInserted(offset);
        } else if (results.size() > 0) {
          notifyItemRemoved(offset);
          notifyItemRangeInserted(offset, results.size());
        }
      } else {
        if (results == null || results.size() == 0) {
          notifyItemRangeRemoved(offset, oldResults.size());
          notifyItemInserted(offset);
        } else {
          diffLists(oldResults, results);
        }
      }
    }
  }

  private void diffLists(final List<Result> oldResults, final List<Result> newResults) {
    final boolean hasRecentQueries = hasRecentQueries();
    final int offset = hasRecentQueries ? 1 : 0;

    DiffUtil.calculateDiff(new DiffUtil.Callback() {
      @Override public int getOldListSize() {
        int size = 0;
        if (hasRecentQueries) {
          size += 1;
        }
        if (oldResults != null) {
          size += oldResults.size();
        }
        return size;
      }

      @Override public int getNewListSize() {
        int size = 0;
        if (hasRecentQueries) {
          size += 1;
        }
        if (newResults != null) {
          size += newResults.size();
        }
        return size;
      }

      @Override public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
        if (hasRecentQueries) {
          if (oldItemPosition == 0 || newItemPosition == 0) {
            return oldItemPosition == newItemPosition;
          }
        }

        final Result oldItem = oldResults.get(oldItemPosition - offset);
        final Result newItem = newResults.get(newItemPosition - offset);

        return newItem.getItemType() == oldItem.getItemType()
            && newItem.getItemId() == oldItem.getItemId();
      }

      @Override public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
        if (hasRecentQueries && oldItemPosition == 0 && newItemPosition == 0) {
          return true;
        }

        final Result oldItem = oldResults.get(oldItemPosition - offset);
        final Result newItem = newResults.get(newItemPosition - offset);

        return newItem.equals(oldItem);
      }
    }).dispatchUpdatesTo(this);
  }

  @Override public int getItemCount() {
    int size = 0;

    if (recentQueries != null && recentQueries.size() > 0) {
      size += 1;
    }

    if (!displaySearching && results != null && results.size() > 0) {
      size += results.size();
    } else {
      size += 1;
    }

    return size;
  }

  @Override public int getItemViewType(int position) {
    if (position == 0 && hasRecentQueries()) {
      return TYPE_RECENT;
    }

    if (displaySearching) {
      return TYPE_SEARCHING;
    }

    if (results == null) {
      return TYPE_SEARCH;
    }

    if (results.size() == 0) {
      return TYPE_NO_RESULTS;
    }

    return TYPE_RESULT;
  }

  @Override public void onViewAttachedToWindow(ViewHolder holder) {
    super.onViewAttachedToWindow(holder);

    if (holder.getItemViewType() == TYPE_SEARCHING) {
      ((SearchingHolder) holder).drawable.start();
    }
  }

  @Override public void onViewDetachedFromWindow(ViewHolder holder) {
    super.onViewDetachedFromWindow(holder);

    if (holder.getItemViewType() == TYPE_SEARCHING) {
      ((SearchingHolder) holder).drawable.stop();
    }
  }

  @Override public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_SEARCHING) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.search_searching, parent, false);

      return new SearchingHolder(view);
    } else if (viewType == TYPE_SEARCH) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.search_something, parent, false);
      return new ViewHolder(view);
    } else if (viewType == TYPE_NO_RESULTS) {
      View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.search_no_results, parent, false);
      return new ViewHolder(view);
    } else if (viewType == TYPE_RECENT) {
      View view =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.search_recents, parent, false);

      final RecentsViewHolder holder = new RecentsViewHolder(view);

      VectorDrawableCompat icon = VectorDrawableCompat.create(parent.getContext().getResources(), R.drawable.ic_search_history_24dp, null);

      holder.query1.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
      holder.query2.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);
      holder.query3.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null);

      holder.query1.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          listener.onQueryClicked(holder.queryOneQuery);
        }
      });

      holder.query2.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          listener.onQueryClicked(holder.queryTwoQuery);
        }
      });

      holder.query3.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          listener.onQueryClicked(holder.queryThreeQuery);
        }
      });

      return holder;
    } else {
      View view =
          LayoutInflater.from(parent.getContext()).inflate(R.layout.search_result, parent, false);

      final ViewHolder holder = new ResultViewHolder(view);

      view.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View view) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            final boolean hasRecentQueries = hasRecentQueries();
            final int offset = hasRecentQueries ? 1 : 0;

            Result result = results.get(position - offset);
            if (result.getItemType() == ItemType.SHOW) {
              listener.onShowClicked(result.getItemId(), result.getTitle(), result.getOverview());
            } else {
              listener.onMovieClicked(result.getItemId(), result.getTitle(), result.getOverview());
            }
          }
        }
      });

      return holder;
    }
  }

  @Override public void onBindViewHolder(ViewHolder holder, int position) {
    if (holder.getItemViewType() == TYPE_RECENT) {
      RecentsViewHolder recentsHolder = (RecentsViewHolder) holder;

      final int recentQueryCount = recentQueries.size();

      recentsHolder.queryOneQuery = recentQueries.get(0);
      recentsHolder.query1.setText(recentsHolder.queryOneQuery);

      if (recentQueryCount >= 2) {
        recentsHolder.queryTwoQuery = recentQueries.get(1);
        recentsHolder.query2.setText(recentsHolder.queryTwoQuery);
        recentsHolder.query2.setVisibility(View.VISIBLE);
      } else {
        recentsHolder.query2.setVisibility(View.GONE);
      }

      if (recentQueryCount >= 3) {
        recentsHolder.queryThreeQuery = recentQueries.get(2);
        recentsHolder.query3.setText(recentsHolder.queryThreeQuery);
        recentsHolder.query3.setVisibility(View.VISIBLE);
      } else {
        recentsHolder.query3.setVisibility(View.GONE);
      }
    } else if (holder.getItemViewType() == TYPE_RESULT) {
      final int offset = hasRecentQueries() ? 1 : 0;
      ResultViewHolder resultHolder = (ResultViewHolder) holder;
      Result result = results.get(position - offset);

      resultHolder.poster.setImage(result.getPoster());
      resultHolder.title.setText(result.getTitle());
      resultHolder.overview.setText(result.getOverview());
      resultHolder.rating.setValue(result.getRating());
    }
  }

  static class ViewHolder extends RecyclerView.ViewHolder {

    public ViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  static class SearchingHolder extends ViewHolder {

    @BindView(R.id.searching) AppCompatImageView searching;
    Animatable drawable;

    public SearchingHolder(View itemView) {
      super(itemView);
      drawable = (Animatable) searching.getDrawable();
    }
  }

  static class RecentsViewHolder extends ViewHolder {

    @BindView(R.id.queries) ViewGroup queries;
    @BindView(R.id.query1) TextView query1;
    @BindView(R.id.query2) TextView query2;
    @BindView(R.id.query3) TextView query3;

    String queryOneQuery;
    String queryTwoQuery;
    String queryThreeQuery;

    public RecentsViewHolder(View itemView) {
      super(itemView);
    }
  }

  static class ResultViewHolder extends ViewHolder {

    @BindView(R.id.poster) RemoteImageView poster;
    @BindView(R.id.title) TextView title;
    @BindView(R.id.overview) TextView overview;
    @BindView(R.id.rating) CircularProgressIndicator rating;

    public ResultViewHolder(View itemView) {
      super(itemView);
    }
  }
}
