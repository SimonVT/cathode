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

import android.content.Context;
import android.graphics.PointF;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v7.widget.LinearSmoothScroller;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.util.Log;
import android.util.SparseArray;
import android.view.View;
import android.view.ViewGroup;
import timber.log.Timber;

public class HeaderGridLayoutManager extends RecyclerView.LayoutManager {

  private static final String TAG = "HeaderGridLayoutManager";

  public static final int TYPE_HEADER = 0x11EAD;
  public static final int INVALID_POSITION = -1;

  private Context context;

  private int colCount = 2;
  private int verticalItemMargin;
  private int horizontalItemMargin;

  private int colWidth;

  private SparseArray<LayoutRecord> layoutRecords = new SparseArray<LayoutRecord>();

  int anchorPosition;
  int anchorOffset;
  long anchorId = -1L;

  private SavedState pendingSavedState;

  private RecyclerView recyclerView;

  private Runnable onItemsChanged;

  private static class RowInfo {

    int y;
    int lastPos;
  }

  final RowInfo rowInfo = new RowInfo();

  private class Row {
    View[] views = new View[colCount];
    int bottom;
  }

  public HeaderGridLayoutManager(Context context, int colCount) {
    this.context = context;
    this.colCount = colCount;

    verticalItemMargin = dpToPx(4);
    horizontalItemMargin = dpToPx(4);
  }

  protected int dpToPx(int dp) {
    return (int) (context.getResources().getDisplayMetrics().density * dp + 0.5f);
  }

  public void setVerticalItemMargin(int verticalItemMargin) {
    this.verticalItemMargin = verticalItemMargin;
    requestLayout();
  }

  public void setHorizontalItemMargin(int horizontalItemMargin) {
    this.horizontalItemMargin = horizontalItemMargin;
    requestLayout();
  }

  @Override public void onAttachedToWindow(RecyclerView view) {
    super.onAttachedToWindow(view);
    if (view != recyclerView) {
      recyclerView = view;
    }
  }

  @Override public void onDetachedFromWindow(RecyclerView view) {
    super.onDetachedFromWindow(view);
    recyclerView = null;
  }

  @Override public boolean supportsPredictiveItemAnimations() {
    return false;
  }

  private static final class LayoutRecord {

    int column;
    boolean isHeader;

    private LayoutRecord(int column, boolean isHeader) {
      this.column = column;
      this.isHeader = isHeader;
    }
  }

  @Override public void onLayoutChildren(RecyclerView.Recycler recycler, RecyclerView.State state) {
    if (colWidth == -1) {
      colWidth =
          (getWidth() - getPaddingStart() - getPaddingEnd() - horizontalItemMargin * (colCount - 1))
              / colCount;
    }

    if (pendingSavedState != null) {
      anchorPosition = pendingSavedState.anchorPosition;
      anchorOffset = pendingSavedState.anchorOffset;
      anchorId = pendingSavedState.anchorId;
      pendingSavedState = null;
    }

    if (onItemsChanged != null) {
      onItemsChanged.run();
      onItemsChanged = null;
    }

    detachAndScrapAttachedViews(recycler);

    fillDown(recycler, state, anchorPosition, 0);
    adjustViewsDown();
    fillUp(recycler, state, anchorPosition - 1, 0);
    correctTooLow();
  }

  private void correctTooLow() {
    if (getChildCount() > 0) {
      final View firstChild = getChildAt(0);
      final int top = firstChild.getTop();
      if (top > getPaddingTop()) {
        final int delta = -top + getPaddingTop();
        offsetChildrenVertical(delta);
      }
    }
  }

  private void adjustViewsDown() {
    final int childCount = getChildCount();
    if (childCount > 0) {
      int lowestBottom = Integer.MIN_VALUE;
      for (int i = getChildCount() - 1; i >= getChildCount() - colCount && i >= 0; i--) {
        final View child = getChildAt(i);
        final int bottom = getDecoratedBottom(child);
        if (bottom > lowestBottom) {
          lowestBottom = bottom;
        }
      }

      final int delta = getHeight() - getPaddingBottom() - lowestBottom;

      if (delta > 0) {
        offsetChildrenVertical(delta);
      }
    }
  }

