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

package net.simonvt.cathode.common.widget;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;
import androidx.annotation.RequiresApi;

public class CatchSystemInsets extends FrameLayout {

  public static final boolean CATCH_INSETS = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

  WindowInsets insets;

  public CatchSystemInsets(Context context) {
    super(context);
    init();
  }

  public CatchSystemInsets(Context context, AttributeSet attrs) {
    super(context, attrs);
    init();
  }

  public CatchSystemInsets(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  private void init() {
    if (CATCH_INSETS) {
      setFitsSystemWindows(true);
      setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP) @Override
  public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    if (CATCH_INSETS) {
      this.insets = new WindowInsets(insets);
      return insets.consumeSystemWindowInsets();
    }

    return super.onApplyWindowInsets(insets);
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP) @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (CATCH_INSETS) {
      if (insets != null) {
        for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
          View child = getChildAt(i);
          child.dispatchApplyWindowInsets(insets);
        }
      }
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}
