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
import android.util.DisplayMetrics;
import android.widget.FrameLayout;

public class HalfScreenHeightFrameLayout extends FrameLayout {

  private DisplayMetrics displayMetrics;

  public HalfScreenHeightFrameLayout(Context context) {
    super(context);
  }

  public HalfScreenHeightFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public HalfScreenHeightFrameLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    displayMetrics = getResources().getDisplayMetrics();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int height = displayMetrics.heightPixels;
    heightMeasureSpec = MeasureSpec.makeMeasureSpec(height / 2, MeasureSpec.EXACTLY);
    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}