  private void recycleOffscreenViews(RecyclerView.Recycler recycler) {
    final int height = getHeight();
    final int childCount = getChildCount();

    for (int i = childCount - 1; i >= 0; i--) {
      final View child = getChildAt(i);
      final int top = getDecoratedTop(child);
      if (top > height) {
        removeAndRecycleView(child, recycler);
      }
    }

    while (getChildCount() > 0) {
      Row row = getFirstRow();
      if (row.bottom >= 0) {
        break;
      }

      for (View child : row.views) {
        if (child != null) {
          removeAndRecycleView(child, recycler);
          setAnchorPosition(anchorPosition + 1);
        }
      }
    }
  }

  private Row getFirstRow() {
    final int childCount = getChildCount();

    Row row = new Row();
    row.bottom = Integer.MIN_VALUE;

    boolean first = true;

    for (int i = 0; i < colCount && i < childCount; i++) {
      final View child = getChildAt(i);
      final boolean isHeader = layoutRecords.get(i).isHeader;
      LayoutParams params = (LayoutParams) child.getLayoutParams();

      if (isHeader) {
        if (first) {
          row.views[0] = child;
          row.bottom = getDecoratedBottom(child);
        }

        return row;
      }

      final int bottom = getDecoratedBottom(child);
      if (bottom > row.bottom) {
        row.bottom = bottom;
      }

      row.views[i] = child;
    }

    return row;
  }

  private int getBottomMost() {
    int lowestBottom = Integer.MIN_VALUE;

    for (int i = getChildCount() - 1; i >= getChildCount() - colCount && i > 0; i--) {
      final View child = getChildAt(i);
      final int bottom = getDecoratedBottom(child);
      if (bottom > lowestBottom) {
        lowestBottom = bottom;
      }
    }

    return lowestBottom;
  }

  private void setAnchorPosition(int position) {
    if (position != anchorPosition) {
      anchorPosition = position;
      final View anchor = getChildAt(0);
      if (anchor != null) {
        anchorOffset = getDecoratedTop(anchor) - getPaddingTop();
      } else {
        anchorOffset = 0;
      }

      if (recyclerView.getAdapter().hasStableIds()) {
        anchorId = recyclerView.getAdapter().getItemId(position);
      }
    }
  }

  private int fillDown(RecyclerView.Recycler recycler, RecyclerView.State state, int fromPosition,
      int overhang) {
    final int height = getHeight();
    final int itemCount = state.getItemCount();
    int position = fromPosition;

    int bottomMost = getPaddingTop();

    if (fromPosition == anchorPosition) {
      bottomMost = getPaddingTop() + anchorOffset - verticalItemMargin;
    } else if (getChildCount() > 0) {
      bottomMost = getBottomMost();
    }

    while (position < itemCount && bottomMost < height + overhang) {
      RowInfo row = makeRow(recycler, state, position, bottomMost, true);

      position = row.lastPos + 1;
      bottomMost = row.y;
    }

    int lowestBottom = getBottomMost();

    return lowestBottom - (height - getPaddingBottom());
  }

  private int fillUp(RecyclerView.Recycler recycler, RecyclerView.State state, int fromPosition,
      int overhang) {
    int position = fromPosition;
    int topMost = getPaddingTop();

    if (fromPosition == anchorPosition) {
      topMost = anchorOffset;
    } else if (getChildCount() > 0) {
      final View child = getChildAt(0);
      topMost = getDecoratedTop(child);
    }

    while (position >= 0 && topMost > -overhang) {
      RowInfo row = makeRow(recycler, state, position, topMost, false);

      position = row.lastPos - 1;
      topMost = row.y;

      setAnchorPosition(row.lastPos);
    }

    int highestTop = getPaddingTop();
    final View firstChild = getChildAt(0);
    if (firstChild != null) {
      highestTop = getDecoratedTop(firstChild);
    }

    return getPaddingTop() - highestTop;
  }

