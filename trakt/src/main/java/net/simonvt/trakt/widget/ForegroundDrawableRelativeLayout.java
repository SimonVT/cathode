package net.simonvt.trakt.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Canvas;
import android.graphics.Rect;
import android.graphics.Region;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.widget.RelativeLayout;

public class ForegroundDrawableRelativeLayout extends RelativeLayout {

    private static final int[] ATTRS = new int[] {
            android.R.attr.foreground,
    };

    private static final int INDEX_FOREGROUND = 0;

    private Drawable mForeground;
    private boolean mForegroundBoundsChanged;

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
        if (mForeground != drawable) {
            if (mForeground != null) {
                mForeground.setCallback(null);
                unscheduleDrawable(mForeground);
            }

            mForeground = drawable;

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

    @Override
    protected boolean verifyDrawable(Drawable who) {
        return super.verifyDrawable(who) || (who == mForeground);
    }

    @Override
    public void jumpDrawablesToCurrentState() {
        super.jumpDrawablesToCurrentState();
        if (mForeground != null) mForeground.jumpToCurrentState();
    }

    @Override
    protected void drawableStateChanged() {
        super.drawableStateChanged();
        if (mForeground != null && mForeground.isStateful()) {
            mForeground.setState(getDrawableState());
        }
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        mForegroundBoundsChanged = true;
    }

    @Override
    public void draw(Canvas canvas) {
        super.draw(canvas);

        if (mForeground != null) {
            final Drawable foreground = mForeground;

            if (mForegroundBoundsChanged) {
                mForegroundBoundsChanged = false;
                foreground.setBounds(0, 0, getWidth(), getHeight());
            }

            foreground.draw(canvas);
        }
    }


    @Override
    public boolean gatherTransparentRegion(Region region) {
        boolean opaque = super.gatherTransparentRegion(region);
        if (region != null && mForeground != null) {
            applyDrawableToTransparentRegion(mForeground, region);
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
