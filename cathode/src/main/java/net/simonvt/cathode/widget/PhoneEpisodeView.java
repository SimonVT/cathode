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
import android.view.ViewGroup;
import android.widget.TextView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.common.widget.RemoteImageView;

public class PhoneEpisodeView extends ViewGroup {

  private static final float SCREEN_RATIO = 680.f / 1000.f;

  private RemoteImageView poster;

  private TextView number;

  private TextView title;

  private TextView firstAired;

  private int minHeight;

  public PhoneEpisodeView(Context context) {
    super(context);
    init(context);
  }

  public PhoneEpisodeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public PhoneEpisodeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    minHeight = getResources().getDimensionPixelSize(R.dimen.showItemMinHeight);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    poster = findViewById(R.id.screen);
    number = findViewById(R.id.number);
    title = findViewById(R.id.title);
    firstAired = findViewById(R.id.firstAired);
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int width = r - l;
    final int height = b - t;

    final int paddingStart = getPaddingStart();
    final int paddingTop = getPaddingTop();
    final int paddingEnd = getPaddingEnd();
    final int paddingBottom = getPaddingBottom();

    LayoutParams posterLp = (LayoutParams) poster.getLayoutParams();
    LayoutParams numberLp = (LayoutParams) number.getLayoutParams();
    LayoutParams titleLp = (LayoutParams) title.getLayoutParams();
    LayoutParams firstAiredLp = (LayoutParams) firstAired.getLayoutParams();

    final int posterLeft = paddingStart + posterLp.leftMargin;
    final int posterRight = posterLeft + poster.getMeasuredWidth();
    final int posterTop = paddingTop + posterLp.topMargin;
    final int posterBottom = posterTop + poster.getMeasuredHeight();
    poster.layout(posterLeft, posterTop, posterRight, posterBottom);

    final int numberRight = width - paddingEnd - numberLp.rightMargin;
    final int numberLeft = numberRight - number.getMeasuredWidth();
    final int numberTop = paddingTop + numberLp.topMargin;
    final int numberBottom = numberTop + number.getMeasuredHeight();
    number.layout(numberLeft, numberTop, numberRight, numberBottom);

    final int infoHeight = titleLp.topMargin
        + title.getMeasuredHeight()
        + titleLp.bottomMargin
        + firstAiredLp.topMargin
        + firstAired.getMeasuredHeight()
        + firstAiredLp.bottomMargin;
    final int infoOffset = (height - paddingTop - paddingBottom - infoHeight) / 2;

    final int titleLeft = posterRight + posterLp.rightMargin + titleLp.leftMargin;
    final int titleRight = numberLeft - numberLp.leftMargin - titleLp.rightMargin;
    final int titleTop = paddingTop + infoOffset + titleLp.topMargin;
    final int titleBottom = titleTop + title.getMeasuredHeight();
    title.layout(titleLeft, titleTop, titleRight, titleBottom);

    final int firstAiredTop = titleBottom + titleLp.bottomMargin + firstAiredLp.topMargin;
    final int firstAiredBottom = firstAiredTop + firstAired.getMeasuredHeight();
    final int firstAiredLeft = posterRight + posterLp.rightMargin + firstAiredLp.leftMargin;
    final int firstAiredRight = firstAiredLeft + firstAired.getMeasuredWidth();
    firstAired.layout(firstAiredLeft, firstAiredTop, firstAiredRight, firstAiredBottom);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int mode = MeasureSpec.getMode(widthMeasureSpec);
    if (mode != MeasureSpec.EXACTLY) {
      throw new RuntimeException("PhoneShowView width must measure as EXACTLY.");
    }

    final int width = MeasureSpec.getSize(widthMeasureSpec);

    LayoutParams posterLp = (LayoutParams) poster.getLayoutParams();
    LayoutParams numberLp = (LayoutParams) number.getLayoutParams();
    LayoutParams titleLp = (LayoutParams) title.getLayoutParams();
    LayoutParams firstAiredLp = (LayoutParams) firstAired.getLayoutParams();

    // Get width of number
    measureChild(number, widthMeasureSpec, heightMeasureSpec);
    final int numberWidth = number.getMeasuredWidth();

    // Get height of title and timestamp
    measureChild(title, widthMeasureSpec, heightMeasureSpec);
    measureChild(firstAired, widthMeasureSpec, heightMeasureSpec);

    int height = getPaddingTop()
        + titleLp.topMargin
        + title.getMeasuredHeight()
        + titleLp.bottomMargin
        + firstAiredLp.topMargin
        + firstAired.getMeasuredHeight()
        + firstAiredLp.bottomMargin
        + getPaddingBottom();
    height = Math.max(height, minHeight);

    // Measure poster
    final int posterWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    final int posterHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
    poster.measure(posterWidthMeasureSpec, posterHeightMeasureSpec);
    final int posterWidth = poster.getMeasuredWidth();

    // Measure number number
    final int numberWidthMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    final int numberHeightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
    number.measure(posterWidthMeasureSpec, posterHeightMeasureSpec);

    // Measure title and timestamp
    final int leftoverWidth = width
        - getPaddingStart()
        - posterLp.leftMargin
        - posterWidth
        - posterLp.rightMargin
        - numberLp.leftMargin
        - numberWidth
        - numberLp.rightMargin
        - getPaddingEnd();

    final int titleMaxWidth = leftoverWidth - titleLp.leftMargin - titleLp.rightMargin;
    final int titleWidthMeasureSpec =
        MeasureSpec.makeMeasureSpec(titleMaxWidth, MeasureSpec.EXACTLY);
    final int titleHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    title.measure(titleWidthMeasureSpec, titleHeightMeasureSpec);

    final int firstAiredMaxWidth =
        leftoverWidth - firstAiredLp.leftMargin - firstAiredLp.rightMargin;
    final int firstAiredWidthMeasureSpec =
        MeasureSpec.makeMeasureSpec(firstAiredMaxWidth, MeasureSpec.AT_MOST);
    final int firstAiredHeightMeasureSpec = MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED);
    title.measure(titleWidthMeasureSpec, titleHeightMeasureSpec);

    setMeasuredDimension(width, height);
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
