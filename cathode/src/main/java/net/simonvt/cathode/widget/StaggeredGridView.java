/*
 * Copyright (C) 2012 The Android Open Source Project
 * Copyright (C) 2014 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.database.DataSetObserver;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.util.SparseArrayCompat;
import android.util.AttributeSet;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.SparseArray;
import android.util.StateSet;
import android.view.MotionEvent;
import android.view.SoundEffectConstants;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.accessibility.AccessibilityEvent;
import android.widget.EdgeEffect;
import android.widget.ListAdapter;
import android.widget.Scroller;
import java.util.ArrayList;
import java.util.Arrays;
import net.simonvt.cathode.R;

public class StaggeredGridView extends ViewGroup {
  private static final String TAG = "StaggeredGridView";
  private static final boolean DEBUG = false;

    /*
     * There are a few things you should know if you're going to make modifications
     * to StaggeredGridView.
     *
     * Like ListView, SGV populates from an adapter and recycles views that fall out
     * of the visible boundaries of the grid. A few invariants always hold:
     *
     * - firstPosition is the adapter position of the View returned by getChildAt(0).
     * - Any child index can be translated to an adapter position by adding firstPosition.
     * - Any adapter position can be translated to a child index by subtracting firstPosition.
     * - Views for items in the range [firstPosition, firstPosition + getChildCount()) are
     *   currently attached to the grid as children. All other adapter positions do not have
     *   active views.
     *
     * This means a few things thanks to the staggered grid's nature. Some views may stay attached
     * long after they have scrolled offscreen if removing and recycling them would result in
     * breaking one of the invariants above.
     *
     * LayoutRecords are used to track data about a particular item's layout after the associated
     * view has been removed. These let positioning and the choice of column for an item
     * remain consistent even though the rules for filling content up vs. filling down vary.
     *
     * Whenever layout parameters for a known LayoutRecord change, other LayoutRecords before
     * or after it may need to be invalidated. e.g. if the item's height or the number
     * of columns it spans changes, all bets for other items in the same direction are off
     * since the cached information no longer applies.
     */

  public interface OnItemClickListener {

    void onItemClick(StaggeredGridView parent, View view, int position, long id);
  }

  private ListAdapter adapter;

  public static final int COLUMN_COUNT_AUTO = -1;

  public static final int INVALID_POSITION = -1;

  private int colCountSetting = 2;
  private int colCount = 2;
  private int minColWidth = 0;
  private int itemMargin;

  private int[] itemTops;
  private int[] itemBottoms;

  private boolean fastChildLayout;
  private boolean populating;
  private boolean forcePopulateOnLayout;
  private boolean inLayout;
  private int restoreOffset;

  private final RecycleBin recycler = new RecycleBin();

  private final AdapterDataSetObserver observer = new AdapterDataSetObserver();

  private boolean dataChanged;
  private int oldItemCount;
  private int itemCount;
  private boolean hasStableIds;

  private int firstPosition;

  private int touchSlop;
  private int maximumVelocity;
  private int flingVelocity;
  private float lastTouchY;
  private float lastTouchX;
  private float touchRemainderY;
  private int activePointerId;

  private static final int TOUCH_MODE_IDLE = 0;
  private static final int TOUCH_MODE_DRAGGING = 1;
  private static final int TOUCH_MODE_FLINGING = 2;

  private int touchMode;
  private final VelocityTracker velocityTracker = VelocityTracker.obtain();
  private final Scroller scroller;

  private final EdgeEffect topEdge;
  private final EdgeEffect bottomEdge;

  private View emptyView;

  private OnItemClickListener onItemClickListener;

  private Drawable selector;

  private Rect selectorRect = new Rect();

  private int motionPosition;

  private long motionId;

  private Runnable pendingTapCheck;

  private Runnable tapReset;

  private class TapCheck implements Runnable {

    @Override public void run() {
      if (touchMode == TOUCH_MODE_IDLE) {
        final View child = getChildAt(motionPosition - firstPosition);
        child.getPaddingTop();
        Rect padding = new Rect();
        selector.getPadding(padding);
        selectorRect.set(child.getLeft() - padding.left, child.getTop() - padding.top,
            child.getRight() + padding.right, child.getBottom() + padding.bottom);
        setPressed(true);
        child.setPressed(true);
        selector.setState(getDrawableState());
        invalidate();
      }

      pendingTapCheck = null;
    }
  }

  private class TapReset implements Runnable {

    @Override public void run() {
      tapReset = null;
      final View child = getChildAt(motionPosition - firstPosition);
      child.setPressed(false);
      setPressed(false);
      selectorRect.setEmpty();
      if (adapter != null) {
        final long id = adapter.getItemId(motionPosition);
        if (!dataChanged) {
          performClick(child, motionPosition, id);
        }
      }
      invalidate();
      motionPosition = INVALID_POSITION;
      motionId = -1L;
    }
  }

  private void performClick(View view, int position, long id) {
    if (onItemClickListener != null) {
      playSoundEffect(SoundEffectConstants.CLICK);
      if (view != null) {
        view.sendAccessibilityEvent(AccessibilityEvent.TYPE_VIEW_CLICKED);
      }
      onItemClickListener.onItemClick(this, view, position, id);
    }
  }

  private static final class LayoutRecord {
    public int column;
    public long id = -1;
    public int height;
    public int span;
    private int[] margins;

    private void ensureMargins() {
      if (margins == null) {
        // Don't need to confirm length;
        // all layoutrecords are purged when column count changes.
        margins = new int[span * 2];
      }
    }

    public int getMarginAbove(int col) {
      if (margins == null) {
        return 0;
      }
      return margins[col * 2];
    }

    public int getMarginBelow(int col) {
      if (margins == null) {
        return 0;
      }
      return margins[col * 2 + 1];
    }

    public void setMarginAbove(int col, int margin) {
      if (margins == null && margin == 0) {
        return;
      }
      ensureMargins();
      margins[col * 2] = margin;
    }

    public void setMarginBelow(int col, int margin) {
      if (margins == null && margin == 0) {
        return;
      }
      ensureMargins();
      margins[col * 2 + 1] = margin;
    }

    @Override
    public String toString() {
      String result = "LayoutRecord{c=" + column + ", id=" + id + " h=" + height + " s=" + span;
      if (margins != null) {
        result += " margins[above, below](";
        for (int i = 0; i < margins.length; i += 2) {
          result += "[" + margins[i] + ", " + margins[i + 1] + "]";
        }
        result += ")";
      }
      return result + "}";
    }
  }

  private final SparseArrayCompat<LayoutRecord> layoutRecords =
      new SparseArrayCompat<LayoutRecord>();

  public StaggeredGridView(Context context) {
    this(context, null);
  }

  public StaggeredGridView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public StaggeredGridView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);

    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.StaggeredGridView, defStyle, 0);

    colCount = a.getInt(R.styleable.StaggeredGridView_android_numColumns, 1);
    itemMargin = a.getDimensionPixelSize(R.styleable.StaggeredGridView_itemMargin, 0);
    selector = a.getDrawable(R.styleable.StaggeredGridView_android_listChoiceBackgroundIndicator);
    if (selector != null) {
      selector.setCallback(this);
    }

    a.recycle();

    final ViewConfiguration vc = ViewConfiguration.get(context);
    touchSlop = vc.getScaledTouchSlop();
    maximumVelocity = vc.getScaledMaximumFlingVelocity();
    flingVelocity = vc.getScaledMinimumFlingVelocity();
    scroller = new Scroller(context);

    topEdge = new EdgeEffect(context);
    bottomEdge = new EdgeEffect(context);
    setWillNotDraw(false);
    setClipToPadding(false);
    setVerticalScrollBarEnabled(true);
  }

  /**
   * Set a fixed number of columns for this grid. Space will be divided evenly
   * among all columns, respecting the item margin between columns.
   * The default is 2. (If it were 1, perhaps you should be using a
   * {@link android.widget.ListView ListView}.)
   *
   * @param colCount Number of columns to display.
   * @see #setMinColumnWidth(int)
   */
  public void setColumnCount(int colCount) {
    if (colCount < 1 && colCount != COLUMN_COUNT_AUTO) {
      throw new IllegalArgumentException("Column count must be at least 1 - received " + colCount);
    }
    final boolean needsPopulate = colCount != this.colCount;
    this.colCount = colCountSetting = colCount;
    if (needsPopulate) {
      populate();
    }
  }

  public int getColumnCount() {
    return colCount;
  }

  /**
   * Set a minimum column width.
   */
  public void setMinColumnWidth(int minColWidth) {
    this.minColWidth = minColWidth;
    setColumnCount(COLUMN_COUNT_AUTO);
  }

  /**
   * Set the margin between items in pixels. This margin is applied
   * both vertically and horizontally.
   *
   * @param marginPixels Spacing between items in pixels
   */
  public void setItemMargin(int marginPixels) {
    final boolean needsPopulate = marginPixels != itemMargin;
    itemMargin = marginPixels;
    if (needsPopulate) {
      populate();
    }
  }

  /**
   * Return the first adapter position with a view currently attached as
   * a child view of this grid.
   *
   * @return the adapter position represented by the view at getChildAt(0).
   */
  public int getFirstPosition() {
    return firstPosition;
  }

  public void setOnItemClickListener(OnItemClickListener listener) {
    onItemClickListener = listener;
  }

  public int getPositionForView(View view) {
    View listItem = view;
    try {
      View v;
      while (!(v = (View) listItem.getParent()).equals(this)) {
        listItem = v;
      }
    } catch (ClassCastException e) {
      // We made it up to the window without find this list view
      return INVALID_POSITION;
    }

    // Search the children for the list item
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      if (getChildAt(i).equals(listItem)) {
        return firstPosition + i;
      }
    }

    // Child not found!
    return INVALID_POSITION;
  }

  private int getPositionAt(int x, int y) {
    Rect frame = new Rect();
    final int count = getChildCount();
    for (int i = count - 1; i >= 0; i--) {
      final View child = getChildAt(i);
      if (child.getVisibility() == View.VISIBLE) {
        child.getHitRect(frame);
        if (frame.contains(x, y)) {
          return firstPosition + i;
        }
      }
    }

    return INVALID_POSITION;
  }

  @Override protected void dispatchSetPressed(boolean pressed) {
    // Don't dispatch pressed state to child views
  }

  @Override
  protected int computeVerticalScrollExtent() {
    int extent = 0;

    if (colCount == 1 && getChildCount() > 0) {
      // ListView behavior
      final int count = getChildCount();
      extent = count * 100;

      View view = getChildAt(0);
      final int top = view.getTop();
      int height = view.getHeight();
      if (height > 0) {
        extent += (top * 100) / height;
      }

      view = getChildAt(count - 1);
      final int bottom = view.getBottom();
      height = view.getHeight();
      if (height > 0) {
        extent -= ((bottom - getHeight()) * 100) / height;
      }
    } else {
      // GridView behavior
    }

    return extent;
  }

  @Override
  protected int computeVerticalScrollOffset() {
    if (getChildCount() == 0) {
      return 0;
    }

    int offset = 0;

    if (colCount == 1) {
      // ListView behavior
      final View view = getChildAt(0);
      final int top = view.getTop();
      int height = view.getHeight();
      if (height > 0) {
        offset = Math.max(firstPosition * 100 - (top * 100) / height, 0);
      }
    } else {
      // GridView behavior
    }

    return offset;
  }

  @Override
  protected int computeVerticalScrollRange() {
    int range = 0;

    if (colCount == 1) {
      // ListView behavior
      range = Math.max(itemCount * 100, 0);
    } else {
      // GridView behavior
    }

    return range;
  }

  @Override
  public boolean onInterceptTouchEvent(MotionEvent ev) {
    velocityTracker.addMovement(ev);
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;
    switch (action) {
      case MotionEvent.ACTION_DOWN:
        velocityTracker.clear();
        scroller.abortAnimation();
        lastTouchY = ev.getY();
        lastTouchX = ev.getX();
        activePointerId = ev.getPointerId(0);
        touchRemainderY = 0;
        if (touchMode == TOUCH_MODE_FLINGING) {
          // Catch!
          touchMode = TOUCH_MODE_DRAGGING;
          return true;
        }
        break;

      case MotionEvent.ACTION_MOVE: {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
          Log.e(TAG, "onInterceptTouchEvent could not find pointer with id "
              + activePointerId
              + " - did StaggeredGridView receive an inconsistent "
              + "event stream?");
          return false;
        }
        final float y = ev.getY(index);
        final float dy = y - lastTouchY + touchRemainderY;
        final int deltaY = (int) dy;
        touchRemainderY = dy - deltaY;

        if (Math.abs(dy) > touchSlop) {
          touchMode = TOUCH_MODE_DRAGGING;
          return true;
        }
      }
    }

    return false;
  }

  @Override
  public boolean onTouchEvent(MotionEvent ev) {
    velocityTracker.addMovement(ev);
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;
    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        velocityTracker.clear();
        scroller.abortAnimation();
        lastTouchY = ev.getY();
        lastTouchX = ev.getX();
        final int x = (int) ev.getX();
        activePointerId = ev.getPointerId(0);
        touchRemainderY = 0;
        motionPosition = getPositionAt(x, (int) lastTouchY);
        if (hasStableIds) {
          motionId = ((LayoutParams) getChildAt(motionPosition).getLayoutParams()).id;
        }
        if (motionPosition != INVALID_POSITION && adapter != null && adapter.isEnabled(
            motionPosition)) {
          pendingTapCheck = new TapCheck();
          postDelayed(pendingTapCheck, ViewConfiguration.getTapTimeout());
        }
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        final int index = ev.findPointerIndex(activePointerId);
        if (index < 0) {
          Log.e(TAG, "onInterceptTouchEvent could not find pointer with id "
              + activePointerId
              + " - did StaggeredGridView receive an inconsistent "
              + "event stream?");
          return false;
        }
        final float y = ev.getY(index);
        final float x = ev.getX(index);
        final float dy = y - lastTouchY + touchRemainderY;
        final int deltaY = (int) dy;
        touchRemainderY = dy - deltaY;

        if (Math.abs(dy) > touchSlop) {
          touchMode = TOUCH_MODE_DRAGGING;
        }

        if (touchMode == TOUCH_MODE_DRAGGING) {
          if (pendingTapCheck != null) {
            removeCallbacks(pendingTapCheck);
          }
          if (!selectorRect.isEmpty()) {
            selectorRect.setEmpty();
          }
          if (motionPosition != INVALID_POSITION) {
            final View child = getChildAt(motionPosition - firstPosition);
            if (child != null) {
              child.setPressed(false);
            }
            setPressed(false);
            selector.setState(StateSet.NOTHING);
            motionPosition = INVALID_POSITION;
            motionId = -1L;
          }

          lastTouchY = y;
          lastTouchX = x;

          if (!trackMotionScroll(deltaY, true)) {
            // Break fling velocity if we impacted an edge.
            velocityTracker.clear();
          }
        }
      }
      break;

      case MotionEvent.ACTION_CANCEL:
        touchMode = TOUCH_MODE_IDLE;

        if (motionPosition != INVALID_POSITION) {
          View child = getChildAt(motionPosition);
          child.setPressed(false);

          setPressed(false);
        }

        motionPosition = INVALID_POSITION;
        motionId = -1L;

        if (pendingTapCheck != null) {
          removeCallbacks(pendingTapCheck);
          pendingTapCheck = null;
        }
        break;

      case MotionEvent.ACTION_UP: {
        velocityTracker.computeCurrentVelocity(1000, maximumVelocity);
        final float velocity = velocityTracker.getYVelocity(activePointerId);

        if (pendingTapCheck != null) {
          removeCallbacks(pendingTapCheck);
          pendingTapCheck = null;
        }

        if (Math.abs(velocity) > flingVelocity) { // TODO
          touchMode = TOUCH_MODE_FLINGING;
          scroller.fling(0, 0, 0, (int) velocity, 0, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
          lastTouchY = 0;
          postInvalidateOnAnimation();
        } else {
          if (touchMode != TOUCH_MODE_DRAGGING && motionPosition != INVALID_POSITION) {
            if (adapter != null && adapter.isEnabled(motionPosition)) {
              new TapCheck().run();
              tapReset = new TapReset();
              postDelayed(tapReset, ViewConfiguration.getPressedStateDuration());
            } else {
              motionPosition = INVALID_POSITION;
              motionId = -1L;
            }
          }
          touchMode = TOUCH_MODE_IDLE;
        }
      }
      break;
    }
    return true;
  }

  /**
   * @param deltaY Pixels that content should move by
   * @return true if the movement completed, false if it was stopped prematurely.
   */
  private boolean trackMotionScroll(int deltaY, boolean allowOverScroll) {
    final boolean contentFits = contentFits();
    final int allowOverhang = Math.abs(deltaY);

    final int overScrolledBy;
    final int movedBy;
    if (!contentFits) {
      final int overhang;
      final boolean up;
      populating = true;
      if (deltaY > 0) {
        overhang = fillUp(firstPosition - 1, allowOverhang);
        up = true;
      } else {
        overhang = fillDown(firstPosition + getChildCount(), allowOverhang) + itemMargin;
        up = false;
      }
      movedBy = Math.min(overhang, allowOverhang);
      offsetChildren(up ? movedBy : -movedBy);
      recycleOffscreenViews();
      populating = false;
      overScrolledBy = allowOverhang - overhang;
    } else {
      overScrolledBy = allowOverhang;
      movedBy = 0;
    }

    if (allowOverScroll) {
      final int overScrollMode = getOverScrollMode();

      if (overScrollMode == OVER_SCROLL_ALWAYS || (overScrollMode == OVER_SCROLL_IF_CONTENT_SCROLLS
          && !contentFits)) {

        if (overScrolledBy > 0) {
          EdgeEffect edge = deltaY > 0 ? topEdge : bottomEdge;
          edge.onPull((float) Math.abs(deltaY) / getHeight());
          postInvalidateOnAnimation();
        }
      }
    }

    if (!awakenScrollBars()) {
      invalidate();
    }

    correctTooLow();

    return deltaY == 0 || movedBy != 0;
  }

  private boolean contentFits() {
    if (firstPosition != 0 || getChildCount() != itemCount) {
      return false;
    }

    int topmost = Integer.MAX_VALUE;
    int bottommost = Integer.MIN_VALUE;
    for (int i = 0; i < colCount; i++) {
      if (itemTops[i] < topmost) {
        topmost = itemTops[i];
      }
      if (itemBottoms[i] > bottommost) {
        bottommost = itemBottoms[i];
      }
    }

    return topmost >= getPaddingTop() && bottommost <= getHeight() - getPaddingBottom();
  }

  private void recycleAllViews() {
    for (int i = 0; i < getChildCount(); i++) {
      recycler.addScrap(getChildAt(i));
    }

    if (inLayout) {
      removeAllViewsInLayout();
    } else {
      removeAllViews();
    }
  }

  /**
   * Important: this method will leave offscreen views attached if they
   * are required to maintain the invariant that child view with index i
   * is always the view corresponding to position firstPosition + i.
   */
  private void recycleOffscreenViews() {
    final int height = getHeight();
    final int clearAbove = -itemMargin;
    final int clearBelow = height + itemMargin;
    for (int i = getChildCount() - 1; i >= 0; i--) {
      final View child = getChildAt(i);
      if (child.getTop() <= clearBelow) {
        // There may be other offscreen views, but we need to maintain
        // the invariant documented above.
        break;
      }

      if (inLayout) {
        removeViewsInLayout(i, 1);
      } else {
        removeViewAt(i);
      }

      recycler.addScrap(child);
    }

    while (getChildCount() > 0) {
      final View child = getChildAt(0);
      if (child.getBottom() >= clearAbove) {
        // There may be other offscreen views, but we need to maintain
        // the invariant documented above.
        break;
      }

      if (inLayout) {
        removeViewsInLayout(0, 1);
      } else {
        removeViewAt(0);
      }

      recycler.addScrap(child);
      firstPosition++;
    }

    final int childCount = getChildCount();
    if (childCount > 0) {
      // Repair the top and bottom column boundaries from the views we still have
      Arrays.fill(itemTops, Integer.MAX_VALUE);
      Arrays.fill(itemBottoms, Integer.MIN_VALUE);

      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int top = child.getTop() - itemMargin;
        final int bottom = child.getBottom();
        final LayoutRecord rec = layoutRecords.get(firstPosition + i);

        final int colEnd = lp.column + Math.min(colCount, lp.span);
        for (int col = lp.column; col < colEnd; col++) {
          final int colTop = top - rec.getMarginAbove(col - lp.column);
          final int colBottom = bottom + rec.getMarginBelow(col - lp.column);
          if (colTop < itemTops[col]) {
            itemTops[col] = colTop;
          }
          if (colBottom > itemBottoms[col]) {
            itemBottoms[col] = colBottom;
          }
        }
      }

      for (int col = 0; col < colCount; col++) {
        if (itemTops[col] == Integer.MAX_VALUE) {
          // If one was untouched, both were.
          itemTops[col] = 0;
          itemBottoms[col] = 0;
        }
      }
    }
  }

  public void computeScroll() {
    if (scroller.computeScrollOffset()) {
      final int y = scroller.getCurrY();
      final int dy = (int) (y - lastTouchY);
      lastTouchY = y;
      final boolean stopped = !trackMotionScroll(dy, false);

      if (!stopped && !scroller.isFinished()) {
        postInvalidateOnAnimation();
      } else {
        if (stopped) {
          final int overScrollMode = getOverScrollMode();
          if (overScrollMode != OVER_SCROLL_NEVER) {
            final EdgeEffect edge;
            if (dy > 0) {
              edge = topEdge;
            } else {
              edge = bottomEdge;
            }
            edge.onAbsorb(Math.abs((int) scroller.getCurrVelocity()));
            postInvalidateOnAnimation();
          }
          scroller.abortAnimation();
        }
        touchMode = TOUCH_MODE_IDLE;
      }
    }
  }

  @Override
  public void draw(Canvas canvas) {
    super.draw(canvas);

    if (topEdge != null) {
      boolean needsInvalidate = false;
      if (!topEdge.isFinished()) {
        topEdge.draw(canvas);
        needsInvalidate = true;
      }
      if (!bottomEdge.isFinished()) {
        final int restoreCount = canvas.save();
        final int width = getWidth();
        canvas.translate(-width, getHeight());
        canvas.rotate(180, width, 0);
        bottomEdge.draw(canvas);
        canvas.restoreToCount(restoreCount);
        needsInvalidate = true;
      }

      if (needsInvalidate) {
        postInvalidateOnAnimation();
      }
    }

    if (!selectorRect.isEmpty()) {
      selector.setBounds(selectorRect);
      selector.draw(canvas);
    }
  }

  public void beginFastChildLayout() {
    fastChildLayout = true;
  }

  public void endFastChildLayout() {
    fastChildLayout = false;
    populate();
  }

  @Override
  public void requestLayout() {
    if (!populating && !fastChildLayout) {
      super.requestLayout();
    }
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    if (widthMode != MeasureSpec.EXACTLY) {
      Log.e(TAG, "onMeasure: must have an exact width or match_parent! "
          + "Using fallback spec of EXACTLY "
          + widthSize);
      widthMode = MeasureSpec.EXACTLY;
    }
    if (heightMode != MeasureSpec.EXACTLY) {
      Log.e(TAG, "onMeasure: must have an exact height or match_parent! "
          + "Using fallback spec of EXACTLY "
          + heightSize);
      heightMode = MeasureSpec.EXACTLY;
    }

    setMeasuredDimension(widthSize, heightSize);

    if (colCountSetting == COLUMN_COUNT_AUTO) {
      final int colCount = widthSize / minColWidth;
      if (colCount != this.colCount) {
        this.colCount = colCount;
        forcePopulateOnLayout = true;
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    inLayout = true;
    populate();
    inLayout = false;
    forcePopulateOnLayout = false;

    final int width = r - l;
    final int height = b - t;
    topEdge.setSize(width, height);
    bottomEdge.setSize(width, height);
  }

  private void populate() {
    if (getWidth() == 0 || getHeight() == 0) {
      return;
    }

    if (colCount == COLUMN_COUNT_AUTO) {
      final int colCount = getWidth() / minColWidth;
      if (colCount != this.colCount) {
        this.colCount = colCount;
      }
    }

    final int colCount = this.colCount;
    if (itemTops == null || itemTops.length != colCount) {
      itemTops = new int[colCount];
      itemBottoms = new int[colCount];
      final int top = getPaddingTop();
      final int offset = top + Math.min(restoreOffset, 0);
      Arrays.fill(itemTops, offset);
      Arrays.fill(itemBottoms, offset);
      layoutRecords.clear();
      if (inLayout) {
        removeAllViewsInLayout();
      } else {
        removeAllViews();
      }

      restoreOffset = 0;
    }

    populating = true;
    layoutChildren(dataChanged);
    fillDown(firstPosition + getChildCount(), 0);
    adjustViewsDown();
    fillUp(firstPosition - 1, 0);
    correctTooLow();
    populating = false;

    if (dataChanged && hasStableIds && motionPosition != INVALID_POSITION) {
      // Match up motion position
      View motionTarget = null;

      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        if (lp.id == motionId) {
          motionPosition = lp.position;
          motionTarget = child;
        }
        child.setPressed(false);
      }

      if (motionTarget != null) {
        // Abort click if view has moved outside pointer
        if (motionTarget.getTop() <= lastTouchY
            && motionTarget.getBottom() >= lastTouchY
            && motionTarget.getLeft() <= lastTouchX
            && motionTarget.getRight() >= lastTouchX) {
          Rect padding = new Rect();
          selector.getPadding(padding);
          selectorRect.set(motionTarget.getLeft() - padding.left,
              motionTarget.getTop() - padding.top, motionTarget.getRight() + padding.right,
              motionTarget.getBottom() + padding.bottom);
          setPressed(true);
          motionTarget.setPressed(true);
          selector.setState(getDrawableState());
          invalidate();
        } else {
          motionTarget.setPressed(false);
          setPressed(false);
          selectorRect.setEmpty();
          invalidate();
          motionPosition = INVALID_POSITION;
          motionId = -1L;
        }
      } else {
        motionPosition = INVALID_POSITION;
        motionId = -1L;
        setPressed(false);
        selectorRect.setEmpty();
        invalidate();
      }
    }

    dataChanged = false;
  }

  private void correctTooLow() {
    final int childCount = getChildCount();
    if (childCount > 0) {
      if (colCount == 1) {
        if (firstPosition == 0) {
          final View firstChild = getChildAt(0);
          final int firstChildTop = firstChild.getTop();
          if (firstChildTop > getPaddingTop() + itemMargin) {
            offsetChildren(-firstChildTop + getPaddingTop() + itemMargin);
            fillDown(firstPosition + getChildCount(), 0);
          }
        }
      } else {
        int[] bottoms = new int[colCount];
        for (int i = 0; i < colCount; i++) {
          bottoms[i] = Integer.MIN_VALUE;
        }

        for (int i = 0; i < childCount; i++) {
          final View child = getChildAt(i);
          LayoutRecord record = layoutRecords.get(firstPosition + i);
          final int childTop = child.getTop();
          final int childBottom = child.getBottom();
          int columnBottom = Integer.MIN_VALUE;
          for (int j = record.column; j < record.column + record.span; j++) {
            if (bottoms[j] > columnBottom) {
              columnBottom = bottoms[j];
            }
          }

          if (columnBottom == Integer.MIN_VALUE) {
            columnBottom = getPaddingTop() + itemMargin;
          } else {
            columnBottom += itemMargin;
          }

          if (childTop > columnBottom && childTop > 0) {
            final int delta = -(childTop - Math.max(columnBottom, itemMargin + getPaddingTop()));
            child.offsetTopAndBottom(delta);
            for (int j = record.column; j < record.column + record.span; j++) {
              if (childTop - itemMargin == itemTops[j]) {
                itemTops[j] = child.getTop();
              }
              if (childBottom == itemBottoms[j]) {
                itemBottoms[j] = child.getBottom();
              }
            }
          }

          for (int j = record.column; j < record.column + record.span; j++) {
            bottoms[j] = child.getBottom();
          }
        }
      }
    }
  }

  private void adjustViewsDown() {
    final int childCount = getChildCount();
    if (childCount > 0) {
      if (colCount == 1) {
        View lastChild = getChildAt(childCount - 1);
        final int delta = (getHeight() - getPaddingBottom() - itemMargin) - lastChild.getBottom();
        if (delta > 0) {
          offsetChildren(delta);
        }
      } else {
        int lowestBottom = 0;
        for (int i = childCount - 1; i >= 0; i--) {
          final View child = getChildAt(i);
          final int childBottom = child.getBottom();
          if (childBottom > lowestBottom) {
            lowestBottom = childBottom;
          }
        }

        final int delta = (getHeight() - getPaddingBottom() - itemMargin) - lowestBottom;
        if (delta > 0) {
          offsetChildren(delta);
        }
      }
    }
  }

  private void dumpItemPositions() {
    final int childCount = getChildCount();
    Log.d(TAG, "dumpItemPositions:");
    Log.d(TAG, " => Tops:");
    for (int i = 0; i < colCount; i++) {
      Log.d(TAG, "  => " + itemTops[i]);
      boolean found = false;
      for (int j = 0; j < childCount; j++) {
        final View child = getChildAt(j);
        if (itemTops[i] == child.getTop() - itemMargin) {
          found = true;
        }
      }
      if (!found) {
        Log.d(TAG, "!!! No top item found for column " + i + " value " + itemTops[i]);
      }
    }
    Log.d(TAG, " => Bottoms:");
    for (int i = 0; i < colCount; i++) {
      Log.d(TAG, "  => " + itemBottoms[i]);
      boolean found = false;
      for (int j = 0; j < childCount; j++) {
        final View child = getChildAt(j);
        if (itemBottoms[i] == child.getBottom()) {
          found = true;
        }
      }
      if (!found) {
        Log.d(TAG, "!!! No bottom item found for column " + i + " value " + itemBottoms[i]);
      }
    }
  }

  final void offsetChildren(int offset) {
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      child.layout(child.getLeft(), child.getTop() + offset, child.getRight(),
          child.getBottom() + offset);
    }

    final int colCount = this.colCount;
    for (int i = 0; i < colCount; i++) {
      itemTops[i] += offset;
      itemBottoms[i] += offset;
    }
  }

  /**
   * Measure and layout all currently visible children.
   *
   * @param dataChanged Whether the data has changed
   */
  final void layoutChildren(boolean dataChanged) {
    final int paddingLeft = getPaddingLeft();
    final int paddingRight = getPaddingRight();
    final int itemMargin = this.itemMargin;
    final int colWidth =
        (getWidth() - paddingLeft - paddingRight - itemMargin * (colCount - 1)) / colCount;
    int rebuildLayoutRecordsBefore = -1;
    int rebuildLayoutRecordsAfter = -1;

    Arrays.fill(itemBottoms, Integer.MIN_VALUE);

    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      View child = getChildAt(i);
      LayoutParams lp = (LayoutParams) child.getLayoutParams();
      final int col = lp.column;
      final int position = firstPosition + i;
      final boolean needsLayout = dataChanged || child.isLayoutRequested();

      if (dataChanged) {
        View newView = obtainView(position, child);
        if (newView != child) {
          removeViewAt(i);
          addView(newView, i);
          child = newView;
        }
        lp = (LayoutParams) child.getLayoutParams(); // Might have changed
      }

      final int span = Math.min(colCount, lp.span);
      final int widthSize = colWidth * span + itemMargin * (span - 1);

      if (needsLayout) {
        final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

        final int heightSpec;
        if (lp.height == LayoutParams.WRAP_CONTENT) {
          heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
        } else {
          heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
        }

        child.measure(widthSpec, heightSpec);
      }

      int childTop = itemBottoms[col] > Integer.MIN_VALUE ? itemBottoms[col] + this.itemMargin
          : child.getTop();
      if (span > 1) {
        int lowest = childTop;
        for (int j = col + 1; j < col + span; j++) {
          final int bottom = itemBottoms[j] + this.itemMargin;
          if (bottom > lowest) {
            lowest = bottom;
          }
        }
        childTop = lowest;
      }
      final int childHeight = child.getMeasuredHeight();
      final int childBottom = childTop + childHeight;
      final int childLeft = paddingLeft + col * (colWidth + itemMargin);
      final int childRight = childLeft + child.getMeasuredWidth();
      child.layout(childLeft, childTop, childRight, childBottom);

      for (int j = col; j < col + span; j++) {
        itemBottoms[j] = childBottom;
      }

      final LayoutRecord rec = layoutRecords.get(position);
      if (rec != null && rec.height != childHeight) {
        // Invalidate our layout records for everything before this.
        rec.height = childHeight;
        rebuildLayoutRecordsBefore = position;
      }

      if (rec != null && rec.span != span) {
        // Invalidate our layout records for everything after this.
        rec.span = span;
        rebuildLayoutRecordsAfter = position;
      }
    }

    // Update itemBottoms for any empty columns
    for (int i = 0; i < colCount; i++) {
      if (itemBottoms[i] == Integer.MIN_VALUE) {
        itemBottoms[i] = itemTops[i];
      }
    }

    if (rebuildLayoutRecordsBefore >= 0 || rebuildLayoutRecordsAfter >= 0) {
      if (rebuildLayoutRecordsBefore >= 0) {
        invalidateLayoutRecordsBeforePosition(rebuildLayoutRecordsBefore);
      }
      if (rebuildLayoutRecordsAfter >= 0) {
        invalidateLayoutRecordsAfterPosition(rebuildLayoutRecordsAfter);
      }
      for (int i = 0; i < childCount; i++) {
        final int position = firstPosition + i;
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        LayoutRecord rec = layoutRecords.get(position);
        if (rec == null) {
          rec = new LayoutRecord();
          layoutRecords.put(position, rec);
        }
        rec.column = lp.column;
        rec.height = child.getHeight();
        rec.id = lp.id;
        rec.span = Math.min(colCount, lp.span);
      }
    }
  }

  final void invalidateLayoutRecordsBeforePosition(int position) {
    int endAt = 0;
    while (endAt < layoutRecords.size() && layoutRecords.keyAt(endAt) < position) {
      endAt++;
    }
    layoutRecords.removeAtRange(0, endAt);
  }

  final void invalidateLayoutRecordsAfterPosition(int position) {
    int beginAt = layoutRecords.size() - 1;
    while (beginAt >= 0 && layoutRecords.keyAt(beginAt) > position) {
      beginAt--;
    }
    beginAt++;
    layoutRecords.removeAtRange(beginAt + 1, layoutRecords.size() - beginAt);
  }

  /**
   * Should be called with populating set to true.
   *
   * @param fromPosition Position to start filling from
   * @param overhang the number of extra pixels to fill beyond the current top edge
   * @return the max overhang beyond the beginning of the view of any added items at the top
   */
  final int fillUp(int fromPosition, int overhang) {
    final int paddingLeft = getPaddingLeft();
    final int paddingRight = getPaddingRight();
    final int itemMargin = this.itemMargin;
    final int colWidth =
        (getWidth() - paddingLeft - paddingRight - itemMargin * (colCount - 1)) / colCount;
    final int gridTop = getPaddingTop();
    final int fillTo = gridTop - overhang;
    int nextCol = getNextColumnUp();
    int position = fromPosition;

    while (nextCol >= 0 && itemTops[nextCol] > fillTo && position >= 0) {
      final View child = obtainView(position, null);
      LayoutParams lp = (LayoutParams) child.getLayoutParams();

      if (child.getParent() != this) {
        if (inLayout) {
          addViewInLayout(child, 0, lp);
        } else {
          addView(child, 0);
        }
      }

      final int span = Math.min(colCount, lp.span);
      final int widthSize = colWidth * span + itemMargin * (span - 1);
      final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

      LayoutRecord rec;
      if (span > 1) {
        rec = getNextRecordUp(position, span);
        nextCol = rec.column;
      } else {
        rec = layoutRecords.get(position);
      }

      boolean invalidateBefore = false;
      if (rec == null) {
        rec = new LayoutRecord();
        layoutRecords.put(position, rec);
        rec.column = nextCol;
        rec.span = span;
      } else if (span != rec.span) {
        rec.span = span;
        rec.column = nextCol;
        invalidateBefore = true;
      } else {
        nextCol = rec.column;
      }

      if (hasStableIds) {
        final long id = adapter.getItemId(position);
        rec.id = id;
        lp.id = id;
      }

      lp.column = nextCol;

      final int heightSpec;
      if (lp.height == LayoutParams.WRAP_CONTENT) {
        heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
      } else {
        heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
      }
      child.measure(widthSpec, heightSpec);

      final int childHeight = child.getMeasuredHeight();
      if (invalidateBefore || (childHeight != rec.height && rec.height > 0)) {
        invalidateLayoutRecordsBeforePosition(position);
      }
      rec.height = childHeight;

      final int startFrom;
      if (span > 1) {
        int highest = itemTops[nextCol];
        for (int i = nextCol + 1; i < nextCol + span; i++) {
          final int top = itemTops[i];
          if (top < highest) {
            highest = top;
          }
        }
        startFrom = highest;
      } else {
        startFrom = itemTops[nextCol];
      }
      final int childBottom = startFrom;
      final int childTop = childBottom - childHeight;
      final int childLeft = paddingLeft + nextCol * (colWidth + itemMargin);
      final int childRight = childLeft + child.getMeasuredWidth();
      child.layout(childLeft, childTop, childRight, childBottom);

      for (int i = nextCol; i < nextCol + span; i++) {
        itemTops[i] = childTop - rec.getMarginAbove(i - nextCol) - itemMargin;
      }

      nextCol = getNextColumnUp();
      firstPosition = position--;
    }

    int highestView = getHeight();
    for (int i = 0; i < colCount; i++) {
      if (itemTops[i] < highestView) {
        highestView = itemTops[i];
      }
    }
    return gridTop - highestView;
  }

  /**
   * Should be called with populating set to true.
   *
   * @param fromPosition Position to start filling from
   * @param overhang the number of extra pixels to fill beyond the current bottom edge
   * @return the max overhang beyond the end of the view of any added items at the bottom
   */
  final int fillDown(int fromPosition, int overhang) {
    final int paddingLeft = getPaddingLeft();
    final int paddingRight = getPaddingRight();
    final int itemMargin = this.itemMargin;
    final int colWidth =
        (getWidth() - paddingLeft - paddingRight - itemMargin * (colCount - 1)) / colCount;
    final int gridBottom = getHeight() - getPaddingBottom();
    final int fillTo = gridBottom + overhang;
    int nextCol = getNextColumnDown();
    int position = fromPosition;

    while (nextCol >= 0 && itemBottoms[nextCol] < fillTo && position < itemCount) {
      final View child = obtainView(position, null);
      LayoutParams lp = (LayoutParams) child.getLayoutParams();

      if (child.getParent() != this) {
        if (inLayout) {
          addViewInLayout(child, -1, lp);
        } else {
          addView(child);
        }
      }

      final int span = Math.min(colCount, lp.span);
      final int widthSize = colWidth * span + itemMargin * (span - 1);
      final int widthSpec = MeasureSpec.makeMeasureSpec(widthSize, MeasureSpec.EXACTLY);

      LayoutRecord rec;
      if (span > 1) {
        rec = getNextRecordDown(position, span);
        nextCol = rec.column;
      } else {
        rec = layoutRecords.get(position);
      }

      boolean invalidateAfter = false;
      if (rec == null) {
        rec = new LayoutRecord();
        layoutRecords.put(position, rec);
        rec.column = nextCol;
        rec.span = span;
      } else if (span != rec.span) {
        rec.span = span;
        rec.column = nextCol;
        invalidateAfter = true;
      } else {
        nextCol = rec.column;
      }

      if (hasStableIds) {
        final long id = adapter.getItemId(position);
        rec.id = id;
        lp.id = id;
      }

      lp.column = nextCol;

      final int heightSpec;
      if (lp.height == LayoutParams.WRAP_CONTENT) {
        heightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
      } else {
        heightSpec = MeasureSpec.makeMeasureSpec(lp.height, MeasureSpec.EXACTLY);
      }
      child.measure(widthSpec, heightSpec);

      final int childHeight = child.getMeasuredHeight();
      if (invalidateAfter || (childHeight != rec.height && rec.height > 0)) {
        invalidateLayoutRecordsAfterPosition(position);
      }
      rec.height = childHeight;

      final int startFrom;
      if (span > 1) {
        int lowest = itemBottoms[nextCol];
        for (int i = nextCol + 1; i < nextCol + span; i++) {
          final int bottom = itemBottoms[i];
          if (bottom > lowest) {
            lowest = bottom;
          }
        }
        startFrom = lowest;
      } else {
        startFrom = itemBottoms[nextCol];
      }
      final int childTop = startFrom + itemMargin;
      final int childBottom = childTop + childHeight;
      final int childLeft = paddingLeft + nextCol * (colWidth + itemMargin);
      final int childRight = childLeft + child.getMeasuredWidth();
      child.layout(childLeft, childTop, childRight, childBottom);

      for (int i = nextCol; i < nextCol + span; i++) {
        itemBottoms[i] = childBottom + rec.getMarginBelow(i - nextCol);
      }

      nextCol = getNextColumnDown();
      position++;
    }

    int lowestView = 0;
    for (int i = 0; i < colCount; i++) {
      if (itemBottoms[i] > lowestView) {
        lowestView = itemBottoms[i];
      }
    }
    return lowestView - gridBottom;
  }

  /**
   * @return column that the next view filling upwards should occupy. This is the bottom-most
   * position available for a single-column item.
   */
  final int getNextColumnUp() {
    int result = -1;
    int bottomMost = Integer.MIN_VALUE;

    final int colCount = this.colCount;
    for (int i = colCount - 1; i >= 0; i--) {
      final int top = itemTops[i];
      if (top > bottomMost) {
        bottomMost = top;
        result = i;
      }
    }
    return result;
  }

  /**
   * Return a LayoutRecord for the given position.
   */
  final LayoutRecord getNextRecordUp(int position, int span) {
    LayoutRecord rec = layoutRecords.get(position);
    if (rec == null) {
      rec = new LayoutRecord();
      rec.span = span;
      layoutRecords.put(position, rec);
    } else if (rec.span != span) {
      throw new IllegalStateException("Invalid LayoutRecord! Record had span="
          + rec.span
          + " but caller requested span="
          + span
          + " for position="
          + position);
    }
    int targetCol = -1;
    int bottomMost = Integer.MIN_VALUE;

    final int colCount = this.colCount;
    for (int i = colCount - span; i >= 0; i--) {
      int top = Integer.MAX_VALUE;
      for (int j = i; j < i + span; j++) {
        final int singleTop = itemTops[j];
        if (singleTop < top) {
          top = singleTop;
        }
      }
      if (top > bottomMost) {
        bottomMost = top;
        targetCol = i;
      }
    }

    rec.column = targetCol;

    for (int i = 0; i < span; i++) {
      rec.setMarginBelow(i, itemTops[i + targetCol] - bottomMost);
    }

    return rec;
  }

  /**
   * @return column that the next view filling downwards should occupy. This is the top-most
   * position available.
   */
  final int getNextColumnDown() {
    int result = -1;
    int topMost = Integer.MAX_VALUE;

    final int colCount = this.colCount;
    for (int i = 0; i < colCount; i++) {
      final int bottom = itemBottoms[i];
      if (bottom < topMost) {
        topMost = bottom;
        result = i;
      }
    }
    return result;
  }

  final LayoutRecord getNextRecordDown(int position, int span) {
    LayoutRecord rec = layoutRecords.get(position);
    if (rec == null) {
      rec = new LayoutRecord();
      rec.span = span;
      layoutRecords.put(position, rec);
    } else if (rec.span != span) {
      throw new IllegalStateException("Invalid LayoutRecord! Record had span="
          + rec.span
          + " but caller requested span="
          + span
          + " for position="
          + position);
    }
    int targetCol = -1;
    int topMost = Integer.MAX_VALUE;

    final int colCount = this.colCount;
    for (int i = 0; i <= colCount - span; i++) {
      int bottom = Integer.MIN_VALUE;
      for (int j = i; j < i + span; j++) {
        final int singleBottom = itemBottoms[j];
        if (singleBottom > bottom) {
          bottom = singleBottom;
        }
      }
      if (bottom < topMost) {
        topMost = bottom;
        targetCol = i;
      }
    }

    rec.column = targetCol;

    for (int i = 0; i < span; i++) {
      rec.setMarginAbove(i, topMost - itemBottoms[i + targetCol]);
    }

    return rec;
  }

  /**
   * Obtain a populated view from the adapter. If optScrap is non-null and is not
   * reused it will be placed in the recycle bin.
   *
   * @param position position to get view for
   * @param optScrap Optional scrap view; will be reused if possible
   * @return A new view, a recycled view from recycler, or optScrap
   */
  final View obtainView(int position, View optScrap) {
    View view = recycler.getTransientStateView(position);
    if (view != null) {
      view = adapter.getView(position, view, this);
      return view;
    }

    // Reuse optScrap if it's of the right type (and not null)
    final int optType =
        optScrap != null ? ((LayoutParams) optScrap.getLayoutParams()).viewType : -1;
    final int positionViewType = adapter.getItemViewType(position);
    final View scrap =
        optType == positionViewType ? optScrap : recycler.getScrapView(positionViewType);

    view = adapter.getView(position, scrap, this);

    if (view != scrap && scrap != null) {
      // The adapter didn't use it; put it back.
      recycler.addScrap(scrap);
    }

    ViewGroup.LayoutParams lp = view.getLayoutParams();

    if (view.getParent() != this) {
      if (lp == null) {
        lp = generateDefaultLayoutParams();
      } else if (!checkLayoutParams(lp)) {
        lp = generateLayoutParams(lp);
      }
    }

    final LayoutParams sglp = (LayoutParams) lp;
    sglp.position = position;
    sglp.viewType = positionViewType;

    return view;
  }

  public void setEmptyView(View emptyView) {
    this.emptyView = emptyView;
  }

  private void updateEmptyState() {
    final boolean empty = adapter == null || adapter.getCount() == 0;
    if (empty) {
      setVisibility(GONE);
      emptyView.setVisibility(VISIBLE);
    } else {
      setVisibility(VISIBLE);
      emptyView.setVisibility(GONE);
    }
  }

  public ListAdapter getAdapter() {
    return adapter;
  }

  public void setAdapter(ListAdapter adapter) {
    if (this.adapter != null) {
      this.adapter.unregisterDataSetObserver(observer);
    }
    // TODO: If the new adapter says that there are stable IDs, remove certain layout records
    // and onscreen views if they have changed instead of removing all of the state here.
    clearAllState();
    this.adapter = adapter;
    dataChanged = true;
    oldItemCount = itemCount = adapter != null ? adapter.getCount() : 0;
    if (adapter != null) {
      adapter.registerDataSetObserver(observer);
      recycler.setViewTypeCount(adapter.getViewTypeCount());
      hasStableIds = adapter.hasStableIds();
    } else {
      hasStableIds = false;
    }
    populate();
    updateEmptyState();
  }

  /**
   * Clear all state because the grid will be used for a completely different set of data.
   */
  private void clearAllState() {
    // Clear all layout records and views
    layoutRecords.clear();
    removeAllViews();

    // Reset to the top of the grid
    resetStateForGridTop();

    // Clear recycler because there could be different view types now
    recycler.clear();
  }

  /**
   * Reset all internal state to be at the top of the grid.
   */
  private void resetStateForGridTop() {
    // Reset itemTops and itemBottoms
    final int colCount = this.colCount;
    if (itemTops == null || itemTops.length != colCount) {
      itemTops = new int[colCount];
      itemBottoms = new int[colCount];
    }
    final int top = getPaddingTop() + Math.min(restoreOffset, 0);
    Arrays.fill(itemTops, top);
    Arrays.fill(itemBottoms, top);

    // Reset the first visible position in the grid to be item 0
    firstPosition = 0;
    restoreOffset = 0;
  }

  /**
   * Scroll the list so the first visible position in the grid is the first item in the adapter.
   */
  public void setSelectionToTop() {
    // Clear out the views (but don't clear out the layout records or recycler because the data
    // has not changed)
    removeAllViews();

    // Reset to top of grid
    resetStateForGridTop();

    // Start populating again
    populate();
  }

  @Override
  protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.WRAP_CONTENT);
  }

  @Override
  protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams lp) {
    return new LayoutParams(lp);
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams lp) {
    return lp instanceof LayoutParams;
  }

  @Override
  public ViewGroup.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  public Parcelable onSaveInstanceState() {
    final Parcelable superState = super.onSaveInstanceState();
    final SavedState ss = new SavedState(superState);
    final int position = firstPosition;
    ss.position = position;
    if (position >= 0 && adapter != null && position < adapter.getCount()) {
      ss.firstId = adapter.getItemId(position);
    }
    if (getChildCount() > 0) {
      ss.topOffset = getChildAt(0).getTop() - itemMargin - getPaddingTop();
    }
    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    dataChanged = true;
    firstPosition = ss.position;
    restoreOffset = ss.topOffset;

    if (restoreOffset != 0 && itemTops != null) {
      Arrays.fill(itemTops, restoreOffset + getPaddingTop() + itemMargin);
      restoreOffset = 0;
    }
    requestLayout();
  }

  public static class LayoutParams extends ViewGroup.LayoutParams {
    private static final int[] LAYOUT_ATTRS = new int[] {
        android.R.attr.layout_span
    };

    private static final int SPAN_INDEX = 0;

    /**
     * The number of columns this item should span.
     */
    public int span = 1;

    /**
     * Item position this view represents.
     */
    int position;

    /**
     * Type of this view as reported by the adapter.
     */
    int viewType;

    /**
     * The column this view is occupying.
     */
    int column;

    /**
     * The stable ID of the item this view displays.
     */
    long id = -1;

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

      TypedArray a = c.obtainStyledAttributes(attrs, LAYOUT_ATTRS);
      span = a.getInteger(SPAN_INDEX, 1);
      a.recycle();
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

  private class RecycleBin {
    private ArrayList<View>[] scrapViews;
    private int viewTypeCount;
    private int maxScrap;

    private SparseArray<View> transientStateViews;
    private LongSparseArray<View> transientStateViewsById;

    public void setViewTypeCount(int viewTypeCount) {
      if (viewTypeCount < 1) {
        throw new IllegalArgumentException(
            "Must have at least one view type (" + viewTypeCount + " types reported)");
      }
      if (viewTypeCount == this.viewTypeCount) {
        return;
      }

      ArrayList<View>[] scrapViews = new ArrayList[viewTypeCount];
      for (int i = 0; i < viewTypeCount; i++) {
        scrapViews[i] = new ArrayList<View>();
      }
      this.viewTypeCount = viewTypeCount;
      this.scrapViews = scrapViews;
    }

    public void clear() {
      final int typeCount = viewTypeCount;
      for (int i = 0; i < typeCount; i++) {
        scrapViews[i].clear();
      }
      if (transientStateViews != null) {
        transientStateViews.clear();
      }
      if (transientStateViewsById != null) {
        transientStateViewsById.clear();
      }
    }

    public void clearTransientViews() {
      if (transientStateViews != null) {
        transientStateViews.clear();
      }
      if (transientStateViewsById != null) {
        transientStateViewsById.clear();
      }
    }

    public void addScrap(View v) {
      final LayoutParams lp = (LayoutParams) v.getLayoutParams();
      if (v.hasTransientState()) {
        if (adapter != null) {
          if (adapter.hasStableIds()) {
            if (transientStateViewsById == null) {
              transientStateViewsById = new LongSparseArray<View>();
            }
            transientStateViewsById.put(lp.id, v);
          } else {
            if (transientStateViews == null) {
              transientStateViews = new SparseArray<View>();
            }
            transientStateViews.put(lp.position, v);
          }
        }
        return;
      }

      final int childCount = getChildCount();
      if (childCount > maxScrap) {
        maxScrap = childCount;
      }

      ArrayList<View> scrap = scrapViews[lp.viewType];
      if (scrap.size() < maxScrap) {
        scrap.add(v);
      }
    }

    public View getTransientStateView(int position) {
      if (adapter != null && adapter.hasStableIds() && transientStateViewsById != null) {
        final long id = adapter.getItemId(position);
        View result = transientStateViewsById.get(id);
        transientStateViewsById.remove(id);
        return result;
      }

      if (transientStateViews != null) {
        final View result = transientStateViews.get(position);
        if (result != null) {
          transientStateViews.remove(position);
        }
        return result;
      }

      return null;
    }

    public View getScrapView(int type) {
      ArrayList<View> scrap = scrapViews[type];
      if (scrap.isEmpty()) {
        return null;
      }

      final int index = scrap.size() - 1;
      final View result = scrap.get(index);
      scrap.remove(index);
      return result;
    }
  }

  private class AdapterDataSetObserver extends DataSetObserver {
    @Override
    public void onChanged() {
      dataChanged = true;
      oldItemCount = itemCount;
      itemCount = adapter.getCount();

      // TODO: Consider matching these back up if we have stable IDs.
      recycler.clearTransientViews();

      if (!hasStableIds) {
        // Clear all layout records and recycle the views
        layoutRecords.clear();
        recycleAllViews();

        // Reset item bottoms to be equal to item tops
        final int colCount = StaggeredGridView.this.colCount;
        for (int i = 0; i < colCount; i++) {
          itemBottoms[i] = itemTops[i];
        }
      }

      // TODO: Handle based on ID instead of position
      if (firstPosition >= itemCount) {
        firstPosition = Math.max(Math.min(firstPosition, itemCount - 1), 0);
      }

      for (int i = getChildCount() - 1; i >= 1; i--) {
        final View child = getChildAt(i);
        LayoutParams lp = (LayoutParams) child.getLayoutParams();
        removeViewAt(i);
        layoutRecords.removeAt(lp.position);
        recycler.addScrap(child);
      }

      requestLayout();
      updateEmptyState();
    }

    @Override
    public void onInvalidated() {
      updateEmptyState();
    }
  }

  static class SavedState extends BaseSavedState {
    long firstId = -1;
    int position;
    int topOffset;

    SavedState(Parcelable superState) {
      super(superState);
    }

    private SavedState(Parcel in) {
      super(in);
      firstId = in.readLong();
      position = in.readInt();
      topOffset = in.readInt();
    }

    @Override
    public void writeToParcel(Parcel out, int flags) {
      super.writeToParcel(out, flags);
      out.writeLong(firstId);
      out.writeInt(position);
      out.writeInt(topOffset);
    }

    @Override
    public String toString() {
      return "StaggereGridView.SavedState{"
          + Integer.toHexString(System.identityHashCode(this))
          + " firstId="
          + firstId
          + " position="
          + position
          + "}";
    }

    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
      public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}
