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
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class EpisodeTopLayout extends ViewGroup {

  private View info;
  private View rating;

  public EpisodeTopLayout(Context context) {
    super(context);
  }

  public EpisodeTopLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public EpisodeTopLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override protected void onFinishInflate() {
    info = getChildAt(0);
    rating = getChildAt(1);
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int width = r - l;
    final int height = b - t;

    MarginLayoutParams infoParams = (MarginLayoutParams) info.getLayoutParams();
    final int infoLeft = getPaddingLeft() + infoParams.leftMargin;
    final int infoWidth = info.getMeasuredWidth();
    final int infoHeight = info.getMeasuredHeight();
    final int infoTop =
        (height - getPaddingTop()) / 2 - infoHeight / 2 + getPaddingTop() + infoParams.topMargin;
    info.layout(infoLeft, infoTop, infoLeft + infoWidth, infoTop + infoHeight);

    MarginLayoutParams ratingParams = (MarginLayoutParams) rating.getLayoutParams();
    final int ratingRight = width - getPaddingRight() - ratingParams.rightMargin;
    final int ratingWidth = rating.getMeasuredWidth();
    final int ratingHeight = rating.getMeasuredHeight();
    final int ratingTop = getPaddingTop() + ratingParams.topMargin;
    rating.layout(ratingRight - ratingWidth, ratingTop, ratingRight, ratingTop + ratingHeight);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthSize = MeasureSpec.getSize(widthMeasureSpec);
    final int minHeight = getMinimumHeight();

    MarginLayoutParams infoParams = (MarginLayoutParams) info.getLayoutParams();
    MarginLayoutParams ratingParams = (MarginLayoutParams) rating.getLayoutParams();

    measureChildWithMargins(info, widthMeasureSpec, 0, heightMeasureSpec, 0);

    final int infoHeight = info.getMeasuredHeight();
    int height = infoHeight
        + getPaddingTop()
        + getPaddingBottom()
        + infoParams.topMargin
        + infoParams.bottomMargin;
    height = Math.max(height, minHeight);

    final int ratingHeight = height
        - getPaddingTop()
        - getPaddingBottom()
        - ratingParams.topMargin
        - ratingParams.bottomMargin;
    final int ratingHeightSpec = MeasureSpec.makeMeasureSpec(ratingHeight, MeasureSpec.EXACTLY);
    final int ratingWidthSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);

    rating.measure(ratingWidthSpec, ratingHeightSpec);

    final int infoWidth = widthSize
        - getPaddingLeft()
        - getPaddingRight()
        - infoParams.leftMargin
        - infoParams.rightMargin
        - rating.getMeasuredWidth()
        - ratingParams.leftMargin
        - ratingParams.rightMargin;

    final int infoWidthSpec = MeasureSpec.makeMeasureSpec(infoWidth, MeasureSpec.EXACTLY);
    final int infoHeightSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    info.measure(infoWidthSpec, infoHeightSpec);

    setMeasuredDimension(widthSize, height);
  }

  @Override protected boolean checkLayoutParams(LayoutParams p) {
    return p instanceof MarginLayoutParams;
  }

  @Override public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new MarginLayoutParams(getContext(), attrs);
  }

  @Override protected LayoutParams generateLayoutParams(LayoutParams p) {
    return new MarginLayoutParams(p);
  }

  @Override protected LayoutParams generateDefaultLayoutParams() {
    return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  }
}
