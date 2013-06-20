package net.simonvt.trakt.widget;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;

public class HiddenPaneLayout extends ViewGroup {

    private static final String TAG = "HiddenPaneLayout";

    private static final int INVALID_POINTER = -1;

    private static final int CLOSE_ENOUGH = 3;

    private static final SmoothInterpolator SMOOTH_INTERPOLATOR = new SmoothInterpolator();

    private static final String ANIMATE_PROPERTY = "offsetPixels";

    public static final int STATE_CLOSED = 0;
    public static final int STATE_CLOSING = 1;
    public static final int STATE_DRAGGING = 2;
    public static final int STATE_OPENING = 4;
    public static final int STATE_OPEN = 8;

    private int mState = STATE_CLOSED;

    private ObjectAnimator mAnimator;

    private int mHiddenPaneWidth = -1;

    private float mOffsetPixels = 0;

    private View mContentPane;

    private View mHiddenPane;

    private boolean mIsDragging;

    private int mCloseEnough;

    private VelocityTracker mVelocityTracker;

    private float mLastMotionX;

    private float mLastMotionY;

    private float mInitialMotionX;

    private float mInitialMotionY;

    private int mActivePointerId;

    private int mTouchSlop;

    private int mMaxVelocity;

    private boolean mLayerTypeHardware;

    private Drawable mDropShadow;

    private int mDropShadowWidth;

    public HiddenPaneLayout(Context context) {
        super(context);
        init(context);
    }

