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

import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.WindowInsetsCompat;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.widget.Toolbar;
import android.util.AttributeSet;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.RelativeLayout;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.widget.ObservableScrollView.ScrollListener;

public class AppBarRelativeLayout extends RelativeLayout {

  private static final int SCRIM_ANIMATION_DURATION = 600;

  static final Interpolator FAST_OUT_SLOW_IN_INTERPOLATOR = new FastOutSlowInInterpolator();

  private int contentTopViewId;

  private CollapsingTextHelper textHelper;

  private Toolbar toolbar;
  private View dummyView;

  private View contentView;

  private WindowInsetsCompat lastInsets;
  private int insetsTop;

  private int offset;

  private int expandedMarginLeft;
  private int expandedMarginTop;
  private int expandedMarginBottom;
  private int expandedMarginRight;

  private Drawable contentScrim;
  private Drawable statusBarScrim;

  private Rect dummyBounds = new Rect();

  private Rect collapsedBounds = new Rect();

  private Rect expandedBounds = new Rect();

  private boolean scrimsVisible;

  private ValueAnimator scrimAnimator;
  private int scrimAlpha;

  public AppBarRelativeLayout(Context context) {
    this(context, null);
  }

  public AppBarRelativeLayout(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public AppBarRelativeLayout(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    setWillNotDraw(false);

    textHelper = new CollapsingTextHelper(this);
    textHelper.setExpandedTextGravity(Gravity.LEFT | Gravity.BOTTOM);
    textHelper.setTextSizeInterpolator(new DecelerateInterpolator());

    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.AppBarRelativeLayout, defStyleAttr,
            R.style.AppBarRelativeLayout);

    contentTopViewId =
        a.getResourceId(R.styleable.AppBarRelativeLayout_contentTopViewId, R.id.appBarContent);

    expandedMarginLeft = expandedMarginTop = expandedMarginRight = expandedMarginBottom =
        a.getDimensionPixelSize(R.styleable.AppBarRelativeLayout_expandedTitleMargin, 0);

    if (a.hasValue(R.styleable.AppBarRelativeLayout_expandedTitleMarginStart)) {
      expandedMarginLeft =
          a.getDimensionPixelSize(R.styleable.AppBarRelativeLayout_expandedTitleMarginStart, 0);
    }
    if (a.hasValue(R.styleable.AppBarRelativeLayout_expandedTitleMarginEnd)) {
      expandedMarginRight =
          a.getDimensionPixelSize(R.styleable.AppBarRelativeLayout_expandedTitleMarginEnd, 0);
    }

    if (a.hasValue(R.styleable.AppBarRelativeLayout_expandedTitleMarginTop)) {
      expandedMarginTop =
          a.getDimensionPixelSize(R.styleable.AppBarRelativeLayout_expandedTitleMarginTop, 0);
    }

    if (a.hasValue(R.styleable.AppBarRelativeLayout_expandedTitleMarginBottom)) {
      expandedMarginBottom =
          a.getDimensionPixelSize(R.styleable.AppBarRelativeLayout_expandedTitleMarginBottom, 0);
    }

    final int collapsedTextAppearance =
        a.getResourceId(R.styleable.AppBarRelativeLayout_collapsedTitleTextAppearance,
            R.style.TextAppearance_AppCompat_Widget_ActionBar_Title);
    textHelper.setCollapsedTextAppearance(collapsedTextAppearance);

    final int expandedTextAppearance =
        a.getResourceId(R.styleable.AppBarRelativeLayout_expandedTitleTextAppearance,
            R.style.TextAppearance_AppCompat_Title);
    textHelper.setExpandedTextAppearance(expandedTextAppearance);

    contentScrim = a.getDrawable(R.styleable.AppBarRelativeLayout_contentScrim);
    if (contentScrim != null) {
      contentScrim.setCallback(this);
    }
    statusBarScrim = a.getDrawable(R.styleable.AppBarRelativeLayout_statusBarScrim);
    if (statusBarScrim != null) {
      statusBarScrim.setCallback(this);
    }

    a.recycle();

    ViewCompat.setOnApplyWindowInsetsListener(this,
        new android.support.v4.view.OnApplyWindowInsetsListener() {
          @Override
          public WindowInsetsCompat onApplyWindowInsets(View v, WindowInsetsCompat insets) {
            lastInsets = insets;
            insetsTop = lastInsets.getSystemWindowInsetTop();
            return insets.consumeSystemWindowInsets();
          }
        });
  }

