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

package net.simonvt.cathode.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.view.View;
import net.simonvt.cathode.common.R;

public abstract class AspectRatioView extends View {

  private static final int MEASUREMENT_HEIGHT = 0;
  private static final int MEASUREMENT_WIDTH = 1;

  private float aspectRatio = 0.0f;

  private int dominantMeasurement = MEASUREMENT_HEIGHT;

  public AspectRatioView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.AspectRatioView);

    aspectRatio = a.getFloat(R.styleable.AspectRatioView_aspectRatio, 0.0f);
    dominantMeasurement =
        a.getInt(R.styleable.AspectRatioView_dominantMeasurement, MEASUREMENT_HEIGHT);

    a.recycle();
  }

  public void setAspectRatio(float aspectRatio) {
    this.aspectRatio = aspectRatio;
    requestLayout();
    invalidate();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      if (aspectRatio != 0.0f) {
        switch (dominantMeasurement) {
          case MEASUREMENT_HEIGHT:
            width = (int) ((height - getPaddingTop() - getPaddingBottom()) * aspectRatio)
                + getPaddingLeft()
                + getPaddingRight();
            break;

          case MEASUREMENT_WIDTH:
            height = (int) ((width - getPaddingLeft() - getPaddingRight()) * aspectRatio)
                + getPaddingTop()
                + getPaddingBottom();
            break;
        }
      }

      setMeasuredDimension(width, height);
    }
  }
}
