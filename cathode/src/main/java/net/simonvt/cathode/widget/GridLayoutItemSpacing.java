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

import android.graphics.Rect;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class GridLayoutItemSpacing extends RecyclerView.ItemDecoration {

  private GridLayoutManager layoutManager;

  private final int columnCount;

  private int horizontalSpacing;

  private int halfHorizontalSpacing;

  private int verticalSpacing;

  private int halfVerticalSpacing;

  public GridLayoutItemSpacing(GridLayoutManager layoutManager, int horizontalSpacing,
      int verticalSpacing) {
    this.layoutManager = layoutManager;
    this.columnCount = layoutManager.getSpanCount();
    this.horizontalSpacing = horizontalSpacing;
    halfHorizontalSpacing = horizontalSpacing / 2;
    this.verticalSpacing = verticalSpacing;
    halfVerticalSpacing = verticalSpacing / 2;
  }

  @Override public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
      RecyclerView.State state) {
    GridLayoutManager.LayoutParams params = (GridLayoutManager.LayoutParams) view.getLayoutParams();
    final int position = params.getViewPosition();

    if (columnCount == 1) {
      if (position == 0) {
        outRect.top = 0;
      } else {
        outRect.top = verticalSpacing;
      }

      outRect.left = 0;
      outRect.right = 0;
      outRect.bottom = 0;
    } else {
      GridLayoutManager.SpanSizeLookup spanSizeLookup = layoutManager.getSpanSizeLookup();

      boolean isFirstRow;

      if (position < columnCount) {
        isFirstRow = spanSizeLookup.getSpanGroupIndex(position, columnCount) == 0;
      } else {
        isFirstRow = false;
      }

      if (isFirstRow) {
        outRect.top = 0;
      } else {
        outRect.top = verticalSpacing;
      }

      final int index = spanSizeLookup.getSpanIndex(position, columnCount);
      final int span = spanSizeLookup.getSpanSize(position);

      if (index + span - 1 == columnCount - 1) {
        outRect.right = 0;
      } else {
        outRect.right = verticalSpacing;
      }

      outRect.left = 0;
      outRect.bottom = 0;
    }
  }
}
