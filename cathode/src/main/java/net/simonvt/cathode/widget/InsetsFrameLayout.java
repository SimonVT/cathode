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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import net.simonvt.cathode.R;

public class InsetsFrameLayout extends FrameLayout {

  public static final boolean CATCH_INSETS = CatchSystemInsets.CATCH_INSETS;

  private WindowInsets insets;

  private Rect rect = new Rect();

  private Paint paint = new Paint();

  private int color = 0xFFFF0000;

  public InsetsFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (CATCH_INSETS) {
      final TypedArray a =
          context.obtainStyledAttributes(attrs, R.styleable.InsetsFrameLayout, 0, 0);
      color = a.getColor(R.styleable.InsetsFrameLayout_insetsColor, 0);
      a.recycle();

      setClipToPadding(false);
      setWillNotDraw(false);
      setFitsSystemWindows(true);
      setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
  }

  @Override public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    this.insets = insets;
    return super.onApplyWindowInsets(insets);
  }

  @Override public void draw(Canvas canvas) {
    super.draw(canvas);

    if (CATCH_INSETS) {
      if (insets == null) {
        return;
      }

      final int insetsLeft = insets.getSystemWindowInsetLeft();
      final int insetsTop = insets.getSystemWindowInsetTop();
      final int insetsRight = insets.getSystemWindowInsetRight();
      // final int insetsBottom = insets.getSystemWindowInsetBottom();
      final int insetsBottom = 0;

      paint.setColor(color);

      final int width = getWidth();
      final int height = getHeight();

      rect.set(0, 0, width, insetsTop);
      canvas.drawRect(rect, paint);

      rect.set(0, height - insetsBottom, width, height);
      canvas.drawRect(rect, paint);

      rect.set(0, insetsTop, insetsLeft, height - insetsBottom);
      canvas.drawRect(rect, paint);

      rect.set(width - insetsRight, insetsTop, width, height - insetsBottom);
      canvas.drawRect(rect, paint);
    }
  }
}