    public HiddenPaneLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    public HiddenPaneLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init(context);
    }

    private void init(Context context) {
        final ViewConfiguration configuration = ViewConfiguration.get(context);
        mTouchSlop = configuration.getScaledTouchSlop();
        mMaxVelocity = configuration.getScaledMaximumFlingVelocity();
        mCloseEnough = dpToPx(CLOSE_ENOUGH);

        mDropShadow = new GradientDrawable(GradientDrawable.Orientation.LEFT_RIGHT, new int[] {
                0x80000000,
                0x00000000,
        });
        mDropShadowWidth = dpToPx(6);

        setWillNotDraw(false);
    }

    protected int dpToPx(int dp) {
        return (int) (getResources().getDisplayMetrics().density * dp + 0.5f);
    }

    public void toggle() {
        if (mState == STATE_OPEN || mState == STATE_OPENING) {
            close();
        } else if (mState == STATE_CLOSED || mState == STATE_CLOSING) {
            open();
        }
    }

    public void open() {
        open(true);
    }

    public void open(boolean animate) {
        animateOffsetTo(-mHiddenPaneWidth, 0, animate);
    }

    public void close() {
        close(true);
    }

    public void close(boolean animate) {
        animateOffsetTo(0, 0, animate);
    }

    @Override
    public void addView(View child, int index, LayoutParams params) {
        super.addView(child, index, params);
        if (mContentPane == null) {
            mContentPane = child;
        } else if (mHiddenPane == null) {
            mHiddenPane = child;
        } else {
            throw new IllegalStateException("HiddenPaneLayout can only have two direct children");
        }
    }

    @Override
    protected void dispatchDraw(Canvas canvas) {
        super.dispatchDraw(canvas);
        final int width = getWidth();
        final int height = getHeight();
        final int offsetPixels = (int) mOffsetPixels;

        mDropShadow.setBounds(width + offsetPixels, 0, width + offsetPixels + mDropShadowWidth, height);
        mDropShadow.draw(canvas);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        final int width = r - l;
        final int height = b - t;

        View contentPane = getChildAt(0);
        contentPane.layout(0, 0, width, height);

        final View hiddenPane = getChildAt(1);
        final int hiddenPaneWidth = hiddenPane.getMeasuredWidth();
        hiddenPane.layout(width, 0, width + hiddenPaneWidth, height);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

        if (widthMode == MeasureSpec.UNSPECIFIED || heightMode == MeasureSpec.UNSPECIFIED) {
            throw new IllegalStateException("Must measure with an exact size");
        }

        final int width = MeasureSpec.getSize(widthMeasureSpec);
        final int height = MeasureSpec.getSize(heightMeasureSpec);

        final int contentWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, width);
        final int contentHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, height);
        mContentPane.measure(contentWidthMeasureSpec, contentHeightMeasureSpec);

        LayoutParams lp = mHiddenPane.getLayoutParams();
        mHiddenPaneWidth = lp.width;
        final int hiddenWidthMeasureSpec = getChildMeasureSpec(widthMeasureSpec, 0, lp.width);
        final int hiddenHeightMeasureSpec = getChildMeasureSpec(heightMeasureSpec, 0, height);
        mHiddenPane.measure(hiddenWidthMeasureSpec, hiddenHeightMeasureSpec);

        setMeasuredDimension(width, height);
    }

    private void setOffsetPixels(float offsetPixels) {
        final int oldOffset = (int) mOffsetPixels;
        mOffsetPixels = offsetPixels;
        final int newOffset = (int) offsetPixels;
        if (newOffset != oldOffset) {
            mContentPane.setTranslationX(newOffset);
            mHiddenPane.setTranslationX(newOffset);
            invalidate();
        }
    }

    private boolean isContentTouch(int x) {
        return mOffsetPixels < 0.0f && x < getWidth() + (int) mOffsetPixels;
    }

    protected boolean onDownAllowDrag(int x) {
        return mOffsetPixels < 0.0f && x < getWidth() + (int) mOffsetPixels;
    }

    protected boolean onMoveAllowDrag(int x) {
        return mOffsetPixels < 0.0f && x < getWidth() + (int) mOffsetPixels;
    }

    protected void onMoveEvent(float dx) {
        setOffsetPixels(Math.max(Math.min(mOffsetPixels + dx, 0), -mHiddenPaneWidth));
    }

    protected void onUpEvent(int x, int y) {
        final int offsetPixels = (int) mOffsetPixels;

        if (mIsDragging) {
            mVelocityTracker.computeCurrentVelocity(1000, mMaxVelocity);
            final int initialVelocity = (int) mVelocityTracker.getXVelocity(mActivePointerId);
            mLastMotionX = x;
            animateOffsetTo(initialVelocity < 0 ? -mHiddenPaneWidth : 0, initialVelocity, true);

            // Close the menu when content is clicked while the menu is visible.
        } else if (mOffsetPixels < 0.0f && x < getWidth() + offsetPixels) {
            close();
        }
    }

    protected boolean checkTouchSlop(float dx, float dy) {
        return Math.abs(dx) > mTouchSlop && Math.abs(dx) > Math.abs(dy);
    }

    private boolean isCloseEnough() {
        return Math.abs(mOffsetPixels) <= mCloseEnough;
    }

    private void stopAnimation() {
        if (mAnimator != null) {
            mAnimator.cancel();
            mAnimator = null;
        }
        stopLayerTranslation();
    }

    /**
     * Called when a drag has been ended.
     */
    protected void endDrag() {
        mIsDragging = false;

        if (mVelocityTracker != null) {
            mVelocityTracker.recycle();
            mVelocityTracker = null;
        }
    }

    private void setState(int state) {
        if (state != mState) {
            mState = state;
        }
    }

    protected void logState(int state) {
        switch (state) {
            case STATE_CLOSED:
                Log.v(TAG, "[State] STATE_CLOSED");
                break;

            case STATE_CLOSING:
                Log.v(TAG, "[State] STATE_CLOSING");
                break;

            case STATE_DRAGGING:
                Log.v(TAG, "[State] STATE_DRAGGING");
                break;

            case STATE_OPENING:
                Log.v(TAG, "[State] STATE_OPENING");
                break;

            case STATE_OPEN:
                Log.v(TAG, "[State] STATE_OPEN");
                break;

            default:
                Log.v(TAG, "[State] Unknown: " + state);
        }
    }

    protected void animateOffsetTo(int position, int velocity, boolean animate) {
        endDrag();
        stopAnimation();

        final int startX = (int) mOffsetPixels;
        final int dx = position - startX;
        if (dx == 0 || !animate) {
            setOffsetPixels(position);
            setState(position == 0 ? STATE_CLOSED : STATE_OPEN);
            stopLayerTranslation();
            return;
        }

        int duration;

        velocity = Math.abs(velocity);
        if (velocity > 0) {
            duration = 4 * Math.round(1000.f * Math.abs((float) dx / velocity));
        } else {
            duration = (int) (600.f * Math.abs((float) dx / mHiddenPaneWidth));
        }

        duration = Math.min(duration, 600);
        animateOffsetTo(position, duration);
    }

    protected void animateOffsetTo(final int position, int duration) {
        if (position < Math.abs(mOffsetPixels)) {
            setState(STATE_OPENING);
        } else {
            setState(STATE_CLOSING);
        }

        mAnimator = ObjectAnimator.ofFloat(this, ANIMATE_PROPERTY, mOffsetPixels, position);
        mAnimator.setInterpolator(SMOOTH_INTERPOLATOR);
        mAnimator.setDuration(duration);
        mAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
                startLayerTranslation();
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                stopLayerTranslation();
                setState(position == 0 ? STATE_CLOSED : STATE_OPEN);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        mAnimator.start();
    }

    protected void startLayerTranslation() {
        if (!mLayerTypeHardware) {
            mLayerTypeHardware = true;
            mContentPane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
            mHiddenPane.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        }
    }

    /**
     * If the current layer type is {@link android.view.View#LAYER_TYPE_HARDWARE}, this will set it to
     * {@link View#LAYER_TYPE_NONE}.
     */
    protected void stopLayerTranslation() {
        if (mLayerTypeHardware) {
            mLayerTypeHardware = false;
            mContentPane.setLayerType(View.LAYER_TYPE_NONE, null);
            mHiddenPane.setLayerType(View.LAYER_TYPE_NONE, null);
        }
    }

    public boolean onInterceptTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL) {
            mActivePointerId = INVALID_POINTER;
            mIsDragging = false;
            if (mVelocityTracker != null) {
                mVelocityTracker.recycle();
                mVelocityTracker = null;
            }

            if (Math.abs(mOffsetPixels) > mHiddenPaneWidth / 2) {
                open();
            } else {
                close();
            }

            return false;
        }

        if (action == MotionEvent.ACTION_DOWN && mOffsetPixels < 0.0f && isCloseEnough()) {
            setOffsetPixels(0);
            stopAnimation();
            setState(STATE_CLOSED);
            mIsDragging = false;
        }

        // Always intercept events over the content while menu is visible.
        if (mOffsetPixels < 0.0f) {
            int index = 0;
            if (mActivePointerId != INVALID_POINTER) {
                index = ev.findPointerIndex(mActivePointerId);
                index = index == -1 ? 0 : index;
            }

            final int x = (int) ev.getX(index);
            final int y = (int) ev.getY(index);
            if (isContentTouch(x)) {
                return true;
            }
        }

        if (mOffsetPixels == 0.0f) {
            return false;
        }

        if (action != MotionEvent.ACTION_DOWN && mIsDragging) {
            return true;
        }

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                final boolean allowDrag = onDownAllowDrag((int) mLastMotionX);
                mActivePointerId = ev.getPointerId(0);

                if (allowDrag) {
                    setState(mOffsetPixels < 0.0f ? STATE_OPEN : STATE_CLOSED);

                    stopAnimation();
                    startLayerTranslation();
                    mIsDragging = false;
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                final int activePointerId = mActivePointerId;
                if (activePointerId == INVALID_POINTER) {
                    // If we don't have a valid id, the touch down wasn't on content.
                    break;
                }

                final int pointerIndex = ev.findPointerIndex(activePointerId);

                final float x = ev.getX(pointerIndex);
                final float dx = x - mLastMotionX;
                final float y = ev.getY(pointerIndex);
                final float dy = y - mLastMotionY;

                if (checkTouchSlop(dx, dy)) {
                    final boolean allowDrag = onMoveAllowDrag((int) x);

                    if (allowDrag) {
                        startLayerTranslation();
                        setState(STATE_DRAGGING);
                        mIsDragging = true;
                        mLastMotionX = x;
                        mLastMotionY = y;
                    }
                }
                break;
            }

            case MotionEvent.ACTION_POINTER_UP:
                onPointerUp(ev);
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
                mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(ev);

        return mIsDragging;
    }

    @Override
    public boolean onTouchEvent(MotionEvent ev) {
        final int action = ev.getAction() & MotionEvent.ACTION_MASK;

        if (action == MotionEvent.ACTION_DOWN && mOffsetPixels == 0.0f) {
            return false;
        }

        if (mVelocityTracker == null) mVelocityTracker = VelocityTracker.obtain();
        mVelocityTracker.addMovement(ev);

        switch (action) {
            case MotionEvent.ACTION_DOWN: {
                mLastMotionX = mInitialMotionX = ev.getX();
                mLastMotionY = mInitialMotionY = ev.getY();
                final boolean allowDrag = onDownAllowDrag((int) mLastMotionX);

                mActivePointerId = ev.getPointerId(0);

                if (allowDrag) {
                    stopAnimation();
                    startLayerTranslation();
                }
                break;
            }

            case MotionEvent.ACTION_MOVE: {
                if (!mIsDragging) {
                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);

                    final float x = ev.getX(pointerIndex);
                    final float dx = x - mLastMotionX;
                    final float y = ev.getY(pointerIndex);
                    final float dy = y - mLastMotionY;

                    if (checkTouchSlop(dx, dy)) {
                        final boolean allowDrag = onMoveAllowDrag((int) x);

                        if (allowDrag) {
                            mIsDragging = true;
                            setState(STATE_DRAGGING);
                            mLastMotionX = x;
                            mLastMotionY = y;
                        } else {
                            mInitialMotionX = x;
                            mInitialMotionY = y;
                        }
                    }
                }

                if (mIsDragging) {
                    startLayerTranslation();

                    final int pointerIndex = ev.findPointerIndex(mActivePointerId);

                    final float x = ev.getX(pointerIndex);
                    final float dx = x - mLastMotionX;
                    final float y = ev.getY(pointerIndex);
                    final float dy = y - mLastMotionY;

                    mLastMotionX = x;
                    mLastMotionY = y;
                    onMoveEvent(dx);
                }
                break;
            }

            case MotionEvent.ACTION_CANCEL:
            case MotionEvent.ACTION_UP: {
                final int index = ev.findPointerIndex(mActivePointerId);
                final int x = (int) ev.getX(index);
                final int y = (int) ev.getY(index);
                onUpEvent(x, y);
                mActivePointerId = INVALID_POINTER;
                mIsDragging = false;
                break;
            }

            case MotionEvent.ACTION_POINTER_DOWN:
                final int index = (ev.getAction() & MotionEvent.ACTION_POINTER_INDEX_MASK)
                        >> MotionEvent.ACTION_POINTER_INDEX_SHIFT;
                mLastMotionX = ev.getX(index);
                mLastMotionY = ev.getY(index);
                mActivePointerId = ev.getPointerId(index);
                break;

            case MotionEvent.ACTION_POINTER_UP:
                onPointerUp(ev);
                mLastMotionX = ev.getX(ev.findPointerIndex(mActivePointerId));
                mLastMotionY = ev.getY(ev.findPointerIndex(mActivePointerId));
                break;
        }

        return true;
    }

    private void onPointerUp(MotionEvent ev) {
        final int pointerIndex = ev.getActionIndex();
        final int pointerId = ev.getPointerId(pointerIndex);
        if (pointerId == mActivePointerId) {
            final int newPointerIndex = pointerIndex == 0 ? 1 : 0;
            mLastMotionX = ev.getX(newPointerIndex);
            mActivePointerId = ev.getPointerId(newPointerIndex);
            if (mVelocityTracker != null) {
                mVelocityTracker.clear();
            }
        }
    }

    @Override
    protected Parcelable onSaveInstanceState() {
        Parcelable superState = super.onSaveInstanceState();
        SavedState state = new SavedState(superState);

        state.mOpen = mState == STATE_OPENING || mState == STATE_OPEN;
        return state;
    }

    @Override
    protected void onRestoreInstanceState(Parcelable state) {
        SavedState savedState = (SavedState) state;
        super.onRestoreInstanceState(savedState.getSuperState());

        final int hiddenPaneWidth = mHiddenPane.getLayoutParams().width;

        setOffsetPixels(savedState.mOpen ? -hiddenPaneWidth : 0);
        setState(savedState.mOpen ? STATE_OPEN : STATE_CLOSED);
    }

    static class SavedState extends BaseSavedState {

        boolean mOpen;

        public SavedState(Parcelable superState) {
            super(superState);
        }

        public SavedState(Parcel in) {
            super(in);
            mOpen = in.readInt() == 1;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            super.writeToParcel(dest, flags);
            dest.writeInt(mOpen ? 1 : 0);
        }

        @SuppressWarnings("UnusedDeclaration")
        public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
            @Override
            public SavedState createFromParcel(Parcel in) {
                return new SavedState(in);
            }

            @Override
            public SavedState[] newArray(int size) {
                return new SavedState[size];
            }
        };
    }
}