  public void setTitle(CharSequence title) {
    textHelper.setText(title);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    toolbar = (Toolbar) findViewById(R.id.toolbar);
    contentView = findViewById(contentTopViewId);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    ObservableScrollView parent = (ObservableScrollView) getParent();
    parent.addListener(scrollListener);
  }

  private ScrollListener scrollListener = new ScrollListener() {
    @Override public void onScrollChanged(int l, int t) {
      if (t == offset) {
        return;
      }

      int offsetBy = t - offset;
      offset = t;

      final int childCount = getChildCount();
      for (int i = 0; i < childCount; i++) {
        final View child = getChildAt(i);
        LayoutParams params = (LayoutParams) child.getLayoutParams();
        final int scrollMode = params.scrollMode;
        switch (scrollMode) {
          case LayoutParams.SCROLL_MODE_PIN:
            child.setTranslationY(t);
            break;

          case LayoutParams.SCROLL_MODE_PARALLEX:
            final float parallexMultiplier = params.parallexMultiplier;
            child.setTranslationY(t * parallexMultiplier);
            break;

          default:
            child.setTranslationY(0);
            break;
        }
      }

      updateScrimBounds();

      collapsedBounds.offset(0, offsetBy);
      expandedBounds.offset(0, offsetBy);
      textHelper.setCollapsedBounds(collapsedBounds);
      textHelper.setExpandedBounds(expandedBounds);
      textHelper.offsetBounds(0, offsetBy);

      final int toolbarBottom = toolbar.getBottom();
      final int contentTop = contentView.getTop();

      final int expanded = contentTop - toolbarBottom;

      final float fraction = 1.0f * offset / expanded;

      textHelper.setExpansionFraction(fraction);

      final int toolbarHeight = toolbar.getHeight();
      if (contentTop - offset < toolbarBottom + toolbarHeight) {
        showScrims();
      } else {
        hideScrims();
      }

      invalidate();
    }
  };

  private void updateScrimBounds() {
    final int scrimTop = insetsTop + offset;
    int scrimHeight = contentView.getTop() - scrimTop;
    scrimHeight = Math.max(scrimHeight, toolbar.getHeight());
    final int scrimBottom = scrimTop + scrimHeight;

    contentScrim.setBounds(0, scrimTop, getWidth(), scrimBottom);
  }

  private void showScrims() {
    if (!scrimsVisible) {
      if (ViewCompat.isLaidOut(this) && !isInEditMode()) {
        animateScrim(255);
      } else {
        setScrimAlpha(255);
      }

      scrimsVisible = true;
    }
  }

  private void hideScrims() {
    if (scrimsVisible) {
      if (ViewCompat.isLaidOut(this) && !isInEditMode()) {
        animateScrim(0);
      } else {
        setScrimAlpha(0);
      }
      scrimsVisible = false;
    }
  }

