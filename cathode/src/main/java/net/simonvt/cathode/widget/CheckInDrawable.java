/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Rect;
import android.graphics.drawable.Animatable2;
import android.graphics.drawable.AnimatedVectorDrawable;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.graphics.drawable.VectorDrawableCompat;
import android.support.v7.widget.RecyclerView;
import net.simonvt.cathode.R;

public class CheckInDrawable extends Drawable implements Drawable.Callback {

  private Context context;

  private long id = RecyclerView.NO_ID;
  private boolean watching;

  private Drawable checkInDrawable;
  private Drawable cancelDrawable;

  private Drawable currentDrawable;

  private Animatable2.AnimationCallback callbacks;

  public CheckInDrawable(Context context) {
    this(context, R.drawable.ic_anim_checkin_24dp, R.drawable.ic_anim_cancel_24dp,
        R.drawable.ic_action_checkin_24dp, R.drawable.ic_action_cancel_24dp);
  }

  public CheckInDrawable(Context context, int animatedCheckInDrawableRes,
      int animatedCancelDrawableRes, int checkInDrawableRes, int cancelDrawableRes) {
    this.context = context;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      checkInDrawable = context.getDrawable(animatedCheckInDrawableRes);
      cancelDrawable = context.getDrawable(animatedCancelDrawableRes);

      callbacks = new Animatable2.AnimationCallback() {
        @RequiresApi(api = Build.VERSION_CODES.M) @Override public void onAnimationEnd(Drawable drawable) {
          ((AnimatedVectorDrawable) checkInDrawable).clearAnimationCallbacks();
          ((AnimatedVectorDrawable) checkInDrawable).reset();
          ((AnimatedVectorDrawable) cancelDrawable).clearAnimationCallbacks();
          ((AnimatedVectorDrawable) cancelDrawable).reset();

          updateWatchingDrawable(watching);
        }
      };
    } else {
      checkInDrawable =
          VectorDrawableCompat.create(context.getResources(), checkInDrawableRes, null);
      cancelDrawable = VectorDrawableCompat.create(context.getResources(), cancelDrawableRes, null);
    }

    checkInDrawable.setCallback(this);
    cancelDrawable.setCallback(this);

    currentDrawable = checkInDrawable;
  }

  public void reset() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      resetImageDrawables();

      id = RecyclerView.NO_ID;
      watching = false;

      currentDrawable = checkInDrawable;
      invalidateSelf();
    }
  }

  private void resetImageDrawables() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      ((AnimatedVectorDrawable) checkInDrawable).clearAnimationCallbacks();
      ((AnimatedVectorDrawable) cancelDrawable).clearAnimationCallbacks();
      ((AnimatedVectorDrawable) checkInDrawable).stop();
      ((AnimatedVectorDrawable) cancelDrawable).stop();
      ((AnimatedVectorDrawable) checkInDrawable).reset();
      ((AnimatedVectorDrawable) cancelDrawable).reset();
    }
  }

  public void setId(long id) {
    this.id = id;
  }

  public void setWatching(boolean watching) {
    if (watching != this.watching) {
      if (id == RecyclerView.NO_ID || Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
        updateWatchingDrawable(watching);
      } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        AnimatedVectorDrawable d = (AnimatedVectorDrawable) currentDrawable;

        if (d.isRunning()) {
          d.clearAnimationCallbacks();
          d.stop();
          d.reset();

          updateWatchingDrawable(this.watching);
          d = (AnimatedVectorDrawable) currentDrawable;
        }

        d.registerAnimationCallback(callbacks);
        d.start();
      }

      this.watching = watching;
      invalidateSelf();
    }
  }

  private void updateWatchingDrawable(boolean watching) {
    resetImageDrawables();

    if (watching) {
      currentDrawable = cancelDrawable;
    } else {
      currentDrawable = checkInDrawable;
    }

    invalidateSelf();
  }

  @Override public int getIntrinsicWidth() {
    return checkInDrawable.getIntrinsicWidth();
  }

  @Override public int getIntrinsicHeight() {
    return checkInDrawable.getIntrinsicHeight();
  }

  @Override protected void onBoundsChange(Rect bounds) {
    super.onBoundsChange(bounds);
    checkInDrawable.setBounds(bounds);
    cancelDrawable.setBounds(bounds);
  }

  @Override public void draw(Canvas canvas) {
    if (currentDrawable != null) {
      currentDrawable.draw(canvas);
    }
  }

  @Override public void setAlpha(int alpha) {
    checkInDrawable.setAlpha(alpha);
    cancelDrawable.setAlpha(alpha);
    invalidateSelf();
  }

  @Override public void setColorFilter(ColorFilter colorFilter) {
    checkInDrawable.setColorFilter(colorFilter);
    cancelDrawable.setColorFilter(colorFilter);
    invalidateSelf();
  }

  @Override public int getOpacity() {
    return checkInDrawable.getOpacity();
  }

  @Override public void invalidateDrawable(Drawable who) {
    invalidateSelf();
  }

  @Override public void scheduleDrawable(Drawable who, Runnable what, long when) {
    scheduleSelf(what, when);
  }

  @Override public void unscheduleDrawable(Drawable who, Runnable what) {
    unscheduleSelf(what);
  }
}
