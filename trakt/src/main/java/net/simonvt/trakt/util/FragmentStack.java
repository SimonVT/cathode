package net.simonvt.trakt.util;

import net.simonvt.trakt.R;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
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

    public static <T extends Fragment> FragmentStack<T> forContainer(FragmentManager fragmentManager, int containerId,
            Callback<T> callback) {
        return new FragmentStack<T>(fragmentManager, containerId, callback);
    }

    private static final String STATE_BACKSTACK = "net.simonvt.trakt.util.FragmentStack.backStack";

    private LinkedList<T> mBackStack = new LinkedList<T>();

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

    private FragmentStack(FragmentManager fragmentManager, int containerId, Callback<T> callback) {
        mContainerId = containerId;
        mFragmentManager = fragmentManager;
        mCallback = callback;

        mHandler = new Handler();
    }

    public void attach() {
        T f = mBackStack.peekLast();
        if (f != null) {
            attachFragment(f, f.getTag());
            mFragmentTransaction.commit();
        }
    }

    public void detach() {
        T f = mBackStack.peekLast();
        if (f != null) {
            detachFragment(f);
            mFragmentTransaction.commit();
        }
    }

    public void onSaveInstanceState(Bundle outState) {
        executePendingTransactions();

        final int backStackSize = mBackStack.size();
        String[] backStackTags = new String[backStackSize];

        int i = 0;
        for (T f : mBackStack) {
            backStackTags[i++] = f.getTag();
        }

        outState.putStringArray(STATE_BACKSTACK, backStackTags);
    }

    public void onRestoreInstanceState(Bundle state) {
        String[] backStackTags = state.getStringArray(STATE_BACKSTACK);
        for (String tag : backStackTags) {
            T f = (T) mFragmentManager.findFragmentByTag(tag);
            mBackStack.add(f);
        }
        dispatchOnStackChangedEvent();
    }

    public int getStackSize() {
        return mBackStack.size();
    }

    public T getTopFragment() {
        return mBackStack.peekLast();
    }

    /**
     * Clears the stack and displays the fragment.
     *
     * @param f   The fragment to display.
     * @param tag The tag of the fragment.
     */
    public void setTopFragment(T f, String tag) {
        ensureTransaction();
        mFragmentTransaction.setCustomAnimations(mEnterAnimation, mExitAnimation);
        clearBackStack();
        attachFragment(f, tag);
        mBackStack.add(f);
    }

    /**
     * Adds a new fragment to the stack and displays it.
     *
     * @param f   The fragment to display.
     * @param tag The tag of the fragment.
     */
    public void addFragment(final T f, String tag) {
        ensureTransaction();
        mFragmentTransaction.setCustomAnimations(mEnterAnimation, mExitAnimation);
        detachTop();
        attachFragment(f, tag);
        mBackStack.add(f);
    }

    private void detachTop() {
        T f = mBackStack.peekLast();
        detachFragment(f);
    }

    /**
     * Removes the fragment at the top of the stack and displays the previous one. This will not do anything if there is
     * only one fragment in the stack.
     *
     * @return Whether a transaction has been enqueued.
     */
    public boolean popBackStack() {
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
        LogWrapper.d("FragmentStack", "Stack size: " + mBackStack.size());
        if (mBackStack.size() > 1) {
            ensureTransaction();
            mFragmentTransaction.setCustomAnimations(mPopStackEnterAnimation, mPopStackExitAnimation);
            removeFragment(mBackStack.pollLast());
            T f = mBackStack.peekLast();
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
    private void clearBackStack() {
        for (T f : mBackStack) {
            removeFragment(f);
        }

        mBackStack.clear();
    }

    private void dispatchOnStackChangedEvent() {
        if (mCallback != null) {
            mCallback.onStackChanged(mBackStack.size(), mBackStack.peekLast());
        }
    }

    private FragmentTransaction ensureTransaction() {
        if (mFragmentTransaction == null) mFragmentTransaction = mFragmentManager.beginTransaction();
        mHandler.removeCallbacks(mExecPendingTransactions);
        return mFragmentTransaction;
    }

    private void attachFragment(T fragment, String tag) {
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

    private void detachFragment(T fragment) {
        if (fragment != null && !fragment.isDetached()) {
            ensureTransaction();
            mFragmentTransaction.detach(fragment);
        }
    }

    private void removeFragment(T fragment) {
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
