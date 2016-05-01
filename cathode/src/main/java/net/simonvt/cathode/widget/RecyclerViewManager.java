/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.widget;

import android.support.v7.widget.RecyclerView;
import android.view.View;

public class RecyclerViewManager {

  RecyclerView recyclerView;

  View emptyView;

  RecyclerView.Adapter adapter;

  public RecyclerViewManager(RecyclerView recyclerView, RecyclerView.LayoutManager layoutManager,
      View emptyView) {
    this.recyclerView = recyclerView;
    recyclerView.setLayoutManager(layoutManager);
    this.emptyView = emptyView;
  }

  public void setAdapter(RecyclerView.Adapter adapter) {
    if (adapter != this.adapter) {
      if (this.adapter != null) {
        this.adapter.unregisterAdapterDataObserver(adapterObserver);
      }

      this.adapter = adapter;
      if (this.adapter != null) {
        if (recyclerView != null) {
          recyclerView.setAdapter(this.adapter);
        }

        adapter.registerAdapterDataObserver(adapterObserver);

        if (adapter.getItemCount() > 0) {
          emptyView.setVisibility(View.GONE);
          recyclerView.setVisibility(View.VISIBLE);
        } else {
          emptyView.setVisibility(View.VISIBLE);
          recyclerView.setVisibility(View.GONE);
        }
      }
    }
  }

  private void showEmptyView(boolean show) {
    if (show && emptyView.getVisibility() == View.GONE) {
      emptyView.setAlpha(0.0f);
      emptyView.animate().alpha(1.0f).withStartAction(new Runnable() {
        @Override public void run() {
          emptyView.setVisibility(View.VISIBLE);
        }
      });
      recyclerView.animate().alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          recyclerView.setVisibility(View.GONE);
          recyclerView.setAlpha(1.0f);
        }
      });
    } else if (!show && emptyView.getVisibility() == View.VISIBLE) {
      emptyView.animate().alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          emptyView.setAlpha(1.0f);
          emptyView.setVisibility(View.GONE);
        }
      });
      recyclerView.animate().alpha(1.0f).withStartAction(new Runnable() {
        @Override public void run() {
          recyclerView.setAlpha(0.0f);
          recyclerView.setVisibility(View.VISIBLE);
        }
      });
    }
  }

  private RecyclerView.AdapterDataObserver adapterObserver =
      new RecyclerView.AdapterDataObserver() {
        @Override public void onChanged() {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeChanged(int positionStart, int itemCount) {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeInserted(int positionStart, int itemCount) {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }
      };
}
