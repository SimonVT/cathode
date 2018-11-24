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
package net.simonvt.cathode.common.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;
import androidx.annotation.NonNull;

public class ForegroundDrawableRelativeLayout extends RelativeLayout {

  private static final int[] ATTRS = new int[] {
      android.R.attr.foreground,
  };

  private static final int INDEX_FOREGROUND = 0;

  private Drawable foreground;
  private boolean foregroundBoundsChanged;

  public ForegroundDrawableRelativeLayout(Context context) {
    super(context);
  }

  public ForegroundDrawableRelativeLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs);
  }

  public ForegroundDrawableRelativeLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs);
  }

  private void init(Context context, AttributeSet attrs) {

    TypedArray a = context.obtainStyledAttributes(attrs, ATTRS, 0, 0);

    final Drawable d = a.getDrawable(INDEX_FOREGROUND);
    if (d != null) {
      setForeground(d);
    }

    a.recycle();
  }

  public void setForeground(Drawable drawable) {
    if (foreground != drawable) {
      if (foreground != null) {
        foreground.setCallback(null);
        unscheduleDrawable(foreground);
      }

      foreground = drawable;

      if (drawable != null) {
        setWillNotDraw(false);
        drawable.setCallback(this);
        if (drawable.isStateful()) {
          drawable.setState(getDrawableState());
        }
      } else {
        setWillNotDraw(true);
      }
      requestLayout();
      invalidate();
    }
  }

  @Override protected boolean verifyDrawable(@NonNull Drawable who) {
    return super.verifyDrawable(who) || (who == foreground);
  }

  @Override public void jumpDrawablesToCurrentState() {
    super.jumpDrawablesToCurrentState();
    if (foreground != null) foreground.jumpToCurrentState();
  }

  @Override protected void drawableStateChanged() {
    super.drawableStateChanged();
    if (foreground != null && foreground.isStateful()) {
      foreground.setState(getDrawableState());
    }
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);
    foregroundBoundsChanged = true;
  }

  @Override public void draw(Canvas canvas) {
    super.draw(canvas);

    if (foreground != null) {
      final Drawable foreground = this.foreground;

      if (foregroundBoundsChanged) {
        foregroundBoundsChanged = false;
        foreground.setBounds(0, 0, getWidth(), getHeight());
      }

      foreground.draw(canvas);
    }
  }

  @Override public boolean gatherTransparentRegion(Region region) {
    boolean opaque = super.gatherTransparentRegion(region);
    if (region != null && foreground != null) {
      applyDrawableToTransparentRegion(foreground, region);
    }
    return opaque;
  }

  public void applyDrawableToTransparentRegion(Drawable dr, Region region) {
    final Region r = dr.getTransparentRegion();
    final Rect db = dr.getBounds();
    if (r != null) {
      final int[] location = new int[2];
      getLocationInWindow(location);
      r.translate(location[0], location[1]);
      region.op(r, Region.Op.INTERSECT);
    } else {
      region.op(db, Region.Op.DIFFERENCE);
    }
  }
}
