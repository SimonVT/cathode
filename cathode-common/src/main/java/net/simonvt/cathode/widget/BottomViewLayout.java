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
package net.simonvt.cathode.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;

public class BottomViewLayout extends LinearLayout {

  private View bottomView;

  public BottomViewLayout(Context context) {
    super(context);
  }

  public BottomViewLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public BottomViewLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    setOrientation(VERTICAL);
  }

  @Override public void addView(View child, int index, ViewGroup.LayoutParams params) {
    super.addView(child, index, params);

    if (getChildCount() > 2) {
      throw new RuntimeException("Only two child views allowed in BottomViewLayout");
    }

    if (getChildCount() == 1) {
      LayoutParams p = (LayoutParams) child.getLayoutParams();
      p.width = ViewGroup.LayoutParams.MATCH_PARENT;
      p.height = 0;
      p.weight = 1;
      child.setLayoutParams(p);
    }
  }

  public void setBottomView(View bottomView) {
    if (getChildCount() == 0) {
      throw new RuntimeException("Must add content view first");
    }

    if (this.bottomView != null) {
      removeView(this.bottomView);
    }

    this.bottomView = bottomView;

    if (bottomView != null) {
      addView(bottomView);
    }
  }

  public boolean hasBottomView() {
    return bottomView != null;
  }
}
