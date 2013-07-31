package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;

import net.simonvt.trakt.R;
import net.simonvt.trakt.util.LogWrapper;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

public abstract class ProgressFragment extends BaseFragment {

    private static final String TAG = "ProgressFragment";

    private static final int STATE_NONE = -1;
    private static final int STATE_PROGRESS_VISIBLE = 0;
    private static final int STATE_CONTENT_VISIBLE = 1;

    private static final int ANIMATION_DURATION = 600;

    @InjectView(R.id.contentContainer) View mContent;

    @InjectView(R.id.progressContainer) View mProgress;

    private boolean mAnimating;

    private boolean mWait;

    private int mCurrentState = STATE_PROGRESS_VISIBLE;
    private int mPendingStateChange = STATE_NONE;

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mWait = true;
        view.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                LogWrapper.v(TAG, "[onGlobalLayout] State: " + mCurrentState);
                getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
                mWait = false;

                if (mCurrentState == STATE_CONTENT_VISIBLE) {
                    mContent.setAlpha(1.0f);
                    mContent.setVisibility(View.VISIBLE);
                    mProgress.setVisibility(View.GONE);
                } else {
                    mContent.setVisibility(View.GONE);
                    mProgress.setAlpha(1.0f);
                    mProgress.setVisibility(View.VISIBLE);
                }
            }
        });
    }

    @Override
    public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
        Animation animation = null;
        if (nextAnim != 0) {
            animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
            animation.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                    mAnimating = true;
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    mAnimating = false;
                    if (mPendingStateChange != STATE_NONE) {
                        changeState(mPendingStateChange, true);
                        mPendingStateChange = STATE_NONE;
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });
        }

        return animation;
    }

    protected void setContentVisible(boolean contentVisible) {
        LogWrapper.v(TAG, "[setContentVisible]");
        if (getView() == null) {
            mCurrentState = contentVisible ? STATE_CONTENT_VISIBLE : STATE_PROGRESS_VISIBLE;
            return;
        }

        if (contentVisible) {
            changeState(STATE_CONTENT_VISIBLE, true);
        } else {
            changeState(STATE_PROGRESS_VISIBLE, true);
        }
    }

    private void changeState(final int newState, final boolean animate) {
        if (newState == mCurrentState) {
            return;
        }

        if (mAnimating) {
            mPendingStateChange = newState;
            return;
        }

        mCurrentState = newState;

        if (mWait || mProgress == null) {
            return;
        }

        if (newState == STATE_PROGRESS_VISIBLE && mContent.getVisibility() != View.VISIBLE) {
            return;
        }

        if (newState == STATE_CONTENT_VISIBLE && !animate) {
            mContent.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.GONE);
        } else if (newState == STATE_PROGRESS_VISIBLE && !animate) {
            mContent.setVisibility(View.GONE);
            mProgress.setVisibility(View.VISIBLE);
        } else {
            mContent.setVisibility(View.VISIBLE);
            mProgress.setVisibility(View.VISIBLE);

            if (newState == STATE_CONTENT_VISIBLE) {
                mProgress.animate().alpha(0.0f);
                if (mContent.getAlpha() == 1.0f) mContent.setAlpha(0.0f);
                mContent.animate().alpha(1.0f).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgress == null) {
                            // In case fragment is removed before animation is done
                            return;
                        }
                        mProgress.setVisibility(View.GONE);
                        ProgressFragment.this.onAnimationEnd(newState);
                    }
                });
            } else {
                if (mProgress.getAlpha() == 1.0f) mProgress.setAlpha(0.0f);
                mProgress.animate().alpha(1.0f);
                mContent.animate().alpha(0.0f).withEndAction(new Runnable() {
                    @Override
                    public void run() {
                        if (mProgress == null) {
                            // In case fragment is removed before animation is done
                            return;
                        }
                        mContent.setVisibility(View.GONE);
                        ProgressFragment.this.onAnimationEnd(newState);
                    }
                });
            }
        }
    }

    protected void onAnimationEnd(int newState) {
    }
}