  private void animateScrim(int targetAlpha) {
    if (scrimAnimator == null) {
      scrimAnimator = new ValueAnimator();
      scrimAnimator.setDuration(SCRIM_ANIMATION_DURATION);
      scrimAnimator.setInterpolator(FAST_OUT_SLOW_IN_INTERPOLATOR);
      scrimAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
        @Override public void onAnimationUpdate(ValueAnimator animator) {
          setScrimAlpha((Integer) animator.getAnimatedValue());
        }
      });
    } else if (scrimAnimator.isRunning()) {
      scrimAnimator.cancel();
    }

    scrimAnimator.setIntValues(scrimAlpha, targetAlpha);
    scrimAnimator.start();
  }

  private void setScrimAlpha(int alpha) {
    if (alpha != scrimAlpha) {
      final Drawable contentScrim = this.contentScrim;
      if (contentScrim != null && toolbar != null) {
        ViewCompat.postInvalidateOnAnimation(toolbar);
      }
      scrimAlpha = alpha;
      ViewCompat.postInvalidateOnAnimation(AppBarRelativeLayout.this);
    }
  }

  @Override public void draw(Canvas canvas) {
    super.draw(canvas);

    textHelper.draw(canvas);

    if (statusBarScrim != null && scrimAlpha > 0) {
      final int topInset = lastInsets != null ? lastInsets.getSystemWindowInsetTop() : 0;
      if (topInset > 0) {
        statusBarScrim.setBounds(0, offset, getWidth(), topInset + offset);
        statusBarScrim.mutate().setAlpha(scrimAlpha);
        statusBarScrim.draw(canvas);
      }
    }
  }

  @Override protected boolean drawChild(Canvas canvas, View child, long drawingTime) {
    if (child == toolbar) {
      contentScrim.mutate().setAlpha(scrimAlpha);
      contentScrim.draw(canvas);
    } else if (child.getId() == R.id.backdrop) {
      final int save = canvas.save();
      canvas.clipRect(child.getLeft(), child.getTop(), child.getRight(), child.getBottom());
      boolean result = super.drawChild(canvas, child, drawingTime);
      canvas.restoreToCount(save);
      return result;
    }

    return super.drawChild(canvas, child, drawingTime);
  }

  @Override protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    if (toolbar.getTop() < insetsTop) {
      toolbar.offsetTopAndBottom(insetsTop - toolbar.getTop());
    }

    // TODO: Is the design lib way betteR?
    dummyView.getDrawingRect(dummyBounds);
    offsetDescendantRectToMyCoords(dummyView, dummyBounds);

    updateScrimBounds();

    collapsedBounds.left = dummyBounds.left;
    collapsedBounds.top = dummyBounds.top;
    collapsedBounds.right = dummyBounds.right;
    collapsedBounds.bottom = dummyBounds.bottom;
    collapsedBounds.offset(0, offset);
    textHelper.setCollapsedBounds(collapsedBounds);

    expandedBounds.left = left + expandedMarginLeft;
    expandedBounds.top = dummyBounds.bottom + expandedMarginTop;
    expandedBounds.right = (right - left) - expandedMarginRight;
    expandedBounds.bottom = contentView.getTop() - expandedMarginBottom;
    expandedBounds.offset(0, offset);
    textHelper.setExpandedBounds(expandedBounds);

    textHelper.recalculate();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (dummyView == null) {
      dummyView = new View(getContext());
      toolbar.addView(dummyView, FrameLayout.LayoutParams.MATCH_PARENT,
          FrameLayout.LayoutParams.MATCH_PARENT);
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }

  @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  @Override protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(super.generateDefaultLayoutParams());
  }

  @Override public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  public static class LayoutParams extends RelativeLayout.LayoutParams {

    public static final int SCROLL_MODE_NONE = 0;

    public static final int SCROLL_MODE_PIN = 1;

    public static final int SCROLL_MODE_PARALLEX = 2;

    private static final float DEFAULT_PARALLAX_MULTIPLIER = 0.0f;

    int scrollMode = SCROLL_MODE_NONE;

    float parallexMultiplier = DEFAULT_PARALLAX_MULTIPLIER;

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);

      TypedArray a = c.obtainStyledAttributes(attrs, R.styleable.AppBar_LayoutParams);
      scrollMode = a.getInt(R.styleable.AppBar_LayoutParams_layout_scrollMode, SCROLL_MODE_NONE);
      setParallaxMultiplier(a.getFloat(R.styleable.AppBar_LayoutParams_layout_parallexMultiplier,
          DEFAULT_PARALLAX_MULTIPLIER));
      a.recycle();
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    public LayoutParams(ViewGroup.LayoutParams p) {
      super(p);
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }

    public LayoutParams(FrameLayout.LayoutParams source) {
      super(source);
    }

    public void setScrollMode(int scrollMode) {
      this.scrollMode = scrollMode;
    }

    public int getScrollMode() {
      return scrollMode;
    }

    public void setParallaxMultiplier(float multiplier) {
      parallexMultiplier = multiplier;
    }

    public float getParallaxMultiplier() {
      return parallexMultiplier;
    }
  }
}