  private View makeAndAddHeader(RecyclerView.Recycler recycler, RecyclerView.State state,
      int position, int y, boolean fillDown) {
    final View header = recycler.getViewForPosition(position);
    return makeAndAddHeader(state, header, y, fillDown);
  }

  private View makeAndAddHeader(RecyclerView.State state, View header, int y, boolean fillDown) {
    LayoutParams params = (LayoutParams) header.getLayoutParams();
    if (!params.isItemRemoved()) {
      addView(header, fillDown ? getChildCount() : 0);
    }
    measureChildWithMargins(header, 0, 0);

    int left = getPaddingLeft();
    int top;
    int right = left + getDecoratedMeasuredWidth(header) + params.leftMargin + params.rightMargin;
    int bottom;

    if (fillDown) {
      top = y + verticalItemMargin;
      bottom = top + getDecoratedMeasuredHeight(header) + params.topMargin + params.bottomMargin;
    } else {
      bottom = y - verticalItemMargin;
      top = bottom - getDecoratedMeasuredHeight(header) - params.topMargin - params.bottomMargin;
    }

    layoutDecorated(header, left + params.leftMargin, top + params.topMargin,
        right - params.leftMargin, bottom - params.bottomMargin);

    return header;
  }

  private View makeAndAddItem(RecyclerView.Recycler recycler, RecyclerView.State state,
      int position, int column, int y, boolean fillDown) {
    final View item = recycler.getViewForPosition(position);
    return makeAndAddItem(state, item, column, y, fillDown);
  }

  private View makeAndAddItem(RecyclerView.State state, View item, int column, int y,
      boolean fillDown) {
    LayoutParams params = (LayoutParams) item.getLayoutParams();
    if (!params.isItemRemoved()) {
      addView(item, fillDown ? getChildCount() : 0);
    }
    int widthUsed = colWidth * (colCount - 1) + horizontalItemMargin * (colCount - 1);
    measureChildWithMargins(item, widthUsed, 0);

    final int left = getPaddingLeft() + colWidth * column + horizontalItemMargin * column;
    final int right =
        left + getDecoratedMeasuredWidth(item) + params.leftMargin + params.rightMargin;
    int top;
    int bottom;

    if (fillDown) {
      top = y + verticalItemMargin;
      bottom = top + getDecoratedMeasuredHeight(item) + params.topMargin + params.bottomMargin;
    } else {
      bottom = y - verticalItemMargin;
      top = bottom - getDecoratedMeasuredHeight(item) - params.topMargin - params.bottomMargin;
    }

    layoutDecorated(item, left + params.leftMargin, top + params.topMargin,
        right - params.leftMargin, bottom - params.bottomMargin);

    return item;
  }

