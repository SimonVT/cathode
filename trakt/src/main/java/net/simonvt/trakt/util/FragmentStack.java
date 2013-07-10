package net.simonvt.trakt.util;

import net.simonvt.trakt.R;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

import java.util.LinkedList;

/**
 * A class that manages a stack of {@link Fragment}s in a single container.
 *
 * @param <T> Can be used to define a base fragment subclass.
 */
public final class FragmentStack<T extends Fragment> {

    public interface Callback<F extends Fragment> {

        void onStackChanged(int stackSize, F topFragment);
    }

    public static <T extends Fragment> FragmentStack<T> forContainer(FragmentActivity activity, int containerId,
            Callback<T> callback) {
        return new FragmentStack<T>(activity, containerId, callback);
    }

    private static final String STATE_STACK = "net.simonvt.trakt.util.FragmentStack.stack";

    private LinkedList<T> mStack = new LinkedList<T>();

    private Activity mActivity;

    private FragmentManager mFragmentManager;
    private FragmentTransaction mFragmentTransaction;

    private int mContainerId;

    private Callback<T> mCallback;

    private Handler mHandler;

    private int mEnterAnimation = R.anim.fade_in_front;

    private int mExitAnimation = R.anim.fade_out_back;

    private int mPopStackEnterAnimation = R.anim.fade_in_back;

    private int mPopStackExitAnimation = R.anim.fade_out_front;

    private Runnable mExecPendingTransactions = new Runnable() {
        @Override
        public void run() {
            LogWrapper.d("FragmentStack", "Executing pending transactions");
            if (mFragmentTransaction != null) {
                mFragmentTransaction.commit();
                mFragmentManager.executePendingTransactions();
                mFragmentTransaction = null;

                dispatchOnStackChangedEvent();
            }
        }
    };

    private FragmentStack(FragmentActivity activity, int containerId, Callback<T> callback) {
        mActivity = activity;
        mFragmentManager = activity.getSupportFragmentManager();
        mContainerId = containerId;
        mCallback = callback;

        mHandler = new Handler();
    }

    public void attach() {
        T f = mStack.peekLast();
        if (f != null) {
            attachFragment(f, f.getTag());
            mFragmentTransaction.commit();
        }
    }

