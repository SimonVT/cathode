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

package net.simonvt.cathode.widget;

import androidx.recyclerview.widget.RecyclerView;

public abstract class AdapterCountDataObserver extends RecyclerView.AdapterDataObserver {

  private RecyclerView.Adapter adapter;

  public AdapterCountDataObserver(RecyclerView.Adapter adapter) {
    this.adapter = adapter;
  }

  @Override public void onChanged() {
    final int adapterItemCount = adapter.getItemCount();
    onCountChanged(adapterItemCount);
  }

  @Override public void onItemRangeChanged(int positionStart, int itemCount) {
    final int adapterItemCount = adapter.getItemCount();
    onCountChanged(adapterItemCount);
  }

  @Override public void onItemRangeInserted(int positionStart, int itemCount) {
    final int adapterItemCount = adapter.getItemCount();
    onCountChanged(adapterItemCount);
  }

  @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
    final int adapterItemCount = adapter.getItemCount();
    onCountChanged(adapterItemCount);
  }

  public abstract void onCountChanged(int itemCount);
}