  private RowInfo makeRow(RecyclerView.Recycler recycler, RecyclerView.State state, int fromPos,
      int y, boolean fillDown) {
    final int itemCount = state.getItemCount();
    final int increment = fillDown ? 1 : -1;
    int col = fillDown ? 0 : colCount - 1;
    int position = fromPos;
    int endPos = fillDown ? fromPos + colCount : fromPos - colCount;
    endPos = Math.min(endPos, itemCount);
    endPos = Math.max(endPos, -1);
    boolean first = true;

    rowInfo.y = y;
    rowInfo.lastPos = position;

    View[] views = new View[colCount];
    int i = 0;

    while (position != endPos) {
      LayoutRecord rec = layoutRecords.get(position);
      if (rec != null) {
        if (rec.isHeader) {
          if (first) {
            final View header = makeAndAddHeader(recycler, state, position, y, fillDown);
            if (fillDown) {
              rowInfo.y = getDecoratedBottom(header);
            } else {
              rowInfo.y = getDecoratedTop(header);
            }
            rowInfo.lastPos = position;
            return rowInfo;
          } else {
            alignViewTops(views);
            break;
          }
        }
      }

      View child = recycler.getViewForPosition(position);

      if (rec == null) {
        final boolean isHeader = recyclerView.getAdapter().getItemViewType(position) == TYPE_HEADER;
        rec = new LayoutRecord(col, isHeader);
        layoutRecords.put(position, rec);
      } else {
        col = rec.column;
      }

      final boolean isHeader = rec.isHeader;

      if (!first && isHeader) {
        alignViewTops(views);
        break;
      } else if (first && isHeader) {
        final View header = makeAndAddHeader(state, child, y, fillDown);
        if (fillDown) {
          rowInfo.y = getDecoratedBottom(header);
        } else {
          rowInfo.y = getDecoratedTop(header);
        }
        rowInfo.lastPos = position;
        return rowInfo;
      }

      makeAndAddItem(state, child, col, y, fillDown);
      int itemY;
      if (fillDown) {
        itemY = getDecoratedBottom(child);
        if (itemY > rowInfo.y) {
          rowInfo.y = itemY;
        }
      } else {
        itemY = getDecoratedTop(child);
        if (itemY < rowInfo.y) {
          rowInfo.y = itemY;
        }
      }

      views[i++] = child;

      rowInfo.lastPos = position;

      if (first) {
        first = false;
      }

      col += increment;
      position += increment;
    }

    alignViewTops(views);

    return rowInfo;
  }

  private void alignViewTops(View[] views) {
    int topMost = Integer.MAX_VALUE;

    for (View view : views) {
      if (view != null) {
        final int top = getDecoratedTop(view);
        if (top < topMost) {
          topMost = top;
        }
      }
    }

    for (View view : views) {
      if (view != null) {
        final int top = getDecoratedTop(view);
        if (top != topMost) {
          view.offsetTopAndBottom(topMost - top);
        }
      }
    }
  }

  private boolean contentFits() {
    final int childCount = getChildCount();
    if (childCount == 0) {
      return true;
    }

    final View firstChild = getChildAt(0);
    final int firstChildTop = getDecoratedTop(firstChild);
    if (firstChildTop < getPaddingTop()) {
      return false;
    }

    int lowestBottom = Integer.MIN_VALUE;
    for (int i = getChildCount() - 1; i >= getChildCount() - colCount && i > 0; i--) {
      final View child = getChildAt(i);
      final int bottom = getDecoratedBottom(child);
      if (bottom > lowestBottom) {
        lowestBottom = bottom;
      }
    }

    if (firstChildTop >= getPaddingTop() && lowestBottom <= getHeight() - getPaddingBottom()) {
      return true;
    }

    return false;
  }

  @Override public int scrollVerticallyBy(int dy, RecyclerView.Recycler recycler,
      RecyclerView.State state) {
    // TODO: State
    final boolean contentFits = contentFits();
    final int allowOverhang = Math.abs(dy);

    if (!contentFits) {
      int overhang;
      final boolean up;
      if (dy < 0) {
        overhang = fillUp(recycler, state, anchorPosition - 1, allowOverhang);
        up = true;
      } else {
        overhang = fillDown(recycler, state, anchorPosition + getChildCount(), allowOverhang);
        up = false;
      }

      int movedBy = Math.min(overhang, allowOverhang);
      movedBy = up ? movedBy : -movedBy;
      offsetChildrenVertical(movedBy);

      anchorOffset += movedBy;

      recycleOffscreenViews(recycler);

      return -movedBy;
    }

    return dy;
  }

  @Override public boolean canScrollHorizontally() {
    return false;
  }

  @Override public boolean canScrollVertically() {
    return true;
  }

  @Override public void scrollToPosition(int position) {
    anchorPosition = position;
    anchorOffset = 0;
    requestLayout();
  }

