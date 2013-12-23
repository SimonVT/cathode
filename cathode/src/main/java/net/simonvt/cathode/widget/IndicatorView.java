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
package net.simonvt.cathode.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;
import net.simonvt.cathode.R;

public class IndicatorView extends View {

  private static final String TAG = "IndicatorView";

  private boolean watched;

  private boolean collected;

  private boolean inWatchlist;

  private int watchedColor;

  private int collectedColor;

  private int watchlistColor;

  private int defaultColor;

  private Paint paint = new Paint();

  public IndicatorView(Context context) {
    super(context);
    init(context);
  }

  public IndicatorView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public IndicatorView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    watchedColor = getResources().getColor(R.color.watchedColor);
    collectedColor = getResources().getColor(R.color.collectedColor);
    watchlistColor = getResources().getColor(R.color.watchlistColor);
    defaultColor = getResources().getColor(R.color.defaultColor);

    if (isInEditMode()) {
      watched = true;
      collected = true;
    }
  }

  public void setWatched(boolean watched) {
    this.watched = watched;
  }

  public void setCollected(boolean collected) {
    this.collected = collected;
  }

  public void setInWatchlist(boolean inWatchlist) {
    this.inWatchlist = inWatchlist;
  }

  @Override protected void onDraw(Canvas canvas) {
    final int width = getWidth();
    final int height = getHeight();

    if (width > height) {
      final int halfWidth = width / 2;

      if (collected) {
        paint.setColor(collectedColor);
      } else {
        paint.setColor(defaultColor);
      }
      canvas.drawRect(0, 0, halfWidth, height, paint);

      if (watched) {
        paint.setColor(watchedColor);
      } else if (inWatchlist) {
        paint.setColor(watchlistColor);
      } else {
        paint.setColor(defaultColor);
      }
      canvas.drawRect(width - halfWidth, 0, width, height, paint);
    } else {
      final int halfHeight = height / 2;

      if (collected) {
        paint.setColor(collectedColor);
      } else {
        paint.setColor(defaultColor);
      }
      canvas.drawRect(0, 0, width, halfHeight, paint);

      if (watched) {
        paint.setColor(watchedColor);
      } else if (inWatchlist) {
        paint.setColor(watchlistColor);
      } else {
        paint.setColor(defaultColor);
      }
      canvas.drawRect(0, height - halfHeight, width, height, paint);
    }
  }
}
