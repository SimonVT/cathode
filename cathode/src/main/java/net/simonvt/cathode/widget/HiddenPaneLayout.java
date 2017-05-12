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
package net.simonvt.cathode.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.TargetApi;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.view.WindowInsets;

public class HiddenPaneLayout extends ViewGroup {

  private static final int INVALID_POINTER = -1;

  private static final int CLOSE_ENOUGH = 3;

  private static final SmoothInterpolator SMOOTH_INTERPOLATOR = new SmoothInterpolator();

  private static final String ANIMATE_PROPERTY = "offsetPixels";

  public static final int STATE_CLOSED = 0;
  public static final int STATE_CLOSING = 1;
  public static final int STATE_DRAGGING = 2;
  public static final int STATE_OPENING = 4;
  public static final int STATE_OPEN = 8;

  private int state = STATE_CLOSED;

  private ObjectAnimator animator;

  private int hiddenPaneWidth = -1;

  private float offsetPixels = 0;

  private View contentPane;

  private View hiddenPane;

  private boolean isDragging;

  private int closeEnough;

  private VelocityTracker velocityTracker;

  private float lastMotionX;

  private float lastMotionY;

  private float initialMotionX;

  private float initialMotionY;

  private int activePointerId;

  private int touchSlop;

  private int maxVelocity;

  private boolean layerTypeHardware;

  private Drawable dropShadow;

  private int dropShadowWidth;

  public HiddenPaneLayout(Context context) {
    super(context);
    init(context);
  }

  public HiddenPaneLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public HiddenPaneLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    final ViewConfiguration configuration = ViewConfiguration.get(context);
    touchSlop = configuration.getScaledTouchSlop();
    maxVelocity = configuration.getScaledMaximumFlingVelocity();
    closeEnough = dpToPx(CLOSE_ENOUGH);