  @Override public void smoothScrollToPosition(RecyclerView recyclerView, RecyclerView.State state,
      final int position) {
    LinearSmoothScroller scroller = new LinearSmoothScroller(recyclerView.getContext()) {
      @Override public PointF computeScrollVectorForPosition(int i) {
        if (getChildCount() > 0) {
          final boolean up = position < anchorPosition;
          return new PointF(0, up ? -1 : 0);
        }
        return null;
      }
    };
    scroller.setTargetPosition(position);
    startSmoothScroll(scroller);
  }

  @Override public View onFocusSearchFailed(View focused, int direction,
      RecyclerView.Recycler recycler, RecyclerView.State state) {
    return super.onFocusSearchFailed(focused, direction, recycler, state);
  }

  @Override public View onInterceptFocusSearch(View focused, int direction) {
    return super.onInterceptFocusSearch(focused, direction);
  }

  @Override public void onAdapterChanged(RecyclerView.Adapter oldAdapter,
      RecyclerView.Adapter newAdapter) {
    super.onAdapterChanged(oldAdapter, newAdapter);
    anchorPosition = 0;
    anchorOffset = 0;
    layoutRecords.clear();
  }

  @Override public void onItemsAdded(RecyclerView recyclerView, int positionStart, int itemCount) {
    super.onItemsAdded(recyclerView, positionStart, itemCount);
    Timber.d("[onItemAdded] positionStart: " + positionStart + " - itemCount: " + itemCount);
    onItemsChanged();
  }

  @Override public void onItemsRemoved(RecyclerView recyclerView, int positionStart,
      int itemCount) {
    super.onItemsRemoved(recyclerView, positionStart, itemCount);
    Timber.d("[onItemsRemoved] positionStart: " + positionStart + " - itemCount: " + itemCount);

    onItemsChanged();
  }

  private void onItemsChanged() {
    if (onItemsChanged == null) {
      onItemsChanged = new Runnable() {
        @Override public void run() {
          final int itemCount = getItemCount();

          if (anchorPosition >= itemCount) {
            anchorPosition = Math.max(itemCount - 1, 0);
          }

          final RecyclerView.Adapter adapter = recyclerView.getAdapter();
          if (adapter.hasStableIds() && itemCount > 0) {
            final int position = tryFindIdPosition(anchorId);

            if (position != INVALID_POSITION) {
              anchorPosition = position;
            } else {
              anchorId = adapter.getItemId(anchorPosition);
            }
          }

          buildLayoutRecords();
        }
      };
    }
  }

  private int tryFindIdPosition(long id) {
    final RecyclerView.Adapter adapter = recyclerView.getAdapter();
    final int count = getItemCount();
    if (count == 0) {
      return INVALID_POSITION;
    }

    int first = anchorPosition;
    int last = anchorPosition;
    int toCheck = anchorPosition;

    final long endTime = System.currentTimeMillis() + 100;
    boolean hitFirst = false;
    boolean hitLast = false;
    boolean moveDown = false;

    while (System.currentTimeMillis() < endTime) {
      final long rowId = adapter.getItemId(toCheck);
      if (rowId == id) {
        return toCheck;
      }

      hitFirst = hitFirst || toCheck == 0;
      hitLast = hitLast || toCheck == count - 1;

      if (hitFirst && hitLast) {
        break;
      }

      if (hitFirst || (moveDown && !hitLast)) {
        last++;
        toCheck = last;
        moveDown = false;
      } else if (hitLast || (!moveDown && !hitFirst)) {
        first--;
        toCheck = first;
        moveDown = true;
      }
    }

    return INVALID_POSITION;
  }

