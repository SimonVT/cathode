package net.simonvt.trakt.widget;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.util.LogWrapper;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.os.SystemClock;
import android.util.AttributeSet;
import android.view.View;

import javax.inject.Inject;

/**
 * Simple View used to display an image from a remote source. An URL to an image is passed to
 * {@link #setImage(String)}, and the view then takes care of loading and displaying it.
 * <p/>
 * The view must either have a fixed width or a fixed width. Both can also be set. If only one dimension is fixed,
 * {@link #setAspectRatio(float)} must be called. The non-fixed dimension will then be calculated by multiplying the
 * fixed dimension with this value.
 */
public class RemoteImageView extends View implements Target {

    private static final String TAG = "RemoteImageView";

    private static final float ANIMATION_DURATION = 300.0f;

    private static final int MEASUREMENT_HEIGHT = 0;

    private static final int MEASUREMENT_WIDTH = 1;

    @Inject Picasso mPicasso;

    private Drawable mPlaceHolder;

    private Bitmap mImage;

    private boolean mAnimating;

    private long mStartTimeMillis;

    private int mAlpha;

    private Paint mPaint = new Paint();

    private String mImageUrl;

    private float mAspectRatio = 0.0f;

    private int mDominantMeasurement = MEASUREMENT_HEIGHT;

    private boolean mMeasured;

    public RemoteImageView(Context context) {
        this(context, null);
    }

    public RemoteImageView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RemoteImageView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        if (!isInEditMode()) {
            TraktApp.inject(context, this);
        }

        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.RemoteImageView);

        mPlaceHolder = a.getDrawable(R.styleable.RemoteImageView_placeholder);
        if (mPlaceHolder == null) {
            mPlaceHolder = getResources().getDrawable(R.drawable.placeholder_default);
        }
        mAspectRatio = a.getFloat(R.styleable.RemoteImageView_aspectRatio, 0.0f);
        mDominantMeasurement = a.getInt(R.styleable.RemoteImageView_dominantMeasurement, MEASUREMENT_HEIGHT);

        a.recycle();
    }

    public void setImage(String imageUrl) {
        mPicasso.cancelRequest(this);

        mImageUrl = imageUrl;
        mImage = null;
        mAlpha = 0;
        mStartTimeMillis = 0;
        mAnimating = false;

        if (mMeasured) {
            loadBitmap();
        }

        invalidate();
    }

    private void loadBitmap() {
        mAlpha = 0;
        mImage = null;
        mPicasso.load(mImageUrl)
                .resize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                        getMeasuredHeight() - getPaddingTop() - getPaddingBottom())
                .centerCrop()
                .into(this);
        if (mImage != null) {
            mAnimating = false;
            mStartTimeMillis = 0;
            mAlpha = 0xFF;
        }
    }

    @Override
    public void onSuccess(Bitmap bitmap) {
        mImage = bitmap;
        mAnimating = true;
        mStartTimeMillis = 0;
        mAlpha = 0;
        invalidate();
    }

    @Override
    public void onError() {
        LogWrapper.d(TAG, "[onError]");
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();

        if (mImage == null) {
            mPlaceHolder.setBounds(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(),
                    height - getPaddingBottom());
            mPlaceHolder.setAlpha(0xFF);
            mPlaceHolder.draw(canvas);
            return;
        }

        boolean done = true;

        if (mAnimating) {
            if (mStartTimeMillis == 0) {
                mStartTimeMillis = SystemClock.uptimeMillis();
                done = false;
                mAlpha = 0;
            } else {
                float normalized = (SystemClock.uptimeMillis() - mStartTimeMillis) / ANIMATION_DURATION;
                done = normalized >= 1.0f;
                normalized = Math.min(normalized, 1.0f);
                mAlpha = (int) (0xFF * normalized);
                mAnimating = mAlpha != 0xFF;
            }
        }

        if (done) {
            canvas.drawBitmap(mImage, getPaddingLeft(), getPaddingTop(), null);
        } else {
            mPlaceHolder.setBounds(getPaddingLeft(), getPaddingTop(), width - getPaddingRight(),
                    height - getPaddingBottom());
            mPlaceHolder.setAlpha(0xFF - mAlpha);
            mPlaceHolder.draw(canvas);

            if (mAlpha > 0) {
                mPaint.setAlpha(mAlpha);
                canvas.drawBitmap(mImage, getPaddingLeft(), getPaddingRight(), mPaint);
            }

            invalidate();
        }
    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        requestLayout();
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMeasured = false;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        } else {
            if (mAspectRatio != 0.0f) {
                switch (mDominantMeasurement) {
                    case MEASUREMENT_HEIGHT:
                        width = (int) ((height - getPaddingTop() - getPaddingBottom()) * mAspectRatio) +
                                getPaddingLeft()
                                + getPaddingRight();
                        break;

                    case MEASUREMENT_WIDTH:
                        height = (int) ((width - getPaddingLeft() - getPaddingRight()) * mAspectRatio) + getPaddingTop()
                                + getPaddingBottom();
                        break;
                }
            }

            setMeasuredDimension(width, height);
        }

        mMeasured = true;

        if (mImageUrl != null && getMeasuredWidth() - getPaddingLeft() - getPaddingRight() > 0
                && getMeasuredHeight() - getPaddingTop() - getPaddingBottom() > 0) {
            loadBitmap();
        }
    }
}
