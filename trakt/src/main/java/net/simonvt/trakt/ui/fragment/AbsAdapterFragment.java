package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

public abstract class AbsAdapterFragment extends BaseFragment {

    private static final String SAVED_EMPTY_TEXT = "savedEmptyText";

    private static final int ANIMATION_DURATION = 600;

    BaseAdapter mAdapter;

    @InjectView(R.id.progressContainer) View mProgressContainer;
    @InjectView(R.id.listContainer) View mListContainer;
    @InjectView(android.R.id.list) AbsListView mAdapterView;
    @InjectView(android.R.id.empty) TextView mEmpty;

    private LinearLayout mProgress;
    private Context mAppContext;

    private String mEmptyText = "";

    protected boolean mAttachLongClickListener;

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

        mEmpty.setText(mEmptyText);

        mAdapterView.setOnItemClickListener(mOnClickListener);
        if (mAttachLongClickListener) mAdapterView.setOnItemLongClickListener(mOnLongClickListener);
        mAdapterView.setEmptyView(mEmpty);

        if (mAdapter == null) {
            mListContainer.setVisibility(View.GONE);
            mProgressContainer.setVisibility(View.VISIBLE);
        } else {
            mAdapterView.setAdapter(mAdapter);
            showList(true, false);
        }
    }

    private void showList(final boolean showList, final boolean animate) {
        if (!showList && mListContainer.getVisibility() != View.VISIBLE) {
            return;
        }

        if (showList && !animate) {
            mListContainer.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.GONE);
        } else if (!showList && !animate) {
            mListContainer.setVisibility(View.GONE);
            mProgressContainer.setVisibility(View.VISIBLE);
        } else {
            mListContainer.setVisibility(View.VISIBLE);
            mProgressContainer.setVisibility(View.VISIBLE);

            Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
            fadeIn.setDuration(ANIMATION_DURATION);
            Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
            fadeOut.setDuration(ANIMATION_DURATION);
            fadeOut.setAnimationListener(new Animation.AnimationListener() {
                @Override
                public void onAnimationStart(Animation animation) {
                }

                @Override
                public void onAnimationEnd(Animation animation) {
                    if (showList) {
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

            mListContainer.startAnimation(showList ? fadeIn : fadeOut);
            mProgressContainer.startAnimation(showList ? fadeOut : fadeIn);
        }
    }

    @Override
    public void onDestroyView() {
        mProgressContainer = null;
        mListContainer = null;
        mEmpty = null;
        mAdapterView = null;
        mProgress = null;

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

    public AdapterView getAdapterView() {
        return mAdapterView;
    }

    public void setAdapter(BaseAdapter adapter) {
        if (adapter != mAdapter) {
            mAdapter = adapter;
            if (mAdapter != null) {
                if (mAdapterView != null) {
                    mAdapterView.setAdapter(mAdapter);
                    if (mListContainer.getVisibility() != View.VISIBLE) {
                        showList(true, true);
                    }
                }
            } else if (mAdapterView != null) {
                showList(false, true);
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