    dropShadow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
        0x80000000, 0x00000000,
    });
    dropShadowWidth = dpToPx(6);

    setWillNotDraw(false);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP) @Override
  public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    hiddenPane.setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);
    return insets;
  }

  protected int dpToPx(int dp) {
    return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
  }

  public void toggle() {
    if (state == STATE_OPEN || state == STATE_OPENING) {
      close();
    } else if (state == STATE_CLOSED || state == STATE_CLOSING) {
      open();
    }
  }

  public void open() {
    open(true);
  }

  public void open(boolean animate) {
    animateOffsetTo(-hiddenPaneWidth, 0, animate);
  }

  public void close() {
    close(true);
  }

  public void close(boolean animate) {
    animateOffsetTo(0, 0, animate);
  }

  public int getState() {
    return state;
  }

  @Override public void addView(View child, int index, LayoutParams params) {
    super.addView(child, index, params);
    if (contentPane == null) {
      contentPane = child;
    } else if (hiddenPane == null) {
      hiddenPane = child;
    } else {
      throw new IllegalStateException("HiddenPaneLayout can only have two direct children");
    }
  }

  @Override protected void dispatchDraw(Canvas canvas) {
    super.dispatchDraw(canvas);
    final int width = getWidth();
    final int height = getHeight();
    final int offsetPixels = (int) this.offsetPixels;

    dropShadow.setBounds(width + offsetPixels, 0, width + offsetPixels + dropShadowWidth, height);
    dropShadow.draw(canvas);
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int width = r - l;
    final int height = b - t;

    View contentPane = getChildAt(0);
    contentPane.layout(0, 0, width, height);

    final View hiddenPane = getChildAt(1);
    final int hiddenPaneWidth = hiddenPane.getMeasuredWidth();
    hiddenPane.layout(width, 0, width + hiddenPaneWidth, height);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
      throw new IllegalStateException("Must measure with an exact size");
    }

    final int width = MeasureSpec.getSize(widthMeasureSpec);
    final int height = MeasureSpec.getSize(heightMeasureSpec);

    final int contentWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, width);
    final int contentHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, height);
    contentPane.measure(contentWidthMeasureSpec, contentHeightMeasureSpec);

    LayoutParams lp = hiddenPane.getLayoutParams();
    hiddenPaneWidth = lp.width;
    final int hiddenWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
    final int hiddenHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, height);
    hiddenPane.measure(hiddenWidthMeasureSpec, hiddenHeightMeasureSpec);

    setMeasuredDimension(width, height);
  }

  private void setOffsetPixels(float offsetPixels) {
    final int oldOffset = (int) this.offsetPixels;
    this.offsetPixels = offsetPixels;
    final int newOffset = (int) offsetPixels;
    if (newOffset != oldOffset) {
      contentPane.setTranslationX(newOffset);
      hiddenPane.setTranslationX(newOffset);
      invalidate();
    }
  }

  private boolean isContentTouch(int x) {
    return offsetPixels < 0.0f && x < getWidth() + (int) offsetPixels;
  }

  protected boolean onDownAllowDrag(int x) {
    return offsetPixels < 0.0f && x < getWidth() + (int) offsetPixels;
  }

  protected boolean onMoveAllowDrag(int x) {
    return offsetPixels < 0.0f && x < getWidth() + (int) offsetPixels;
  }

  protected void onMoveEvent(float dx) {
    setOffsetPixels(Math.max(Math.min(offsetPixels + dx, 0), -hiddenPaneWidth));
  }

  protected boolean checkTouchSlop(float dx, float dy) {
    return Math.abs(dx) > touchSlop && Math.abs(dx) > Math.abs(dy);
  }

  private boolean isCloseEnough() {
    return Math.abs(offsetPixels) <= closeEnough;
  }

  private void stopAnimation() {
    if (animator != null) {
      animator.cancel();
      animator = null;
    }
    stopLayerTranslation();
  }

  /** Called when a drag has been ended. */
  protected void endDrag() {
    isDragging = false;

    if (velocityTracker != null) {
      velocityTracker.recycle();
      velocityTracker = null;
    }
  }

  private void setState(int state) {
    if (state != this.state) {
      this.state = state;
    }
  }

  protected void animateOffsetTo(int position, int velocity, boolean animate) {
    endDrag();
    stopAnimation();

    final int startX = (int) offsetPixels;
    final int dx = position - startX;
    if (dx == 0 || !animate) {
      setOffsetPixels(position);
      setState(position == 0 ? STATE_CLOSED : STATE_OPEN);
      stopLayerTranslation();
      return;
    }

    int duration;

    velocity = Math.abs(velocity);
    if (velocity > 0) {
      duration = 4 * Math.round(1000.f * Math.abs((float) dx / velocity));
    } else {
      duration = (int) (600.f * Math.abs((float) dx / hiddenPaneWidth));
    }

    duration = Math.min(duration, 600);
    animateOffsetTo(position, duration);
  }

  protected void animateOffsetTo(final int position, int duration) {
    if (position < offsetPixels) {
      setState(STATE_OPENING);
    } else {
      setState(STATE_CLOSING);
    }

    animator = ObjectAnimator.ofFloat(this, ANIMATE_PROPERTY, offsetPixels, position);
    animator.setInterpolator(SMOOTH_INTERPOLATOR);
    animator.setDuration(duration);
    animator.addListener(new Animator.AnimatorListener() {
      @Override public void onAnimationStart(Animator animator) {
        startLayerTranslation();
      }

      @Override public void onAnimationEnd(Animator animator) {
        stopLayerTranslation();
        setState(position == 0 ? STATE_CLOSED : STATE_OPEN);
      }

      @Override public void onAnimationCancel(Animator animator) {
      }

      @Override public void onAnimationRepeat(Animator animator) {
      }
    });
    animator.start();
  }

  protected void startLayerTranslation() {
    if (!layerTypeHardware) {
      layerTypeHardware = true;
      contentPane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
      hiddenPane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
    }
  }

  /**
   * If the current layer type is {@link android.view.View#LAYER_TYPE_HARDWARE}, this will set it
   * to
   * {@link View#LAYER_TYPE_NONE}.
   */
  protected void stopLayerTranslation() {
    if (layerTypeHardware) {
      layerTypeHardware = false;
      contentPane.setLayerType(View.LAYER_TYPE_NONE, null);
      hiddenPane.setLayerType(View.LAYER_TYPE_NONE, null);
    }
  }

  public boolean onInterceptTouchEvent(MotionEvent ev) {
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
      activePointerId = INVALID_POINTER;
      isDragging = false;
      if (velocityTracker != null) {
        velocityTracker.recycle();
        velocityTracker = null;
      }

      if (Math.abs(offsetPixels) > hiddenPaneWidth / 2) {
        open();
      } else {
        close();
      }

      return false;
    }

    if (action == MotionEvent.ACTION_DOWN && offsetPixels < 0.0f && isCloseEnough()) {
      setOffsetPixels(0);
      stopAnimation();
      setState(STATE_CLOSED);
      isDragging = false;
    }

    // Always intercept events over the content while menu is visible.
    if (offsetPixels < 0.0f) {
      int index = 0;
      if (activePointerId != INVALID_POINTER) {
        index = ev.findPointerIndex(activePointerId);
        index = index == -1 ? 0 : index;
      }

      final int x = (int) ev.getX(index);
      final int y = (int) ev.getY(index);
      if (isContentTouch(x)) {
        return true;
      }
    }

    if (offsetPixels == 0.0f) {
      return false;
    }

    if (action != MotionEvent.ACTION_DOWN && isDragging) {
      return true;
    }

    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        lastMotionX = initialMotionX = ev.getX();
        lastMotionY = initialMotionY = ev.getY();
        final boolean allowDrag = onDownAllowDrag((int) lastMotionX);
        activePointerId = ev.getPointerId(0);

        if (allowDrag) {
          setState(offsetPixels < 0.0f ? STATE_OPEN : STATE_CLOSED);

          stopAnimation();
          startLayerTranslation();
          isDragging = false;
        }
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        final int activePointerId = this.activePointerId;
        if (activePointerId == INVALID_POINTER) {
          // If we don't have a valid id, the touch down wasn't on content.
          break;
        }

        final int pointerIndex = ev.findPointerIndex(activePointerId);

        final float x = ev.getX(pointerIndex);
        final float dx = x - lastMotionX;
        final float y = ev.getY(pointerIndex);
        final float dy = y - lastMotionY;

        if (checkTouchSlop(dx, dy)) {
          final boolean allowDrag = onMoveAllowDrag((int) x);

          if (allowDrag) {
            startLayerTranslation();
            setState(STATE_DRAGGING);
            isDragging = true;
            lastMotionX = x;
            lastMotionY = y;
          }
        }
        break;
      }

      case MotionEvent.ACTION_POINTER_UP:
        onPointerUp(ev);
        lastMotionX = ev.getX(ev.findPointerIndex(activePointerId));
        lastMotionY = ev.getY(ev.findPointerIndex(activePointerId));
        break;
    }

    if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
    velocityTracker.addMovement(ev);

    return isDragging;
  }

  @Override public boolean onTouchEvent(MotionEvent ev) {
    final int action = ev.getAction() & MotionEvent.ACTION_MASK;

    if (action == MotionEvent.ACTION_DOWN && offsetPixels == 0.0f) {
      return false;
    }

    if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
    velocityTracker.addMovement(ev);

    switch (action) {
      case MotionEvent.ACTION_DOWN: {
        lastMotionX = initialMotionX = ev.getX();
        lastMotionY = initialMotionY = ev.getY();
        final boolean allowDrag = onDownAllowDrag((int) lastMotionX);

        activePointerId = ev.getPointerId(0);

        if (allowDrag) {
          stopAnimation();
          startLayerTranslation();
        }
        break;
      }

      case MotionEvent.ACTION_MOVE: {
        if (!isDragging) {
          final int pointerIndex = ev.findPointerIndex(activePointerId);

          final float x = ev.getX(pointerIndex);
          final float dx = x - lastMotionX;
          final float y = ev.getY(pointerIndex);
          final float dy = y - lastMotionY;

          if (checkTouchSlop(dx, dy)) {
            final boolean allowDrag = onMoveAllowDrag((int) x);

            if (allowDrag) {
              isDragging = true;
              setState(STATE_DRAGGING);
              lastMotionX = x;
              lastMotionY = y;
            } else {
              initialMotionX = x;
              initialMotionY = y;
            }
          }
        }

        if (isDragging) {
          startLayerTranslation();

          final int pointerIndex = ev.findPointerIndex(activePointerId);

          final float x = ev.getX(pointerIndex);
          final float dx = x - lastMotionX;
          final float y = ev.getY(pointerIndex);
          final float dy = y - lastMotionY;

          lastMotionX = x;
          lastMotionY = y;
          onMoveEvent(dx);
        }
        break;
      }

      case MotionEvent.ACTION_UP: {
        final int index = ev.findPointerIndex(activePointerId);
        final int x = (int) ev.getX(index);
        final int offsetPixels = (int) this.offsetPixels;

        if (isDragging) {
          velocityTracker.computeCurrentVelocity(1000, maxVelocity);
          final int initialVelocity = (int) velocityTracker.getXVelocity(activePointerId);
          lastMotionX = x;
          animateOffsetTo(initialVelocity < 0 ? -hiddenPaneWidth : 0, initialVelocity, true);

          // Close the menu when content is clicked while the menu is visible.
        } else if (this.offsetPixels < 0.0f && x < getWidth() + offsetPixels) {
          close();
        }

        activePointerId = INVALID_POINTER;
        isDragging = false;
        break;
      }

      case MotionEvent.ACTION_CANCEL: {
        close();
        activePointerId = INVALID_POINTER;
        isDragging = false;
        break;
      }

      case MotionEvent.ACTION_POINTER_DOWN:
        final int index = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
            >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
        lastMotionX = ev.getX(index);
        lastMotionY = ev.getY(index);
        activePointerId = ev.getPointerId(index);
        break;

      case MotionEvent.ACTION_POINTER_UP:
        onPointerUp(ev);
        lastMotionX = ev.getX(ev.findPointerIndex(activePointerId));
        lastMotionY = ev.getY(ev.findPointerIndex(activePointerId));
        break;
    }

    return true;
  }

  private void onPointerUp(MotionEvent ev) {
    final int pointerIndex = ev.getActionIndex();
    final int pointerId = ev.getPointerId(pointerIndex);
    if (pointerId == activePointerId) {
      final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
      lastMotionX = ev.getX(newPointerIndex);
      activePointerId = ev.getPointerId(newPointerIndex);
      if (velocityTracker != null) {
        velocityTracker.clear();
      }
    }
  }

  @Override protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState state = new SavedState(superState);

    state.open = this.state == STATE_OPENING || this.state == STATE_OPEN;
    return state;
  }

  @Override protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());

    final int hiddenPaneWidth = hiddenPane.getLayoutParams().width;

    setOffsetPixels(savedState.open ? -hiddenPaneWidth : 0);
    setState(savedState.open ? STATE_OPEN : STATE_CLOSED);
  }

  static class SavedState extends BaseSavedState {

    boolean open;

    SavedState(Parcelable superState) {
      super(superState);
    }

    SavedState(Parcel in) {
      super(in);
      open = in.readInt() == 1;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeInt(open ? 1 : 0);
    }

    @SuppressWarnings("UnusedDeclaration") public static final Creator<SavedState> CREATOR =
        new Creator<SavedState>() {
          @Override public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
          }

          @Override public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }
}
