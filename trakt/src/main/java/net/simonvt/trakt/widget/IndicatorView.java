package net.simonvt.trakt.widget;

import net.simonvt.trakt.R;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

public class IndicatorView extends View {

    private static final String TAG = "IndicatorView";

    private boolean mWatched;

    private boolean mCollected;

    private boolean mInWatchlist;

    private int mWatchedColor;

    private int mCollectedColor;

    private int mWatchlistColor;

    private int mDefaultColor;

    private Paint mPaint = new Paint();

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
        mWatchedColor = getResources().getColor(R.color.watchedColor);
        mCollectedColor = getResources().getColor(R.color.collectedColor);
        mWatchlistColor = getResources().getColor(R.color.watchlistColor);
        mDefaultColor = getResources().getColor(R.color.defaultColor);

        if (isInEditMode()) {
            mWatched = true;
            mCollected = true;
        }
    }

    public void setWatched(boolean watched) {
        mWatched = watched;
    }

    public void setCollected(boolean collected) {
        mCollected = collected;
    }

    public void setInWatchlist(boolean inWatchlist) {
        mInWatchlist = inWatchlist;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        final int width = getWidth();
        final int height = getHeight();

        if (width > height) {
            final int halfWidth = width / 2;

            if (mCollected) {
                mPaint.setColor(mCollectedColor);
            } else {
                mPaint.setColor(mDefaultColor);
            }
            canvas.drawRect(0, 0, halfWidth, height, mPaint);

            if (mWatched) {
                mPaint.setColor(mWatchedColor);
            } else if (mInWatchlist) {
                mPaint.setColor(mWatchlistColor);
            } else {
                mPaint.setColor(mDefaultColor);
            }
            canvas.drawRect(width - halfWidth, 0, width, height, mPaint);

        } else {
            final int halfHeight = height / 2;

            if (mCollected) {
                mPaint.setColor(mCollectedColor);
            } else {
                mPaint.setColor(mDefaultColor);
            }
            canvas.drawRect(0, 0, width, halfHeight, mPaint);

            if (mWatched) {
                mPaint.setColor(mWatchedColor);
            } else if (mInWatchlist) {
                mPaint.setColor(mWatchlistColor);
            } else {
                mPaint.setColor(mDefaultColor);
            }
            canvas.drawRect(0, height - halfHeight, width, height, mPaint);
        }
    }
}
