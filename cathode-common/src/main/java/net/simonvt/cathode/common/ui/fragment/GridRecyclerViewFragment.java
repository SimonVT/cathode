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

package net.simonvt.cathode.common.ui.fragment;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.widget.GridLayoutItemSpacing;

public abstract class GridRecyclerViewFragment<T extends RecyclerView.ViewHolder>
    extends RecyclerViewFragment<T> {

  private GridLayoutItemSpacing itemSpacing;

  protected int getColumnCount() {
    return 1;
  }

  protected int getHorizontalSpacing() {
    return getResources().getDimensionPixelSize(R.dimen.recyclerViewHorizontalSpacing);
  }

  protected int getVerticalSpacing() {
    return getResources().getDimensionPixelSize(R.dimen.recyclerViewVerticalSpacing);
  }

  protected GridLayoutManager.SpanSizeLookup getSpanSizeLookup() {
    return null;
  }

  @Override protected void addItemDecorations(RecyclerView recyclerView) {
    GridLayoutManager layoutManager = (GridLayoutManager) recyclerView.getLayoutManager();

    if (itemSpacing == null) {
      final int horizontalSpacing = getHorizontalSpacing();
      final int verticalSpacing = getVerticalSpacing();
      itemSpacing = new GridLayoutItemSpacing(layoutManager, horizontalSpacing, verticalSpacing);
    }

    recyclerView.addItemDecoration(itemSpacing);
  }

  @Override protected RecyclerView.LayoutManager getLayoutManager() {
    GridLayoutManager layoutManager = new GridLayoutManager(getActivity(), getColumnCount());
    GridLayoutManager.SpanSizeLookup spanSizeLookup = getSpanSizeLookup();
    if (spanSizeLookup != null) {
      layoutManager.setSpanSizeLookup(spanSizeLookup);
    }

    return layoutManager;
  }
}
