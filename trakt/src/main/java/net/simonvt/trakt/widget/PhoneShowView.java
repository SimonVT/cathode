package net.simonvt.trakt.widget;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

public class PhoneShowView extends ViewGroup {

    private int mMinHeight;

    @InjectView(R.id.infoParent) View mInfoParent;
    @InjectView(R.id.overflow) OverflowView mOverflow;
    @InjectView(R.id.poster) RemoteImageView mPoster;

    public PhoneShowView(Context context) {
        super(context);
        init();
    }

    public PhoneShowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public PhoneShowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        mMinHeight = getResources().getDimensionPixelSize(R.dimen.showItemMinHeight);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Views.inject(this);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();

        final int width = r - l;

        final LayoutParams posterLp = (LayoutParams) mPoster.getLayoutParams();
        final int posterWidth = mPoster.getMeasuredWidth();
        final int posterHeight = mPoster.getMeasuredHeight();

        final int posterLeft = paddingLeft + posterLp.leftMargin;
        final int posterTop = paddingTop + posterLp.topMargin;
        mPoster.layout(posterLeft, posterTop, posterLeft + posterWidth, posterTop + posterHeight);

        final LayoutParams infoLp = (LayoutParams) mInfoParent.getLayoutParams();

        final int infoWidth = mInfoParent.getMeasuredWidth();
        final int infoHeight = mInfoParent.getMeasuredHeight();
        final int infoLeft = posterLeft + posterWidth + posterLp.rightMargin + infoLp.leftMargin;
        final int infoTop = paddingTop + infoLp.topMargin;

        mInfoParent.layout(infoLeft, infoTop, infoLeft + infoWidth, infoTop + infoHeight);

        final int overflowWidth = mOverflow.getMeasuredWidth();
        final int overflowHeight = mOverflow.getMeasuredHeight();
        final int overflowTop = getPaddingTop();
        final int overflowRight = width;
        mOverflow.layout(overflowRight - overflowWidth, overflowTop, overflowRight, overflowTop + overflowHeight);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode != MeasureSpec.EXACTLY) {
            throw new RuntimeException("PhoneShowView width must measure as EXACTLY.");
        }

        LayoutParams posterLp = (LayoutParams) mPoster.getLayoutParams();
        LayoutParams infoParentLp = (LayoutParams) mInfoParent.getLayoutParams();

        // Measure the height of show and next episode info
        measureChild(mInfoParent, widthMeasureSpec, heightMeasureSpec);
        final int infoParentHeight =
                mInfoParent.getMeasuredHeight() + infoParentLp.topMargin + infoParentLp.bottomMargin;

        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);

        final int paddingLeft = getPaddingLeft();
        final int paddingTop = getPaddingTop();
        final int paddingRight = getPaddingRight();
        final int paddingBottom = getPaddingBottom();

        int leftoverWidth = viewWidth - paddingLeft - paddingRight;

        final int viewHeight = Math.max(infoParentHeight + paddingTop + paddingBottom, mMinHeight);

        final int osterHeight = viewHeight - paddingTop - paddingBottom - posterLp.topMargin - posterLp.bottomMargin;
        final int posterWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
        final int posterHeightMeasureSpec = MeasureSpec.makeMeasureSpec(osterHeight, MeasureSpec.EXACTLY);
        mPoster.measure(posterWidthMeasureSpec, posterHeightMeasureSpec);

        leftoverWidth -= mPoster.getMeasuredWidth() - posterLp.leftMargin - posterLp.rightMargin;

        final int infoParentWidth = leftoverWidth - infoParentLp.leftMargin - infoParentLp.rightMargin;
        final int infoParentWidthMeasureSpec = MeasureSpec.makeMeasureSpec(infoParentWidth, MeasureSpec.EXACTLY);
        final int infoParentHeightMeasureSpec = MeasureSpec.makeMeasureSpec(
                viewHeight - paddingTop - paddingBottom - infoParentLp.topMargin - infoParentLp.bottomMargin,
                MeasureSpec.EXACTLY);
        mInfoParent.measure(infoParentWidthMeasureSpec, infoParentHeightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);

        measureChild(mOverflow, widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new LayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
        return new LayoutParams(p);
    }

    @Override
    protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
        return p instanceof LayoutParams;
    }

    public static class LayoutParams extends MarginLayoutParams {

        public LayoutParams(Context c, AttributeSet attrs) {
            super(c, attrs);
        }

        public LayoutParams(int width, int height) {
            super(width, height);
        }

        public LayoutParams(ViewGroup.LayoutParams p) {
            super(p);
        }

        public LayoutParams(MarginLayoutParams source) {
            super(source);
        }
    }
}
