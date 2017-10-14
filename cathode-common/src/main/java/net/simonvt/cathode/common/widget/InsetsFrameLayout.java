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
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.util.AttributeSet;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import net.simonvt.cathode.common.R;

public class InsetsFrameLayout extends FrameLayout {

  public static final boolean CATCH_INSETS = CatchSystemInsets.CATCH_INSETS;

  private WindowInsets insets;

  private Rect rect = new Rect();

  private Paint paint = new Paint();

  private int color;

  public InsetsFrameLayout(Context context, AttributeSet attrs) {
    super(context, attrs);

    if (CATCH_INSETS) {
      final TypedArray a =
          context.obtainStyledAttributes(attrs, R.styleable.InsetsFrameLayout, 0, 0);
      color = a.getColor(R.styleable.InsetsFrameLayout_insetsColor, 0);
      a.recycle();

      paint.setColor(color);

      setClipToPadding(false);
      setWillNotDraw(false);
      setFitsSystemWindows(true);
      setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP) @Override
  public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    this.insets = new WindowInsets(insets);

    setPadding(0, insets.getSystemWindowInsetTop(), 0, 0);

    return insets.consumeSystemWindowInsets();
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP) private int getSystemWindowInsetsTop() {
    if (CATCH_INSETS && insets != null) {
      return insets.getSystemWindowInsetTop();
    }

    return 0;
  }

  @Override public void draw(Canvas canvas) {
    super.draw(canvas);

    if (CATCH_INSETS) {
      if (insets == null) {
        return;
      }

      final int insetsTop = getSystemWindowInsetsTop();
      final int width = getWidth();

      rect.set(0, 0, width, insetsTop);
      canvas.drawRect(rect, paint);
    }
  }
}
