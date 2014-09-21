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

import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class ScalingItemAnimator extends ExtensibleItemAnimator {

  @Override public void beforeAddAnimation(View view) {
    view.setAlpha(0.0f);
    view.setScaleX(0.5f);
    view.setScaleY(0.5f);
  }

  @Override public void setupAddAnimation(ViewPropertyAnimatorCompat animator) {
    animator.alpha(1.0f).scaleX(1.0f).scaleY(1.0f);
  }

  @Override public void onAddAnimationEnd(View view) {
  }

  @Override public void onResetAddAnimation(View view) {
    view.setAlpha(1.0f);
    view.setScaleX(1.0f);
    view.setScaleY(1.0f);
  }

  @Override public void beforeRemoveAnimation(View view) {
  }

  @Override public void setupRemoveAnimation(ViewPropertyAnimatorCompat animator) {
    animator.alpha(0.0f).scaleX(0.5f).scaleY(0.5f);
  }

  @Override public void onRemoveAnimationEnd(View view) {
    view.setAlpha(1.0f);
    view.setScaleX(1.0f);
    view.setScaleY(1.0f);
  }

  @Override public void onResetRemoveAnimation(View view) {
    view.setAlpha(1.0f);
    view.setScaleX(1.0f);
    view.setScaleY(1.0f);
  }

  @Override public void beforeMoveAnimation(View view, int fromX, int fromY, int toX, int toY) {
    view.setTranslationX(fromX - toX);
    view.setTranslationY(fromY - toY);
  }

  @Override public void setupMoveAnimation(ViewPropertyAnimatorCompat animator, int fromX,
      int fromY, int toX, int toY) {
    animator.translationX(0.0f).translationY(0.0f);
  }

  @Override public void onMoveAnimationEnd(View view) {
  }

  @Override public void onResetMoveAnimation(View view) {
    view.setTranslationX(0.0f);
    view.setTranslationY(0.0f);
  }

  @Override public boolean animateChange(RecyclerView.ViewHolder oldHolder, RecyclerView.ViewHolder newHolder, int fromLeft, int fromTop, int toLeft, int toTop) {
    // TODO:
    return false;
  }
}
