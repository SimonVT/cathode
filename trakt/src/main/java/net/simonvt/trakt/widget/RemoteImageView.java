package net.simonvt.trakt.widget;

import com.squareup.picasso.Picasso;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.widget.ImageView;
import android.widget.Scroller;

import javax.inject.Inject;

/**
 * Simple View used to display an image from a remote source. An URL to an image is passed to
 * {@link #setImage(String)}, and the view then takes care of loading and displaying it.
 * <p/>
 * The view must either have a fixed width or a fixed width. Both can also be set. If only one dimension is fixed,
 * {@link #setAspectRatio(float)} must be called. The non-fixed dimension will then be calculated by multiplying the
 * fixed dimension with this value.
 */
public class RemoteImageView extends ImageView {

    private static final int ANIMATION_DURATION = 600;

    private static final int MEASUREMENT_HEIGHT = 0;

    private static final int MEASUREMENT_WIDTH = 1;

    @Inject Picasso mPicasso;

    private Bitmap mPlaceHolder;

    private Bitmap mImage;

    private int mImageAlpha;

    private Paint mPaint = new Paint();

    private Rect mDstRect = new Rect();

    private Scroller mScroller;

    private String mImageUrl;

    private float mAspectRatio = 0.0f;

    private int mDominantMeasurement = MEASUREMENT_HEIGHT;

    private boolean mMeasured;

    private int mPlaceholderRes;

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

        mPlaceholderRes =
                a.getResourceId(R.styleable.RemoteImageView_placeholder, R.drawable.placeholder_default);
        mPlaceHolder = BitmapFactory.decodeResource(getResources(), mPlaceholderRes);
        mAspectRatio = a.getFloat(R.styleable.RemoteImageView_aspectRatio, 0.0f);
        mDominantMeasurement = a.getInt(R.styleable.RemoteImageView_dominantMeasurement, MEASUREMENT_HEIGHT);

        a.recycle();
    }

    public void setImage(String imageUrl) {
        //removeCallbacks(mAnimationRunnable);

        mPicasso.cancelRequest(this);

        mImageUrl = imageUrl;
        mImage = null;
        mImageAlpha = 0;

        if (mMeasured) {
            mPicasso.load(imageUrl)
                    .resize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom())
                    .placeholder(mPlaceholderRes)
                    .centerCrop()
                    .into(this);
            //resetAnimation();
        }
    }

    //    public void setImage(Bitmap bitmap) {
    //        removeCallbacks(mAnimationRunnable);
    //        mPicasso.cancelRequest(this);
    //        mImage = bitmap;
    //        mImageAlpha = 0;
    //        if (bitmap != null) {
    //            startAnimation();
    //        }
    //        invalidate();
    //    }

    public void setAspectRatio(float aspectRatio) {
        mAspectRatio = aspectRatio;
        requestLayout();
        invalidate();
    }

    //    @Override
    //    public void onSuccess(Bitmap bitmap) {
    //        if (bitmap == null) {
    //            LogWrapper.d("RemoteImageView", "Picasso returned a null bitmap.. wtf?");
    //        }
    //        setImage(bitmap);
    //    }
    //
    //    @Override
    //    public void onError() {
    //        LogWrapper.d("RemoteImageView", "[onError]");
    //        postDelayed(new Runnable() {
    //            @Override
    //            public void run() {
    //                setImage(mImageUrl);
    //            }
    //        }, 10 * DateUtils.SECOND_IN_MILLIS);
    //    }

    //    private void startAnimation() {
    //        if (mScroller == null) {
    //            mScroller = new Scroller(getContext(), new AccelerateDecelerateInterpolator());
    //        }
    //
    //        mScroller.startScroll(0, 0, 255, 0, ANIMATION_DURATION);
    //        mAnimationRunnable.run();
    //    }
    //
    //    private Runnable mAnimationRunnable = new Runnable() {
    //        @Override
    //        public void run() {
    //            if (mScroller.computeScrollOffset()) {
    //                final int alpha = mScroller.getCurrX();
    //
    //                if (alpha != mImageAlpha) {
    //                    mImageAlpha = alpha;
    //                    invalidate();
    //                }
    //                if (alpha != mScroller.getFinalX()) {
    //                    postOnAnimation(this);
    //                    return;
    //                }
    //            }
    //
    //            endAnimation();
    //        }
    //    };
    //
    //    void endAnimation() {
    //        mImageAlpha = mImage != null ? 255 : 0;
    //        invalidate();
    //    }
    //
    //    public void resetAnimation() {
    //        removeCallbacks(mAnimationRunnable);
    //        mImageAlpha = mImage != null ? 255 : 0;
    //        invalidate();
    //    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        mMeasured = false;
    }

    //    @Override
    //    protected void onDraw(Canvas canvas) {
    //        final int width = getWidth();
    //        final int height = getHeight();
    //
    //        if (mImageAlpha == 255 && mImage == null) {
    //            //throw new RuntimeException("mImageAlpha == 255 && mImage == null");
    //        }
    //
    //        if (mImageAlpha != 255) {
    //            mDstRect.left = getPaddingLeft();
    //            mDstRect.top = getPaddingTop();
    //            mDstRect.right = width - getPaddingRight();
    //            mDstRect.bottom = height - getPaddingBottom();
    //
    //            mPaint.setAlpha(255 - mImageAlpha);
    //            canvas.drawBitmap(mPlaceHolder, null, mDstRect, mPaint);
    //        }
    //
    //        if (mImage != null && mImageAlpha != 0) {
    //            mPaint.setAlpha(mImageAlpha);
    //            canvas.drawBitmap(mImage, getPaddingLeft(), getPaddingTop(), mPaint);
    //        }
    //    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        int width = MeasureSpec.getSize(widthMeasureSpec);
        int height = MeasureSpec.getSize(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED && heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalArgumentException("RemoteImageView must be measured as EXACTLY");
        }

        if (mAspectRatio != 0.0f) {
            switch (mDominantMeasurement) {
                case MEASUREMENT_HEIGHT:
                    width = (int) ((height - getPaddingTop() - getPaddingBottom()) * mAspectRatio) + getPaddingLeft()
                            + getPaddingRight();
                    break;

                case MEASUREMENT_WIDTH:
                    height = (int) ((width - getPaddingLeft() - getPaddingRight()) * mAspectRatio) + getPaddingTop()
                            + getPaddingBottom();
                    break;
            }
        }

        setMeasuredDimension(width, height);

        mMeasured = true;

        if (mImageUrl != null) {
            mPicasso.load(mImageUrl)
                    .resize(getMeasuredWidth() - getPaddingLeft() - getPaddingRight(),
                            getMeasuredHeight() - getPaddingTop() - getPaddingBottom())
                    .placeholder(mPlaceholderRes)
                    .centerCrop()
                    .into(this);
            //resetAnimation();
        }
    }
}
