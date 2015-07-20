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

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import butterknife.Bind;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;

public class PhoneShowView extends ViewGroup {

  private int minHeight;

  @Bind(R.id.infoParent) View infoParent;
  @Bind(R.id.overflow) OverflowView overflow;
  @Bind(R.id.poster) RemoteImageView poster;

  public PhoneShowView(Context context) {
    super(context);
    init();
  }

  public PhoneShowView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public PhoneShowView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init();
  }

  private void init() {
    minHeight = getResources().getDimensionPixelSize(R.dimen.showItemMinHeight);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int paddingLeft = getPaddingLeft();
    final int paddingTop = getPaddingTop();

    final int width = r - l;

    final LayoutParams posterLp = (LayoutParams) poster.getLayoutParams();
    final int posterWidth = poster.getMeasuredWidth();
    final int posterHeight = poster.getMeasuredHeight();

    final int posterLeft = paddingLeft + posterLp.leftMargin;
    final int posterTop = paddingTop + posterLp.topMargin;
    poster.layout(posterLeft, posterTop, posterLeft + posterWidth, posterTop + posterHeight);

    final LayoutParams infoLp = (LayoutParams) infoParent.getLayoutParams();

    final int infoWidth = infoParent.getMeasuredWidth();
    final int infoHeight = infoParent.getMeasuredHeight();
    final int infoLeft = posterLeft + posterWidth + posterLp.rightMargin + infoLp.leftMargin;
    final int infoTop = paddingTop + infoLp.topMargin;

    infoParent.layout(infoLeft, infoTop, infoLeft + infoWidth, infoTop + infoHeight);

    final int overflowWidth = overflow.getMeasuredWidth();
    final int overflowHeight = overflow.getMeasuredHeight();
    final int overflowTop = getPaddingTop();
    final int overflowRight = width;
    overflow.layout(overflowRight - overflowWidth, overflowTop, overflowRight,
        overflowTop + overflowHeight);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int mode = MeasureSpec.getMode(widthMeasureSpec);
    if (mode != MeasureSpec.EXACTLY) {
      throw new RuntimeException("PhoneShowView width must measure as EXACTLY.");
    }

    LayoutParams posterLp = (LayoutParams) poster.getLayoutParams();
    LayoutParams infoParentLp = (LayoutParams) infoParent.getLayoutParams();

    // Measure the height of show and next episode info
    measureChild(infoParent, widthMeasureSpec, heightMeasureSpec);
    final int infoParentHeight =
        infoParent.getMeasuredHeight() + infoParentLp.topMargin + infoParentLp.bottomMargin;

    final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);

    final int paddingLeft = getPaddingLeft();
    final int paddingTop = getPaddingTop();
    final int paddingRight = getPaddingRight();
    final int paddingBottom = getPaddingBottom();

    int leftoverWidth = viewWidth - paddingLeft - paddingRight;

    final int viewHeight = Math.max(infoParentHeight + paddingTop + paddingBottom, minHeight);

    final int osterHeight =
        viewHeight - paddingTop - paddingBottom - posterLp.topMargin - posterLp.bottomMargin;
    final int posterWidthMeasureSpec =
        MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
    final int posterHeightMeasureSpec =
        MeasureSpec.makeMeasureSpec(osterHeight, MeasureSpec.EXACTLY);
    poster.measure(posterWidthMeasureSpec, posterHeightMeasureSpec);

    leftoverWidth -= poster.getMeasuredWidth() - posterLp.leftMargin - posterLp.rightMargin;

    final int infoParentWidth = leftoverWidth - infoParentLp.leftMargin - infoParentLp.rightMargin;
    final int infoParentWidthMeasureSpec =
        MeasureSpec.makeMeasureSpec(infoParentWidth, MeasureSpec.EXACTLY);
    final int infoParentHeightMeasureSpec = MeasureSpec.makeMeasureSpec(viewHeight
        - paddingTop
        - paddingBottom
        - infoParentLp.topMargin
        - infoParentLp.bottomMargin, MeasureSpec.EXACTLY);
    infoParent.measure(infoParentWidthMeasureSpec, infoParentHeightMeasureSpec);

    setMeasuredDimension(viewWidth, viewHeight);

    measureChild(overflow, widthMeasureSpec, heightMeasureSpec);
  }

  @Override public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  }

  @Override protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  public static class LayoutParams extends MarginLayoutParams {

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);
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
  }
}
