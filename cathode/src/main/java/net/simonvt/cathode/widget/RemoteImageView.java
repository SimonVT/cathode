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

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.SystemClock;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.ViewOutlineProvider;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.RequestCreator;
import com.squareup.picasso.Target;
import com.squareup.picasso.Transformation;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.animation.MaterialTransition;
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
public class RemoteImageView extends AspectRatioView implements Target {

  private static final float ANIMATION_DURATION = 1000.0f;

  @Inject Picasso picasso;

  private Drawable placeHolder;

  private Bitmap image;

  private boolean animating;

  private long startTimeMillis;

  private float fraction;

  private Paint paint = new Paint();

  private ColorMatrix colorMatrix;
  private ColorMatrixColorFilter colorMatrixColorFilter;

  private String imageUrl;
  private int imageResource;

  private List<Transformation> transformations = new ArrayList<>();

  private int resizeInsetX;

  private int resizeInsetY;

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

    a.recycle();

    setupOutlineProvider();

    colorMatrix = new ColorMatrix();
    colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
  }

  @TargetApi(Build.VERSION_CODES.LOLLIPOP) private void setupOutlineProvider() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      setOutlineProvider(ViewOutlineProvider.PADDED_BOUNDS);
    }
  }

  public void addTransformation(Transformation transformation) {
    transformations.add(transformation);
  }

  public void clearTransformations() {
    transformations.clear();
  }

  public void removeTransformation(Transformation transformation) {
    transformations.remove(transformation);
  }

  public void setResizeInsets(int x, int y) {
    resizeInsetX = x;
    resizeInsetY = y;
  }

  public void setImage(String imageUrl) {
    setImage(imageUrl, false);
  }

  public void setImage(String imageUrl, boolean animateIfDifferent) {
    picasso.cancelRequest(this);

    boolean animate = animateIfDifferent && !TextUtils.equals(imageUrl, this.imageUrl);

    this.imageUrl = imageUrl;
    this.imageResource = 0;
    image = null;
    fraction = 0.0f;
    startTimeMillis = 0;
    animating = false;

    if (getWidth() - getPaddingLeft() - getPaddingRight() > 0
        && getHeight() - getPaddingTop() - getPaddingBottom() > 0) {
      loadBitmap(animate);
    }

    invalidate();
  }

  public void setImage(int imageResource) {
    picasso.cancelRequest(this);

    this.imageUrl = null;
    this.imageResource = imageResource;
    image = null;
    fraction = 0.0f;
    startTimeMillis = 0;
    animating = false;

    if (getWidth() - getPaddingLeft() - getPaddingRight() > 0
        && getHeight() - getPaddingTop() - getPaddingBottom() > 0) {
      loadBitmap(false);
    }

    invalidate();
  }

  private void loadBitmap(boolean animate) {
    fraction = 0.0f;
    image = null;
    final int width = getWidth() - getPaddingLeft() - getPaddingRight();
    final int height = getHeight() - getPaddingTop() - getPaddingBottom();

    RequestCreator creator = null;
    if (imageUrl != null) {
      creator = picasso.load(imageUrl);
    } else if (imageResource > 0) {
      creator = picasso.load(imageResource);
    }

    if (creator != null) {
      creator.resize(width - resizeInsetX, height - resizeInsetY).centerCrop();
      for (Transformation transformation : transformations) {
        creator.transform(transformation);
      }
      creator.into(this);
    }

    if (!animate && image != null) {
      animating = false;
      startTimeMillis = 0;
      fraction = 1.0f;
    }
  }

  @Override public void onBitmapLoaded(Bitmap bitmap, Picasso.LoadedFrom loadedFrom) {
    image = bitmap;
    animating = true;
    startTimeMillis = 0;
    fraction = 0.0f;
    invalidate();
  }

  @Override public void onBitmapFailed(Drawable drawable) {
    Timber.d("[onBitmapFailed] %s", imageUrl);
  }

  @Override public void onPrepareLoad(Drawable drawable) {
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if ((imageUrl != null || imageResource > 0)
        && w - getPaddingLeft() - getPaddingRight() > 0
        && h - getPaddingTop() - getPaddingBottom() > 0) {
      loadBitmap(false);
    }
  }

  public float getFraction() {
    return fraction;
  }

  @Override protected void onDraw(Canvas canvas) {
    if (image == null) {
      drawPlaceholder(canvas, placeHolder, 255);
      return;
    }

    boolean done = true;
    int alpha = 0;

    if (animating) {
      if (startTimeMillis == 0) {
        startTimeMillis = SystemClock.uptimeMillis();
        done = false;
        fraction = 0.0f;
      } else {
        float normalized = (SystemClock.uptimeMillis() - startTimeMillis) / ANIMATION_DURATION;
        done = normalized >= 1.0f;
        fraction = Math.min(normalized, 1.0f);
        alpha = (int) (0xFF * fraction);
        animating = alpha != 0xFF;
      }
    }

    if (done) {
      drawBitmap(canvas, image, !done, fraction);
    } else {
      drawPlaceholder(canvas, placeHolder, 0xFF - alpha);

      if (alpha > 0) {
        drawBitmap(canvas, image, !done, fraction);
      }

      invalidate();
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
        invalidateOutline();
      }
    }
  }

  protected void drawPlaceholder(Canvas canvas, Drawable placeholder, int alpha) {
    final int width = getWidth();
    final int height = getHeight();
    placeHolder.setBounds(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(),
        height - getPaddingBottom());
    placeholder.setAlpha(alpha);
    placeholder.setAlpha(alpha);
    placeholder.draw(canvas);
  }

  protected void drawBitmap(Canvas canvas, Bitmap bitmap, boolean animating, float fraction) {
    if (animating) {
      colorMatrix.reset();
      MaterialTransition.apply(colorMatrix, fraction);
      colorMatrixColorFilter = new ColorMatrixColorFilter(colorMatrix);
      paint.setColorFilter(colorMatrixColorFilter);
    } else if (paint.getColorFilter() != null) {
      paint.setColorFilter(null);
    }

    canvas.drawBitmap(bitmap, getPaddingLeft(), getPaddingTop(), paint);
  }

  @Override protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState state = new SavedState(superState);

    state.imageUrl = imageUrl;
    state.imageResource = imageResource;

    return state;
  }

  @Override protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());

    setImage(savedState.imageUrl);
  }

  static class SavedState extends BaseSavedState {

    String imageUrl;

    int imageResource;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    public SavedState(Parcel in) {
      super(in);
      imageUrl = in.readString();
      imageResource = in.readInt();
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeString(imageUrl);
      dest.writeInt(imageResource);
    }

    @SuppressWarnings("UnusedDeclaration") public static final Creator<SavedState> CREATOR =
        new Creator<SavedState>() {
          @Override public SavedState createFromParcel(Parcel in) {
            return new SavedState(in);
          }

          @Override public SavedState[] newArray(int size) {
            return new SavedState[size];
          }
        };
  }
}
