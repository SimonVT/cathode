/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.ObservableScrollView.ScrollListener;

public class AppBarScrollParent extends FrameLayout {

  private ObservableScrollView scrollView;

  public AppBarScrollParent(Context context) {
    super(context);
  }

  public AppBarScrollParent(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public AppBarScrollParent(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();

    scrollView = findScrollView(this);

    scrollView.addListener(scrollListener);
  }

  private ObservableScrollView findScrollView(ViewGroup viewGroup) {
    for (int i = 0, childCount = viewGroup.getChildCount(); i < childCount; i++) {
      final View child = viewGroup.getChildAt(i);
      if (child instanceof ObservableScrollView) {
        return (ObservableScrollView) child;
      }

      if (child instanceof ViewGroup) {
        return findScrollView((ViewGroup) child);
      }
    }

    return null;
  }

  private ScrollListener scrollListener = new ScrollListener() {
    @Override public void onScrollChanged(int l, int t) {
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
            child.setTranslationY(-t * parallexMultiplier);
            break;

          default:
            child.setTranslationY(0);
            break;
        }
      }
    }
  };

  @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  @Override protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(super.generateDefaultLayoutParams());
  }

  @Override public FrameLayout.LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override protected FrameLayout.LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  public static class LayoutParams extends FrameLayout.LayoutParams {

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

    public LayoutParams(int width, int height, int gravity) {
      super(width, height, gravity);
    }

    public LayoutParams(ViewGroup.LayoutParams p) {
      super(p);
    }

    public LayoutParams(MarginLayoutParams source) {
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