  private void buildLayoutRecords() {
    layoutRecords.clear();
    final RecyclerView.Adapter adapter = recyclerView.getAdapter();

    int col = 0;

    for (int position = 0; position <= anchorPosition; position++) {
      final int itemType = adapter.getItemViewType(position);
      if (itemType == TYPE_HEADER) {
        LayoutRecord rec = new LayoutRecord(0, true);
        layoutRecords.put(position, rec);
        col = 0;
      } else {
        if (col >= colCount) {
          col = 0;
        }

        LayoutRecord rec = new LayoutRecord(col, false);
        layoutRecords.put(position, rec);

        col++;
      }
    }

    LayoutRecord rec = layoutRecords.get(anchorPosition);
    if (rec.column > 0) {
      anchorPosition -= rec.column;
    }
  }

  @Override public void onMeasure(RecyclerView.Recycler recycler, RecyclerView.State state,
      int widthSpec, int heightSpec) {
    super.onMeasure(recycler, state, widthSpec, heightSpec);

    colWidth = -1;
  }

  @Override public Parcelable onSaveInstanceState() {
    if (pendingSavedState != null) {
      return new SavedState(pendingSavedState);
    }

    SavedState state = new SavedState();

    if (getChildCount() > 0) {
      state.anchorPosition = anchorPosition;
      state.anchorOffset = anchorOffset;
      state.anchorId = anchorId;
    } else {
      state.anchorPosition = 0;
      state.anchorOffset = 0;
      state.anchorId = 0L;
    }

    return state;
  }

  @Override public void onRestoreInstanceState(Parcelable state) {
    pendingSavedState = (SavedState) state;
    requestLayout();
  }

  static class SavedState implements Parcelable {

    int anchorPosition;

    int anchorOffset;

    long anchorId;

    public SavedState() {
    }

    SavedState(Parcel in) {
      anchorPosition = in.readInt();
      anchorOffset = in.readInt();
      anchorId = in.readLong();
    }

    public SavedState(SavedState other) {
      anchorPosition = other.anchorPosition;
      anchorOffset = other.anchorOffset;
      anchorId = other.anchorId;
    }

    @Override
    public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      dest.writeInt(anchorPosition);
      dest.writeInt(anchorOffset);
      dest.writeLong(anchorId);
    }

    public static final Parcelable.Creator<SavedState> CREATOR =
        new Parcelable.Creator<SavedState>() {
          @Override
          public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
          }

          @Override
          public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }

  @Override public RecyclerView.LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT);
  }

  @Override public boolean checkLayoutParams(RecyclerView.LayoutParams lp) {
    return lp instanceof LayoutParams;
  }

  @Override public RecyclerView.LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
    return new LayoutParams(lp);
  }

  @Override public RecyclerView.LayoutParams generateLayoutParams(Context c, AttributeSet attrs) {
    return new LayoutParams(c, attrs);
  }

  public static class LayoutParams extends RecyclerView.LayoutParams {

    public LayoutParams(int height) {
      super(MATCH_PARENT, height);

      if (this.height == MATCH_PARENT) {
        Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - "
            + "impossible! Falling back to WRAP_CONTENT");
        this.height = WRAP_CONTENT;
      }
    }

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);

      if (this.width != MATCH_PARENT) {
        Log.w(TAG,
            "Inflation setting LayoutParams width to " + this.width + " - must be MATCH_PARENT");
        this.width = MATCH_PARENT;
      }
      if (this.height == MATCH_PARENT) {
        Log.w(TAG, "Inflation setting LayoutParams height to MATCH_PARENT - "
            + "impossible! Falling back to WRAP_CONTENT");
        this.height = WRAP_CONTENT;
      }
    }

    public LayoutParams(ViewGroup.LayoutParams other) {
      super(other);

      if (this.width != MATCH_PARENT) {
        Log.w(TAG,
            "Constructing LayoutParams with width " + this.width + " - must be MATCH_PARENT");
        this.width = MATCH_PARENT;
      }
      if (this.height == MATCH_PARENT) {
        Log.w(TAG, "Constructing LayoutParams with height MATCH_PARENT - "
            + "impossible! Falling back to WRAP_CONTENT");
        this.height = WRAP_CONTENT;
      }
    }
  }
}
