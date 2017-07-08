/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

import android.content.Context;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import butterknife.BindView;
import butterknife.ButterKnife;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.R2;
import timber.log.Timber;

public class CategoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

  public interface CategoryClickListener {

    void onCategoryClick(int category);
  }

  private class Category extends RecyclerView.AdapterDataObserver {

    final int category;

    final long categoryId;

    final long adapterId;

    RecyclerView.Adapter adapter;

    private int adapterCount;

    Category(int category, long categoryId, long adapterId) {
      this.category = category;
      this.categoryId = categoryId;
      this.adapterId = adapterId;
    }

    public void setAdapter(RecyclerView.Adapter adapter) {
      Timber.d("Setting adapter");
      if (this.adapter != null) {
        this.adapter.unregisterAdapterDataObserver(this);
      }
      this.adapter = adapter;
      adapter.registerAdapterDataObserver(this);
      onAdapterChanged();
    }

    private void onAdapterChanged() {
      final int adapterCount = adapter.getItemCount();

      Timber.d("onAdapterChanged: %d - %d", this.adapterCount, adapterCount);

      if (adapterCount == 0 && this.adapterCount > 0) {
        onHideCategory(this);
      } else if (this.adapterCount == 0 && adapterCount > 0) {
        onShowCategory(this);
      }
      this.adapterCount = adapterCount;
    }

    @Override public void onChanged() {
      onAdapterChanged();
    }

    @Override public void onItemRangeChanged(int positionStart, int itemCount) {
      onAdapterChanged();
    }

    @Override public void onItemRangeChanged(int positionStart, int itemCount, Object payload) {
      onAdapterChanged();
    }

    @Override public void onItemRangeInserted(int positionStart, int itemCount) {
      onAdapterChanged();
    }

    @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
      onAdapterChanged();
    }

    @Override public void onItemRangeMoved(int fromPosition, int toPosition, int itemCount) {
      onAdapterChanged();
    }
  }

  private static final int TYPE_CATEGORY = 0;
  private static final int TYPE_ITEMS = 1;

  List<Category> categories = new ArrayList<>();

  private long nextId;

  private Context context;

  private CategoryClickListener clickListener;

  private boolean allCategoriesLoaded;

  public CategoryAdapter(Context context, CategoryClickListener clickListener) {
    this.context = context;
    this.clickListener = clickListener;
  }

  public void initCategory(int category) {
    categories.add(new Category(category, nextId++, nextId++));
  }

  public void setAdapter(int category, RecyclerView.Adapter adapter) {
    for (Category c : categories) {
      if (category == c.category) {
        c.setAdapter(adapter);
        break;
      }
    }

    if (allCategoriesLoaded) {
      return;
    }

    for (Category c : categories) {
      if (c.adapter == null) {
        return;
      }
    }

    allCategoriesLoaded = true;

    notifyItemRangeInserted(0, getItemCount());
  }

  private void onShowCategory(Category category) {
    Timber.d("onShowCategory");
    if (allCategoriesLoaded) {
      final int position = getPositionOfCategory(category);
      notifyItemRangeInserted(position, 2);
    }
  }

  private void onHideCategory(Category category) {
    Timber.d("onHideCategory");
    if (allCategoriesLoaded) {
      final int position = getPositionOfCategory(category);
      notifyItemRangeRemoved(position, 2);
    }
  }

  @Override public int getItemCount() {
    if (!allCategoriesLoaded) {
      return 0;
    }

    int count = 0;
    for (Category category : categories) {
      if (category.adapterCount > 0) {
        count += 2;
      }
    }
    return count;
  }

  private int getPositionOfCategory(Category category) {
    int position = 0;
    for (Category c : categories) {
      if (category == c) {
        return position;
      }

      if (c.adapterCount > 0) {
        position += 2;
      }
    }

    throw new IllegalStateException("No category found for category");
  }

  private Category getCategoryForPosition(int position) {
    int offset = 0;
    for (Category category : categories) {
      if (category.adapterCount > 0) {
        offset += 2;
        if (position < offset) {
          return category;
        }
      }
    }

    throw new IllegalStateException("No category found for position " + position);
  }

  @Override public int getItemViewType(int position) {
    return position % 2 == 0 ? TYPE_CATEGORY : TYPE_ITEMS;
  }

  @Override public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    if (viewType == TYPE_CATEGORY) {
      final View view = LayoutInflater.from(parent.getContext())
          .inflate(R.layout.category_adapter_category, parent, false);
      final CategoryViewHolder holder = new CategoryViewHolder(view);

      view.setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          final int position = holder.getAdapterPosition();
          if (position != RecyclerView.NO_POSITION) {
            Category category = getCategoryForPosition(position);
            clickListener.onCategoryClick(category.category);
          }
        }
      });

      return holder;
    } else {
      final RecyclerView recyclerView = (RecyclerView) LayoutInflater.from(parent.getContext())
          .inflate(R.layout.category_adapter_recyclerview, parent, false);
      recyclerView.setLayoutManager(
          new LinearLayoutManager(context, LinearLayoutManager.HORIZONTAL, false));
      ItemsViewHolder holder = new ItemsViewHolder(recyclerView);

      return holder;
    }
  }

  @Override public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
    Category category = getCategoryForPosition(position);
    if (holder.getItemViewType() == TYPE_CATEGORY) {
      CategoryViewHolder cHolder = (CategoryViewHolder) holder;
      cHolder.category.setText(category.category);
    } else {
      ItemsViewHolder iHolder = (ItemsViewHolder) holder;
      iHolder.recyclerView.setAdapter(category.adapter);
    }
  }

  static final class CategoryViewHolder extends RecyclerView.ViewHolder {

    @BindView(R2.id.category) TextView category;

    CategoryViewHolder(View itemView) {
      super(itemView);
      ButterKnife.bind(this, itemView);
    }
  }

  static final class ItemsViewHolder extends RecyclerView.ViewHolder {

    RecyclerView recyclerView;

    ItemsViewHolder(RecyclerView recyclerView) {
      super(recyclerView);
      this.recyclerView = recyclerView;
    }
  }
}
