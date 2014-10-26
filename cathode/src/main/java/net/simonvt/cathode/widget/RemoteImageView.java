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
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import timber.log.Timber;

/**
 * Simple View used to display an image from a remote source. An URL to an image is passed to
 * {@link #setImage(String)}, and the view then takes care of loading and displaying it.
 * <p/>
 * The view must either have a fixed width or a fixed width. Both can also be set. If only one
 * dimension is fixed,
 * {@link #setAspectRatio(float)} must be called. The non-fixed dimension will then be calculated
 * by
 * multiplying the
 * fixed dimension with this value.
 */
public class RemoteImageView extends View implements Target {

  private static final float ANIMATION_DURATION = 300.0f;

  private static final int MEASUREMENT_HEIGHT = 0;

  private static final int MEASUREMENT_WIDTH = 1;

  @Inject Picasso picasso;

  private Drawable placeHolder;

  private Bitmap image;

  private boolean animating;

  private long startTimeMillis;

  private int alpha;

  private Paint paint = new Paint();

  private String imageUrl;

  private float aspectRatio = 0.0f;

  private int dominantMeasurement = MEASUREMENT_HEIGHT;

  public RemoteImageView(Context context) {
    this(context, null);
  }

  public RemoteImageView(Context context, AttributeSet attrs) {
    this(context, attrs, 0);
  }

  public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    if (!isInEditMode()) {
      CathodeApp.inject(context, this);
    }

    TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RemoteImageView);

    placeHolder = a.getDrawable(R.styleable.RemoteImageView_placeholder);
    if (placeHolder == null) {
      placeHolder = getResources().getDrawable(R.drawable.placeholder);
    }
    aspectRatio = a.getFloat(R.styleable.RemoteImageView_aspectRatio, 0.0f);
    dominantMeasurement =
        a.getInt(R.styleable.RemoteImageView_dominantMeasurement, MEASUREMENT_HEIGHT);

    a.recycle();
  }

  public void setImage(String imageUrl) {
    picasso.cancelRequest(this);

    this.imageUrl = imageUrl;
    image = null;
    alpha = 0;
    startTimeMillis = 0;
    animating = false;

    if (getWidth() - getPaddingLeft() - getPaddingRight() > 0
        && getHeight() - getPaddingTop() - getPaddingBottom() > 0) {
      loadBitmap();
    }

    invalidate();
  }

  private void loadBitmap() {
    alpha = 0;
    image = null;
    final int width = getWidth() - getPaddingLeft() - getPaddingRight();
    final int height = getHeight() - getPaddingTop() - getPaddingBottom();
    picasso.load(imageUrl).resize(width, height).centerCrop().into(this);
    if (image != null) {
      animating = false;
      startTimeMillis = 0;
      alpha = 0xFF;
    }
  }

  @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
    image = bitmap;
    animating = true;
    startTimeMillis = 0;
    alpha = 0;
    invalidate();
  }

  @Override public void onBitmapFailed(Drawable drawable) {
    Timber.d("onBitmapFailed");
  }

  @Override public void onPrepareLoad(Drawable drawable) {
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (imageUrl != null
        && w - getPaddingLeft() - getPaddingRight() > 0
        && h - getPaddingTop() - getPaddingBottom() > 0) {
      loadBitmap();
    }
  }

  @Override protected void onDraw(Canvas canvas) {
    final int width = getWidth();
    final int height = getHeight();

    if (image == null) {
      placeHolder.setBounds(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(),
          height - getPaddingBottom());
      placeHolder.setAlpha(0xFF);
      placeHolder.draw(canvas);
      return;
    }

    boolean done = true;

    if (animating) {
      if (startTimeMillis == 0) {
        startTimeMillis = SystemClock.uptimeMillis();
        done = false;
        alpha = 0;
      } else {
        float normalized = (SystemClock.uptimeMillis() - startTimeMillis) / ANIMATION_DURATION;
        done = normalized >= 1.0f;
        normalized = Math.min(normalized, 1.0f);
        alpha = (int) (0xFF * normalized);
        animating = alpha != 0xFF;
      }
    }

    if (done) {
      canvas.drawBitmap(image, getPaddingLeft(), getPaddingTop(), null);
    } else {
      placeHolder.setBounds(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(),
          height - getPaddingBottom());
      placeHolder.setAlpha(0xFF - alpha);
      placeHolder.draw(canvas);

      if (alpha > 0) {
        paint.setAlpha(alpha);
        canvas.drawBitmap(image, getPaddingLeft(), getPaddingTop(), paint);
      }

      invalidate();
    }
  }

  public void setAspectRatio(float aspectRatio) {
    this.aspectRatio = aspectRatio;
    requestLayout();
    invalidate();
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    int width = MeasureSpec.getSize(widthMeasureSpec);
    int height = MeasureSpec.getSize(heightMeasureSpec);

    if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
      super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    } else {
      if (aspectRatio != 0.0f) {
        switch (dominantMeasurement) {
          case MEASUREMENT_HEIGHT:
            width = (int) ((height - getPaddingTop() - getPaddingBottom()) * aspectRatio)
                + getPaddingLeft()
                + getPaddingRight();
            break;

          case MEASUREMENT_WIDTH:
            height = (int) ((width - getPaddingLeft() - getPaddingRight()) * aspectRatio)
                + getPaddingTop()
                + getPaddingBottom();
            break;
        }
      }

      setMeasuredDimension(width, height);
    }
  }

  @Override protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState state = new SavedState(superState);

    state.imageUrl = imageUrl;

    return state;
  }

  @Override protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());

    setImage(savedState.imageUrl);
  }

  static class SavedState extends BaseSavedState {

    String imageUrl;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    public SavedState(Parcel in) {
      super(in);
      imageUrl = in.readString();
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeString(imageUrl);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      @Override public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}
