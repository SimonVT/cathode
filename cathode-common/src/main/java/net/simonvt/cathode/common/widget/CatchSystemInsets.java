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
import android.util.AttributeSet;
import android.view.View;
import android.view.WindowInsets;
import android.widget.FrameLayout;

public class CatchSystemInsets extends FrameLayout {

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
    setFitsSystemWindows(true);
    setSystemUiVisibility(SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
  }

  @Override public WindowInsets onApplyWindowInsets(WindowInsets insets) {
    this.insets = new WindowInsets(insets);
    return insets.consumeSystemWindowInsets();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    if (insets != null) {
      for (int i = 0, childCount = getChildCount(); i < childCount; i++) {
        View child = getChildAt(i);
        child.dispatchApplyWindowInsets(insets);
      }
    }

    super.onMeasure(widthMeasureSpec, heightMeasureSpec);
  }
}
