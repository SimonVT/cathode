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

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewPropertyAnimator;

public class ErrorView extends AppCompatTextView {

  private boolean showing;

  private ViewPropertyAnimator animator;

  public ErrorView(Context context) {
    this(context, null);
  }

  public ErrorView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public ErrorView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  @SuppressLint("SetTextI18n") private void init() {
    if (isInEditMode()) {
      setText("This is an error");
    }
  }

  public void show() {
    if (!showing) {
      showing = true;

      if (getVisibility() == View.GONE) {
        setVisibility(View.VISIBLE);
        setTranslationY(getHeight());
      }

      animator = animate().translationY(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          animator = null;
        }
      });
    }
  }

  public void hide() {
    if (showing) {
      showing = false;

      animator = animate().translationY(getHeight()).withEndAction(new Runnable() {
        @Override public void run() {
          animator = null;
        }
      });
    }
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (isInEditMode()) {
      return;
    }

    if (animator != null) {
      animator.cancel();
    }

    if (!showing) {
      setTranslationY(getHeight());
    } else {
      setTranslationY(0);
    }
  }
}