    public void detach() {
        T f = mStack.peekLast();
        if (f != null) {
            detachFragment(f);
            mFragmentTransaction.commit();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        executePendingTransactions();

        final int stackSize = mStack.size();
        String[] stackTags = new String[stackSize];

        int i = 0;
        for (T f : mStack) {
            stackTags[i++] = f.getTag();
        }

        outState.putStringArray(STATE_STACK, stackTags);
    }

    public void onRestoreInstanceState(Bundle state) {
        String[] stackTags = state.getStringArray(STATE_STACK);
        for (String tag : stackTags) {
            T f = (T) mFragmentManager.findFragmentByTag(tag);
            mStack.add(f);
        }
        dispatchOnStackChangedEvent();
    }

    public int getStackSize() {
        return mStack.size();
    }

    public <T> T getFragment(String tag) {
        return (T) mFragmentManager.findFragmentByTag(tag);
    }

    public T getTopFragment() {
        return mStack.peekLast();
    }

    public void setTopFragment(Class fragment, String tag) {
        setTopFragment(fragment, tag, null);
    }

    /**
     * Clears the stack and displays the fragment.
     *
     * @param fragment The fragment to display.
     * @param tag      The tag of the fragment.
     */
    public void setTopFragment(Class fragment, String tag, Bundle args) {
        Fragment first = mStack.peekFirst();
        if (first != null && tag.equals(first.getTag())) {
            while (mStack.size() > 1) {
                popStack();
            }
            return;
        }

        T f = (T) mFragmentManager.findFragmentByTag(tag);
        if (f == null) {
            f = (T) Fragment.instantiate(mActivity, fragment.getName(), args);
        }

        ensureTransaction();
        mFragmentTransaction.setCustomAnimations(mEnterAnimation, mExitAnimation);
        clearStack();
        attachFragment(f, tag);
        mStack.add(f);
    }

    public void addFragment(Class fragment, String tag) {
        addFragment(fragment, tag, null);
    }

    /**
     * Adds a new fragment to the stack and displays it.
     *
     * @param fragment The fragment to display.
     * @param tag      The tag of the fragment.
     */
    public void addFragment(Class fragment, String tag, Bundle args) {
        ensureTransaction();
        mFragmentTransaction.setCustomAnimations(mEnterAnimation, mExitAnimation);
        detachTop();

        T f = (T) mFragmentManager.findFragmentByTag(tag);

        if (f == null) {
            f = (T) Fragment.instantiate(mActivity, fragment.getName(), args);
        }

        attachFragment(f, tag);
        mStack.add(f);
    }

    private void detachTop() {
        T f = mStack.peekLast();
        detachFragment(f);
    }

    /**
     * Removes the fragment at the top of the stack and displays the previous one. This will not do anything if there is
     * only one fragment in the stack.
     *
     * @return Whether a transaction has been enqueued.
     */
    public boolean popStack() {
        return popStack(false);
    }

    /**
     * Removes the fragment at the top of the stack and displays the previous one. This will not do anything if there is
     * only one fragment in the stack.
     *
     * @param commit Whether the transaction should be committed.
     * @return Whether a transaction has been enqueued.
     */
    public boolean popStack(boolean commit) {
        LogWrapper.d("FragmentStack", "Stack size: " + mStack.size());
        if (mStack.size() > 1) {
            ensureTransaction();
            mFragmentTransaction.setCustomAnimations(mPopStackEnterAnimation, mPopStackExitAnimation);
            removeFragment(mStack.pollLast());
            Fragment f = mStack.peekLast();
            attachFragment(f, f.getTag());

            if (commit) commit();

            return true;
        }

        return false;
    }

    /**
     * Removes all fragment in the stack. The fragment at the base of the stack will stay added to the
     * {@link FragmentManager}, but its view will be detached.
     */
    private void clearStack() {
        Fragment first = mStack.peekFirst();
        for (Fragment f : mStack) {
            if (f == first) {
                detachFragment(f);
            } else {
                removeFragment(f);
            }
        }

        mStack.clear();
    }

    private void dispatchOnStackChangedEvent() {
        if (mCallback != null && mStack.size() > 0) {
            mCallback.onStackChanged(mStack.size(), mStack.peekLast());
        }
    }

    private FragmentTransaction ensureTransaction() {
        if (mFragmentTransaction == null) mFragmentTransaction = mFragmentManager.beginTransaction();
        mHandler.removeCallbacks(mExecPendingTransactions);
        return mFragmentTransaction;
    }

    private void attachFragment(Fragment fragment, String tag) {
        if (fragment != null) {
            if (fragment.isDetached()) {
                ensureTransaction();

                mFragmentTransaction.attach(fragment);
            } else if (!fragment.isAdded()) {
                ensureTransaction();

                mFragmentTransaction.add(mContainerId, fragment, tag);
            }

        }
    }

    private void detachFragment(Fragment fragment) {
        if (fragment != null && !fragment.isDetached()) {
            ensureTransaction();
            mFragmentTransaction.detach(fragment);
        }
    }

    private void removeFragment(Fragment fragment) {
        if (fragment != null && fragment.isAdded()) {
            ensureTransaction();
            mFragmentTransaction.remove(fragment);
        }
    }

    public void setAnimation(int enter, int exit) {
        ensureTransaction();
        mFragmentTransaction.setCustomAnimations(enter, exit);
    }

    public void setAnimation(int enter, int exit, int popEnter, int popExit) {
        ensureTransaction();
        mFragmentTransaction.setCustomAnimations(enter, exit, popEnter, popExit);
    }

    /**
     * Commit pending transactions. This will be posted, not executed immediately.
     *
     * @return Whether there were any transactions to commit.
     */
    public boolean commit() {
        if (mFragmentTransaction != null && !mFragmentTransaction.isEmpty()) {
            mHandler.removeCallbacks(mExecPendingTransactions);
            mHandler.post(mExecPendingTransactions);
            return true;
        }

        return false;
    }

    public boolean executePendingTransactions() {
        if (mFragmentTransaction != null && !mFragmentTransaction.isEmpty()) {
            mHandler.removeCallbacks(mExecPendingTransactions);
            mFragmentTransaction.commit();
            boolean result = mFragmentManager.executePendingTransactions();
            if (result) {
                dispatchOnStackChangedEvent();
                return true;
            }
        }

        return false;
    }
}
