package net.simonvt.trakt.widget;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.util.UiUtils;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class PhoneEpisodeView extends AbsEpisodeView {

    private static final float SCREEN_RATIO = 680.f / 1000.f;

    @InjectView(R.id.screen) RemoteImageView mScreen;

    @InjectView(R.id.infoParent) ViewGroup mInfoParent;
    @InjectView(R.id.title) TextView mTitle;
    @InjectView(R.id.firstAired) TextView mFirstAired;
    @InjectView(R.id.episode) TextView mNumber;
    @InjectView(R.id.overflow) OverflowView mOverflow;
    @InjectView(R.id.checkbox) CheckMark mCheckbox;

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    private int mMinHeight;
    private int mPosterHeight;
    private int mPosterWidth;

    public PhoneEpisodeView(Context context) {
        super(context);
        init(context);
    }

    public PhoneEpisodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public PhoneEpisodeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        if (!isInEditMode()) TraktApp.inject(context, this);
        mMinHeight = getResources().getDimensionPixelSize(R.dimen.showItemMinHeight);
    }

    @Override
    public void setType(LibraryType type) {
        super.setType(type);
        if (mCheckbox != null) mCheckbox.setType(type == LibraryType.COLLECTION ? type : LibraryType.WATCHED);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        Views.inject(this);
        if (mType != null) mCheckbox.setType(mType);
        mOverflow.setListener(new OverflowView.OverflowActionListener() {
            @Override
            public void onPopupShown() {
                setHasTransientState(true);
            }

            @Override
            public void onPopupDismissed() {
                setHasTransientState(false);
            }

            @Override
            public void onActionSelected(int action) {
                switch (action) {
                    case R.id.action_watched:
                        mEpisodeWatched = true;
                        updateOverflowMenu();
                        mEpisodeScheduler.setWatched(mEpisodeId, true);
                        if (mType == LibraryType.WATCHED) mCheckbox.setVisibility(View.VISIBLE);
                        break;

                    case R.id.action_unwatched:
                        mEpisodeWatched = false;
                        updateOverflowMenu();
                        mEpisodeScheduler.setWatched(mEpisodeId, false);
                        if (mType == LibraryType.WATCHED) mCheckbox.setVisibility(View.INVISIBLE);
                        break;

                    case R.id.action_collection_add:
                        mEpisodeInCollection = true;
                        updateOverflowMenu();
                        mEpisodeScheduler.setIsInCollection(mEpisodeId, true);
                        if (mType == LibraryType.COLLECTION) mCheckbox.setVisibility(View.VISIBLE);
                        break;

                    case R.id.action_collection_remove:
                        mEpisodeInCollection = false;
                        updateOverflowMenu();
                        mEpisodeScheduler.setIsInCollection(mEpisodeId, false);
                        if (mType == LibraryType.COLLECTION) mCheckbox.setVisibility(View.INVISIBLE);
                        break;

                    case R.id.action_watchlist_add:
                        mEpisodeInWatchlist = true;
                        updateOverflowMenu();
                        mEpisodeScheduler.setIsInWatchlist(mEpisodeId, true);
                        break;

                    case R.id.action_watchlist_remove:
                        mEpisodeInWatchlist = false;
                        updateOverflowMenu();
                        mEpisodeScheduler.setIsInWatchlist(mEpisodeId, false);
                        break;
                }

                setHasTransientState(false);
            }
        });
    }

    private void updateOverflowMenu() {
        mOverflow.removeItems();
        if (mEpisodeWatched) {
            mOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
        } else {
            mOverflow.addItem(R.id.action_watched, R.string.action_watched);
        }

        if (mEpisodeInCollection) {
            mOverflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
        } else {
            mOverflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
        }

        if (mEpisodeInWatchlist) {
            mOverflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
        } else if (!mEpisodeWatched) {
            mOverflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
        }
    }

    @Override
    protected void onDataBound() {
        mTitle.setText(mEpisodeTitle);

        mFirstAired.setText(UiUtils.secondsToDate(getContext(), mEpisodeAired));
        mNumber.setText(String.valueOf(mEpisodeNumber));

        mScreen.setImage(mEpisodeScreenUrl);

        if (mType == LibraryType.COLLECTION) {
            mCheckbox.setVisibility(mEpisodeInCollection ? View.VISIBLE : View.INVISIBLE);
        } else {
            mCheckbox.setVisibility(mEpisodeWatched ? View.VISIBLE : View.INVISIBLE);
        }
        updateOverflowMenu();
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;

        final LayoutParams posterLp = (LayoutParams) mScreen.getLayoutParams();
        final int posterWidth = mScreen.getMeasuredWidth();
        mScreen.layout(posterLp.leftMargin, posterLp.topMargin, posterWidth + posterLp.leftMargin,
                height - posterLp.bottomMargin);

        final LayoutParams infoParentLp = (LayoutParams) mInfoParent.getLayoutParams();
        final int infoParentHeight = mInfoParent.getMeasuredHeight();
        final int infoParentTop = (height - infoParentHeight) / 2 + getPaddingTop() + infoParentLp.topMargin;
        final int infoParentBottom = infoParentTop + infoParentHeight;
        final int paddingRight = getPaddingRight();
        final int infoParentLeft = posterLp.leftMargin + posterWidth + posterLp.rightMargin + infoParentLp.leftMargin;
        final int infoParentRight = width - getPaddingRight() - infoParentLp.rightMargin;
        mInfoParent.layout(infoParentLeft, infoParentTop, infoParentRight, infoParentBottom);

        mOverflow.layout(width - mOverflow.getMeasuredWidth(), 0, width, mOverflow.getMeasuredHeight());

        LayoutParams watchedParams = (LayoutParams) mCheckbox.getLayoutParams();
        final int watchedRight = width - watchedParams.rightMargin - getPaddingRight();
        final int watchedBottom = height - watchedParams.bottomMargin - getPaddingBottom();
        mCheckbox.layout(watchedRight - mCheckbox.getMeasuredWidth(),
                watchedBottom - mCheckbox.getMeasuredHeight(), watchedRight, watchedBottom);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int mode = MeasureSpec.getMode(widthMeasureSpec);
        if (mode != MeasureSpec.EXACTLY) {
            throw new RuntimeException("PhoneShowView width must measure as EXACTLY.");
        }

        LayoutParams posterLp = (LayoutParams) mScreen.getLayoutParams();
        LayoutParams infoParentLp = (LayoutParams) mInfoParent.getLayoutParams();

        // Measure the height of show and next episode info
        measureChild(mInfoParent, widthMeasureSpec, heightMeasureSpec);
        final int infoParentHeight =
                mInfoParent.getMeasuredHeight() + infoParentLp.topMargin + infoParentLp.bottomMargin;

        final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
        final int viewHeight = Math.max(infoParentHeight + getPaddingTop() + getPaddingBottom(), mMinHeight);

        mPosterHeight = viewHeight - posterLp.topMargin - posterLp.bottomMargin;
        mPosterWidth = (int) (viewHeight * SCREEN_RATIO);
        final int posterWidthMeasureSpec = MeasureSpec.makeMeasureSpec(mPosterWidth, MeasureSpec.EXACTLY);
        final int posterHeightMeasureSpec = MeasureSpec.makeMeasureSpec(mPosterHeight, MeasureSpec.EXACTLY);
        mScreen.measure(posterWidthMeasureSpec, posterHeightMeasureSpec);

        final int infoParentWidth = viewWidth - mPosterWidth - posterLp.leftMargin - posterLp.rightMargin
                - infoParentLp.leftMargin - infoParentLp.rightMargin - getPaddingLeft() - getPaddingRight();
        final int infoParentWidthMeasureSpec =
                MeasureSpec.makeMeasureSpec(infoParentWidth, MeasureSpec.EXACTLY);
        final int infoParentHeightMeasureSpec =
                MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
        mInfoParent.measure(infoParentWidthMeasureSpec, infoParentHeightMeasureSpec);

        measureChild(mOverflow, widthMeasureSpec, heightMeasureSpec);
        measureChild(mCheckbox, widthMeasureSpec, heightMeasureSpec);

        setMeasuredDimension(viewWidth, viewHeight);
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
