package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;

public abstract class AbsAdapterFragment extends BaseFragment {

    private static final String SAVED_EMPTY_TEXT = "savedEmptyText";

    private static final int STATE_NONE = -1;
    private static final int STATE_PROGRESS_VISIBLE = 0;
    private static final int STATE_LIST_VISIBLE = 1;

    private static final int ANIMATION_DURATION = 600;

    private BaseAdapter mAdapter;

    @InjectView(R.id.progressContainer) View mProgressContainer;
    @InjectView(R.id.listContainer) View mListContainer;
    @InjectView(android.R.id.list) AbsListView mAdapterView;
    @InjectView(android.R.id.empty) TextView mEmpty;

    private Context mAppContext;

    private String mEmptyText;

    protected boolean mAttachLongClickListener;

    private boolean mAnimating;

    private int mCurrentState = STATE_PROGRESS_VISIBLE;
    private int mPendingStateChange = STATE_NONE;

    @Override
    public void onCreate(Bundle state) {
        super.onCreate(state);
        mAppContext = getActivity().getApplicationContext();

        if (state != null) {
            mEmptyText = state.getString(SAVED_EMPTY_TEXT);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        outState.putString(SAVED_EMPTY_TEXT, mEmptyText);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Views.inject(this, view);

        if (mEmptyText != null) mEmpty.setText(mEmptyText);

        mAdapterView.setOnItemClickListener(mOnClickListener);
        if (mAttachLongClickListener) mAdapterView.setOnItemLongClickListener(mOnLongClickListener);
        mAdapterView.setEmptyView(mEmpty);

        if (mAdapter == null) {
            mListContainer.setVisibility(View.GONE);
            mProgressContainer.setVisibility(View.VISIBLE);
            mCurrentState = STATE_PROGRESS_VISIBLE;
        } else {
            mAdapterView.setAdapter(mAdapter);
            mCurrentState = STATE_LIST_VISIBLE;
            mListContainer.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.GONE);
        }
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

    private void changeState(final int newState, final boolean animate) {
        if (newState == mCurrentState) {
            return;
        }

        if (mAnimating) {
            mPendingStateChange = newState;
            return;
        }

        mCurrentState = newState;

        if (newState == STATE_PROGRESS_VISIBLE && mListContainer.getVisibility() != View.VISIBLE) {
            return;
        }

        if (newState == STATE_LIST_VISIBLE && !animate) {
            mListContainer.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.GONE);
        } else if (newState == STATE_PROGRESS_VISIBLE && !animate) {
            mListContainer.setVisibility(View.GONE);
            mProgressContainer.setVisibility(View.VISIBLE);
        } else {
            mListContainer.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.VISIBLE);

            final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(ANIMATION_DURATION);
            final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(ANIMATION_DURATION);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (mProgressContainer == null) {
                        // In case fragment is removed before animation is done
                        return;
                    }
                    if (newState == STATE_LIST_VISIBLE) {
                        mProgressContainer.setVisibility(View.GONE);
                    } else {
                        mListContainer.setVisibility(View.GONE);
                        if (mAdapter == null) {
                            mAdapterView.setAdapter(null);
                        }
                    }
                }

                @Override
                public void onAnimationRepeat(Animation animation) {
                }
            });

            getView().postOnAnimation(new Runnable() {
                @Override
                public void run() {
                    if (mListContainer != null) {
                        mListContainer.startAnimation(newState == STATE_LIST_VISIBLE ? fadeIn : fadeOut);
                        mProgressContainer.startAnimation(newState == STATE_LIST_VISIBLE ? fadeOut : fadeIn);
                    }
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        mProgressContainer = null;
        mListContainer = null;
        mEmpty = null;
        mAdapterView = null;

        super.onDestroyView();
    }

    private final AdapterView.OnItemClickListener mOnClickListener = new AdapterView.OnItemClickListener() {
        public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
            AbsAdapterFragment.this.onItemClick(parent, v, position, id);
        }
    };

    protected void onItemClick(AdapterView l, View v, int position, long id) {
    }

    private final AdapterView.OnItemLongClickListener mOnLongClickListener = new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View v, int position, long id) {
            return AbsAdapterFragment.this.onItemLongClick(adapterView, v, position, id);
        }
    };

    protected boolean onItemLongClick(AdapterView l, View v, int position, long id) {
        return false;
    }

    public AbsListView getAdapterView() {
        return mAdapterView;
    }

    public void setAdapter(BaseAdapter adapter) {
        if (adapter != mAdapter) {
            mAdapter = adapter;
            if (mAdapter != null) {
                if (mAdapterView != null) {
                    mAdapterView.setAdapter(mAdapter);
                    if (mListContainer.getVisibility() != View.VISIBLE) {
                        changeState(STATE_LIST_VISIBLE, true);
                    }
                }
            } else if (mAdapterView != null) {
                changeState(STATE_PROGRESS_VISIBLE, true);
            }
        }
    }

    public BaseAdapter getAdapter() {
        return mAdapter;
    }

    public void clearEmptyText() {
        mEmptyText = "";
        if (mEmpty != null) {
            mEmpty.setText(mEmptyText);
        }
    }

    final void setEmptyText(String text) {
        if (text == null) {
            text = "";
        }
        mEmptyText = text;
        if (mEmpty != null) {
            mEmpty.setText(mEmptyText);
        }
    }

    final void setEmptyText(int resId) {
        String text = mAppContext.getString(resId);
        if (text != null) {
            mEmptyText = text;
            if (mEmpty != null) {
                mEmpty.setText(mEmptyText);
            }
        }
    }

    final void setEmptyText(int resId, Object... formatArgs) {
        String text = mAppContext.getString(resId, formatArgs);
        if (text != null) {
            mEmptyText = text;
            if (mEmpty != null) {
                mEmpty.setText(mEmptyText);
            }
        }
    }
}
