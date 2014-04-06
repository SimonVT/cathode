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
  private int verticalItemMargin;
  private int horizontalItemMargin;

  private int[] itemTops;
  private int[] itemBottoms;

  private boolean populating;
  private boolean inLayout;
  private int restoreOffset;

  private int colWidth;

  private final RecycleBin recycler = new RecycleBin();

  private final AdapterDataSetObserver observer = new AdapterDataSetObserver();

  private boolean dataChanged;
  private int itemCount;
  private boolean hasStableIds;

  private int firstPosition;
  private long firstPositionId = 0;

  private int touchSlop;
  private int maximumVelocity;
  private int flingVelocity;
  private float lastTouchY;
  private float lastTouchX;
  private float touchRemainderY;
  private int activePointerId;

  private boolean[] isScrap = new boolean[1];

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
    public int[] topMargin;

    private LayoutRecord(int column, int span) {
      this.column = column;
      topMargin = new int[span];
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
    verticalItemMargin =
        a.getDimensionPixelSize(R.styleable.StaggeredGridView_verticalItemMargin, 0);
    horizontalItemMargin =
        a.getDimensionPixelSize(R.styleable.StaggeredGridView_horizontalItemMargin, 0);
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
    final boolean dataChanged = colCount != this.colCount;
    this.colCount = colCountSetting = colCount;
    if (dataChanged) {
      layoutRecords.clear();
      requestLayout();
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
        if (tapReset != null) {
          removeCallbacks(tapReset);
          tapReset = null;
        }
        if (pendingTapCheck != null) {
          removeCallbacks(pendingTapCheck);
          pendingTapCheck = null;
        }

        velocityTracker.clear();
        scroller.abortAnimation();
        lastTouchY = ev.getY();
        lastTouchX = ev.getX();
        final int x = (int) ev.getX();
        activePointerId = ev.getPointerId(0);
        touchRemainderY = 0;
        motionPosition = getPositionAt(x, (int) lastTouchY);
        if (motionPosition != INVALID_POSITION && adapter != null && adapter.isEnabled(
            motionPosition)) {
          pendingTapCheck = new TapCheck();
          postDelayed(pendingTapCheck, ViewConfiguration.getTapTimeout());
          if (hasStableIds) {
            motionId =
                ((LayoutParams) getChildAt(motionPosition - firstPosition).getLayoutParams()).id;
          }
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
          View child = getChildAt(motionPosition - firstPosition);
          child.setPressed(false);

          setPressed(false);
        }

        motionPosition = INVALID_POSITION;
        motionId = -1L;
        selectorRect.setEmpty();

        if (pendingTapCheck != null) {
          removeCallbacks(pendingTapCheck);
          pendingTapCheck = null;
        }
        if (tapReset != null) {
          removeCallbacks(tapReset);
          tapReset = null;
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

          if (motionPosition != INVALID_POSITION) {
            View child = getChildAt(motionPosition - firstPosition);
            if (child != null) {
              child.setPressed(false);
            }

            setPressed(false);

            motionPosition = INVALID_POSITION;
            motionId = -1L;
            selectorRect.setEmpty();

            if (pendingTapCheck != null) {
              removeCallbacks(pendingTapCheck);
              pendingTapCheck = null;
            }
          }
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
        overhang = fillDown(firstPosition + getChildCount(), allowOverhang);
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
      recycler.addScrap(getChildAt(i), firstPosition + i);
    }

    detachAllViewsFromParent();
  }

  /**
   * Important: this method will leave offscreen views attached if they
   * are required to maintain the invariant that child view with index i
   * is always the view corresponding to position firstPosition + i.
   */
  private void recycleOffscreenViews() {
    final int height = getHeight();
    final int clearAbove = -verticalItemMargin;
    final int clearBelow = height + verticalItemMargin;
    for (int i = getChildCount() - 1; i >= 0; i--) {
      final View child = getChildAt(i);
      if (child.getTop() <= clearBelow) {
        // There may be other offscreen views, but we need to maintain
        // the invariant documented above.
        break;
      }

      detachViewFromParent(i);

      recycler.addScrap(child, i + firstPosition);
    }

    while (getChildCount() > 0) {
      final View child = getChildAt(0);
      if (child.getBottom() >= clearAbove) {
        // There may be other offscreen views, but we need to maintain
        // the invariant documented above.
        break;
      }

      detachViewFromParent(0);

      recycler.addScrap(child, firstPosition);
      setFirstPosition(firstPosition + 1);
    }

    final int childCount = getChildCount();
    if (childCount > 0) {
      // Repair the top and bottom column boundaries from the views we still have
      Arrays.fill(itemTops, Integer.MAX_VALUE);
      Arrays.fill(itemBottoms, Integer.MIN_VALUE);

      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        final LayoutParams lp = (LayoutParams) child.getLayoutParams();
        final int top = child.getTop();
        final int bottom = child.getBottom();
        LayoutRecord rec = layoutRecords.get(lp.position);

        final int colEnd = lp.column + Math.min(colCount, lp.span);
        for (int col = lp.column; col < colEnd; col++) {
          int itemTop = top;
          if (rec != null) {
            itemTop -= rec.topMargin[col - lp.column];
          }
          if (itemTop < itemTops[col]) {
            itemTops[col] = itemTop;
          }
          if (bottom > itemBottoms[col]) {
            itemBottoms[col] = bottom;
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

  @Override public void computeScroll() {
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

  @Override protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
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

  @Override
  public void requestLayout() {
    if (!populating) {
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

    colWidth =
        (widthSize - getPaddingLeft() - getPaddingRight() - verticalItemMargin * (colCount - 1))
            / colCount;

    if (colCountSetting == COLUMN_COUNT_AUTO) {
      final int colCount = widthSize / minColWidth;
      if (colCount != this.colCount) {
        this.colCount = colCount;
      }
    }
  }

  private void handleDataChanged() {
    if (itemCount == 0) {
      if (itemTops != null) {
        Arrays.fill(itemTops, 0);
      }
      if (itemBottoms != null) {
        Arrays.fill(itemBottoms, 0);
      }

      clearChildViews();

      dataChanged = false;
    }
  }

  private void clearChildViews() {
    for (int i = getChildCount() - 1; i >= 1; i--) {
      final View child = getChildAt(i);
      detachViewFromParent(child);
      recycler.addScrap(child, firstPosition + i);
    }
  }

  private void setFirstPosition(int position) {
    if (position != firstPosition) {
      firstPosition = position;
      if (hasStableIds) {
        firstPositionId = adapter.getItemId(position);
      }
    }
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    inLayout = true;

    if (dataChanged) {
      int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        getChildAt(i).forceLayout();
      }
      recycler.markChildrenDirty();
      clearChildViews();
    }

    layoutChildren();
    inLayout = false;

    final int width = r - l;
    final int height = b - t;
    topEdge.setSize(width, height);
    bottomEdge.setSize(width, height);
  }

  private void layoutChildren() {
    if (getWidth() == 0 || getHeight() == 0 || itemCount == 0) {
      dataChanged = false;
      return;
    }

    invalidate();

    final int childCount = getChildCount();

    if (itemTops == null || itemTops.length != colCount) {
      itemTops = new int[colCount];
      itemBottoms = new int[colCount];
    }

    if (dataChanged) {
      final View firstChild = getChildAt(0);
      if (firstChild != null) {
        final int firstTop = firstChild.getTop();
        Arrays.fill(itemTops, firstTop);
        Arrays.fill(itemBottoms, firstTop - verticalItemMargin);
      } else {
        Arrays.fill(itemTops, getPaddingTop());
        Arrays.fill(itemBottoms, getPaddingTop() - verticalItemMargin);
      }

      for (int i = 0; i < childCount; i++) {
        getChildAt(i).forceLayout();
      }

      recycler.markChildrenDirty();
    } else {
      final int count = colCount;
      for (int i = 0; i < count; i++) {
        itemBottoms[i] = itemTops[i] - verticalItemMargin;
      }
    }

    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      recycler.addScrap(child, firstPosition + i);
    }

    detachAllViewsFromParent();

    fillDown(firstPosition, 0);
    adjustViewsDown();
    fillUp(firstPosition - 1, 0);
    correctTooLow();

    dataChanged = false;
  }

  private int fillDown(int fromPosition, int overhang) {
    final int height = getHeight();
    int nextCol = getNextColumnDown();
    int position = fromPosition;

    while (position < itemCount && itemBottoms[nextCol] < height + overhang) {
      View child = makeAndAddView(position);
      LayoutParams lp = (LayoutParams) child.getLayoutParams();
      final int span = Math.min(lp.span, colCount);

      LayoutRecord rec = layoutRecords.get(position);

      int startColumn;
      if (rec != null) {
        startColumn = rec.column;
      } else {
        startColumn = getNextColumnDown(span);

        rec = new LayoutRecord(startColumn, span);
        layoutRecords.put(position, rec);
      }
      int startFrom = Integer.MIN_VALUE;
      for (int i = startColumn; i < startColumn + span; i++) {
        final int bottom = itemBottoms[i];
        if (bottom > startFrom) {
          startFrom = bottom;
        }
      }
      lp.column = startColumn;

      final int left =
          getPaddingLeft() + colWidth * startColumn + horizontalItemMargin * startColumn;
      final int top = startFrom + verticalItemMargin;
      final int right = left + colWidth * span + horizontalItemMargin * (span - 1);
      final int bottom = top + child.getMeasuredHeight();
      child.layout(left, top, right, bottom);

      for (int i = startColumn; i < startColumn + span; i++) {
        final int oldBottom = itemBottoms[i];
        rec.topMargin[i - startColumn] = top - verticalItemMargin - oldBottom;

        itemBottoms[i] = bottom;
      }

      nextCol = getNextColumnDown();

      position++;
    }

    int lowest = 0;
    for (int i = 0; i < colCount; i++) {
      final int bottom = itemBottoms[i];
      if (bottom > lowest) {
        lowest = bottom;
      }
    }

    return lowest - height - getPaddingBottom();
  }

  private int fillUp(int fromPosition, int overhang) {
    int nextCol = getNextColumnUp();
    int position = fromPosition;

    while (position >= 0 && itemTops[nextCol] > 0 - overhang) {
      View child = makeAndAddView(position);
      LayoutParams lp = (LayoutParams) child.getLayoutParams();
      final int span = Math.min(lp.span, colCount);

      int startColumn;
      LayoutRecord rec = layoutRecords.get(position);
      if (rec != null) {
        startColumn = rec.column;
      } else {
        startColumn = getNextColumnUp(span);
        rec = new LayoutRecord(startColumn, span);
        layoutRecords.put(position, rec);
      }

      int startFrom = Integer.MAX_VALUE;
      for (int i = startColumn; i < startColumn + span; i++) {
        final int top = itemTops[i];
        if (top < startFrom) {
          startFrom = top;
        }
      }
      lp.column = startColumn;

      final int left =
          getPaddingLeft() + colWidth * startColumn + horizontalItemMargin * startColumn;
      final int right = left + colWidth * span + horizontalItemMargin * (span - 1);
      final int bottom = startFrom - verticalItemMargin;
      final int top = bottom - child.getMeasuredHeight();
      child.layout(left, top, right, bottom);

      for (int i = startColumn; i < startColumn + span; i++) {
        int topOffset = 0;
        if (rec != null) {
          topOffset = rec.topMargin[i - startColumn];
        }
        itemTops[i] = top - topOffset;
      }

      nextCol = getNextColumnUp();
      setFirstPosition(position);
      position--;
    }

    int highest = getHeight();
    for (int i = 0; i < colCount; i++) {
      final int top = itemTops[i];
      if (top < highest) {
        highest = top;
      }
    }

    return getPaddingTop() - highest;
  }

  private void correctTooLow() {
    final int childCount = getChildCount();
    if (childCount > 0) {
      if (colCount == 1) {
        if (firstPosition == 0) {
          final View firstChild = getChildAt(0);
          final int firstChildTop = firstChild.getTop();
          if (firstChildTop > getPaddingTop()) {
            offsetChildren(-firstChildTop + getPaddingTop());
            fillDown(firstPosition + childCount, 0);
          }
        }
      } else {
        int[] bottoms = new int[colCount];
        for (int i = 0; i < colCount; i++) {
          bottoms[i] = Integer.MIN_VALUE;
        }

        for (int i = 0; i < childCount; i++) {
          final View child = getChildAt(i);
          final LayoutParams lp = (LayoutParams) child.getLayoutParams();
          final int childTop = child.getTop();
          final int childBottom = child.getBottom();
          final int span = Math.min(colCount, lp.span);
          int columnBottom = Integer.MIN_VALUE;
          for (int j = lp.column; j < lp.column + span; j++) {
            if (bottoms[j] > columnBottom) {
              columnBottom = bottoms[j];
            }
          }

          if (columnBottom == Integer.MIN_VALUE) {
            columnBottom = getPaddingTop();
          } else {
            columnBottom += verticalItemMargin;
          }

          if (childTop > columnBottom && childTop > 0) {
            final int delta = columnBottom - childTop;
            child.offsetTopAndBottom(delta);
            for (int j = lp.column; j < lp.column + span; j++) {
              if (childTop == itemTops[j]) {
                itemTops[j] = child.getTop();
              }
              if (childBottom == itemBottoms[j]) {
                itemBottoms[j] = child.getBottom();
              }
            }
          }

          for (int j = lp.column; j < lp.column + span; j++) {
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
        final int delta = (getHeight() - getPaddingBottom()) - lastChild.getBottom();
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

        final int delta = (getHeight() - getPaddingBottom()) - lowestBottom;
        if (delta > 0) {
          offsetChildren(delta);
        }
      }
    }
  }

  final void offsetChildren(int offset) {
    final int childCount = getChildCount();
    for (int i = 0; i < childCount; i++) {
      final View child = getChildAt(i);
      child.offsetTopAndBottom(offset);
    }

    final int colCount = this.colCount;
    for (int i = 0; i < colCount; i++) {
      itemTops[i] += offset;
      itemBottoms[i] += offset;
    }
  }

  final int getNextColumnUp(int span) {
    int min;
    int max = Integer.MIN_VALUE;
    int maxColumn = 0;

    for (int i = colCount - 1; i >= span - 1; i--) {
      min = Integer.MAX_VALUE;
      final int col = i - (span - 1);
      for (int j = i; j > i - span; j--) {
        final int top = itemTops[j];
        if (top < min) {
          min = top;
        }
      }

      if (min > max) {
        max = min;
        maxColumn = col;
      }
    }

    return maxColumn;
  }

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

  final int getNextColumnDown(int span) {
    int max;
    int min = Integer.MAX_VALUE;
    int minColumn = 0;

    for (int i = 0; i < colCount - (span - 1); i++) {
      max = Integer.MIN_VALUE;
      for (int j = i; j < i + span; j++) {
        final int bottom = itemBottoms[j];
        if (bottom > max) {
          max = bottom;
        }
      }

      if (max < min) {
        min = max;
        minColumn = i;
      }
    }

    return minColumn;
  }

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

  private View makeAndAddView(int position) {
    View child = obtainView(position, isScrap);
    LayoutParams lp = (LayoutParams) child.getLayoutParams();

    final boolean recycled = isScrap[0];
    final int addAt = position < firstPosition ? 0 : -1;
    if (recycled) {
      attachViewToParent(child, addAt, child.getLayoutParams());
    } else {
      addViewInLayout(child, addAt, child.getLayoutParams());
    }

    int childHeightSpec =
        ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED), 0,
            lp.height);

    int childWidthSpec =
        ViewGroup.getChildMeasureSpec(MeasureSpec.makeMeasureSpec(colWidth, MeasureSpec.EXACTLY), 0,
            lp.width);
    child.measure(childWidthSpec, childHeightSpec);

    return child;
  }

  final View obtainView(int position, boolean[] isScrap) {
    isScrap[0] = false;
    View scrapView;

    scrapView = recycler.getTransientStateView(position);
    if (scrapView == null) {
      scrapView = recycler.getScrapView(position);
    }

    View child;
    if (scrapView != null) {
      child = adapter.getView(position, scrapView, this);
      if (child != scrapView) {
        isScrap[0] = false;
        recycler.addScrap(scrapView, position);
      } else {
        isScrap[0] = true;
      }
    } else {
      isScrap[0] = false;
      child = adapter.getView(position, null, this);
    }

    int viewType = adapter.getItemViewType(position);
    final LayoutParams sglp = (LayoutParams) child.getLayoutParams();
    sglp.position = position;
    sglp.viewType = viewType;

    if (hasStableIds) {
      sglp.id = adapter.getItemId(position);
    }

    return child;
  }

  public void setEmptyView(View emptyView) {
    this.emptyView = emptyView;
  }

  private void updateEmptyState() {
    final boolean empty = adapter == null || adapter.getCount() == 0;
    if (empty) {
      setVisibility(GONE);
      if (emptyView != null) emptyView.setVisibility(VISIBLE);

      if (dataChanged) {
        handleDataChanged();
      }
    } else {
      setVisibility(VISIBLE);
      if (emptyView != null) emptyView.setVisibility(GONE);
    }
  }

  public ListAdapter getAdapter() {
    return adapter;
  }

  public void setAdapter(ListAdapter adapter) {
    if (this.adapter != null) {
      this.adapter.unregisterDataSetObserver(observer);
    }

    clearAllState();
    this.adapter = adapter;
    dataChanged = true;
    itemCount = adapter != null ? adapter.getCount() : 0;
    if (adapter != null) {
      adapter.registerDataSetObserver(observer);
      recycler.setViewTypeCount(adapter.getViewTypeCount());
      hasStableIds = adapter.hasStableIds();
    } else {
      hasStableIds = false;
    }
    requestLayout();
    updateEmptyState();
  }

  /**
   * Clear all state because the grid will be used for a completely different set of data.
   */
  private void clearAllState() {
    // Clear all layout records and views
    removeAllViews();

    // Reset to the top of the grid
    resetStateForGridTop();

    // Clear recycler because there could be different view types now
    recycler.clear();

    layoutRecords.clear();
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
    firstPositionId = -1;
    restoreOffset = 0;
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
      ss.topOffset = getChildAt(0).getTop();
    }
    return ss;
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    SavedState ss = (SavedState) state;
    super.onRestoreInstanceState(ss.getSuperState());
    dataChanged = true;
    firstPosition = ss.position;
    firstPositionId = ss.firstId;
    restoreOffset = ss.topOffset;

    if (restoreOffset != 0 && itemTops != null) {
      Arrays.fill(itemTops, restoreOffset);
      Arrays.fill(itemBottoms, restoreOffset - verticalItemMargin);
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

    int scrappedFromPosition;

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
        ArrayList<View> scrap = scrapViews[i];
        for (View view : scrap) {
          removeDetachedView(view, false);
        }
      }
      clearTransientViews();
    }

    public void clearTransientViews() {
      if (transientStateViews != null) {
        for (int i = 0; i < transientStateViews.size(); i++) {
          removeDetachedView(transientStateViews.valueAt(i), false);
        }
        transientStateViews.clear();
      }
      if (transientStateViewsById != null) {
        for (int i = 0; i < transientStateViewsById.size(); i++) {
          removeDetachedView(transientStateViewsById.valueAt(i), false);
        }
        transientStateViewsById.clear();
      }
    }

    public void addScrap(View v, int position) {
      final LayoutParams lp = (LayoutParams) v.getLayoutParams();
      if (lp == null) {
        return;
      }

      lp.scrappedFromPosition = position;

      if (v.hasTransientState()) {
        if (adapter != null) {
          if (adapter.hasStableIds()) {
            if (transientStateViewsById == null) {
              transientStateViewsById = new LongSparseArray<View>();
            }
            transientStateViewsById.put(lp.id, v);
          } else if (!dataChanged) {
            if (transientStateViews == null) {
              transientStateViews = new SparseArray<View>();
            }
            transientStateViews.put(lp.position, v);
          } else {
            removeDetachedView(v, false);
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
        if (result != null) {
          transientStateViewsById.remove(id);
        }
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

    public View getScrapView(int position) {
      ArrayList<View> scrap;

      if (viewTypeCount == 1) {
        scrap = scrapViews[0];
      } else {
        final int viewType = adapter.getItemViewType(position);
        scrap = scrapViews[viewType];
      }

      final int size = scrap.size();
      if (size > 0) {
        for (int i = 0; i < size; i++) {
          final View view = scrap.get(i);
          final LayoutParams lp = (LayoutParams) view.getLayoutParams();
          if (position == lp.scrappedFromPosition) {
            scrap.remove(view);
            return view;
          }
        }

        return scrap.remove(size - 1);
      }

      return null;
    }

    public void markChildrenDirty() {
      final int typeCount = viewTypeCount;
      for (int i = 0; i < typeCount; i++) {
        final ArrayList<View> scrap = scrapViews[i];
        final int scrapCount = scrap.size();
        for (int j = 0; j < scrapCount; j++) {
          scrap.get(j).forceLayout();
        }
      }

      if (transientStateViews != null) {
        final int count = transientStateViews.size();
        for (int i = 0; i < count; i++) {
          transientStateViews.valueAt(i).forceLayout();
        }
      }

      if (transientStateViewsById != null) {
        final int count = transientStateViewsById.size();
        for (int i = 0; i < count; i++) {
          transientStateViewsById.valueAt(i).forceLayout();
        }
      }
    }
  }

  private int tryFindIdPosition(long id) {
    final int count = itemCount;
    if (count == 0) {
      return INVALID_POSITION;
    }

    int first = firstPosition;
    int last = firstPosition;
    int toCheck = firstPosition;

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
      hitLast = hitLast || toCheck == itemCount - 1;

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

  private class AdapterDataSetObserver extends DataSetObserver {
    @Override
    public void onChanged() {
      dataChanged = true;
      itemCount = adapter.getCount();

      layoutRecords.clear();

      // TODO: Consider matching these back up if we have stable IDs.
      recycler.clearTransientViews();

      if (firstPosition >= itemCount) {
        firstPosition = Math.max(Math.min(firstPosition, itemCount - 1), 0);
      }

      if (hasStableIds) {
        final int position = tryFindIdPosition(firstPositionId);
        if (position != INVALID_POSITION) {
          firstPosition = position;
        }
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
