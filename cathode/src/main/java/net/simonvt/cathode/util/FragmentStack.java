package net.simonvt.cathode.util;

import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import java.util.LinkedList;
import net.simonvt.cathode.R;

/** A class that manages a stack of {@link Fragment}s in a single container. */
public final class FragmentStack {

  public interface Callback {

    void onStackChanged(int stackSize, Fragment topFragment);
  }

  public static FragmentStack forContainer(FragmentActivity activity, int containerId,
      Callback callback) {
    return new FragmentStack(activity, containerId, callback);
  }

  private static final String STATE_STACK = "net.simonvt.cathode.util.FragmentStack.stack";

  private LinkedList<Fragment> stack = new LinkedList<Fragment>();

  private Activity activity;

  private FragmentManager fragmentManager;
  private FragmentTransaction fragmentTransaction;

  private int containerId;

  private Callback callback;

  private Handler handler;

  private int enterAnimation = R.anim.fade_in_front;

  private int exitAnimation = R.anim.fade_out_back;

  private int popStackEnterAnimation = R.anim.fade_in_back;

  private int popStackExitAnimation = R.anim.fade_out_front;

  private Runnable execPendingTransactions = new Runnable() {
    @Override
    public void run() {
      LogWrapper.d("FragmentStack", "Executing pending transactions");
      if (fragmentTransaction != null) {
        fragmentTransaction.commit();
        fragmentManager.executePendingTransactions();
        fragmentTransaction = null;

        dispatchOnStackChangedEvent();
      }
    }
  };

  private FragmentStack(FragmentActivity activity, int containerId, Callback callback) {
    this.activity = activity;
    fragmentManager = activity.getSupportFragmentManager();
    this.containerId = containerId;
    this.callback = callback;

    handler = new Handler();
  }

  public void attach() {
    Fragment f = stack.peekLast();
    if (f != null) {
      attachFragment(f, f.getTag());
      fragmentTransaction.commit();
    }
  }

  public void detach() {
    Fragment f = stack.peekLast();
    if (f != null) {
      detachFragment(f);
      fragmentTransaction.commit();
    }
  }

  public void onSaveInstanceState(Bundle outState) {
    executePendingTransactions();

    final int stackSize = stack.size();
    String[] stackTags = new String[stackSize];

    int i = 0;
    for (Fragment f : stack) {
      stackTags[i++] = f.getTag();
    }

    outState.putStringArray(STATE_STACK, stackTags);
  }

  public void onRestoreInstanceState(Bundle state) {
    String[] stackTags = state.getStringArray(STATE_STACK);
    for (String tag : stackTags) {
      Fragment f = fragmentManager.findFragmentByTag(tag);
      stack.add(f);
    }
    dispatchOnStackChangedEvent();
  }

  public int getStackSize() {
    return stack.size();
  }

  @SuppressWarnings("unchecked")
  public <T> T getFragment(String tag) {
    return (T) fragmentManager.findFragmentByTag(tag);
  }

  public Fragment getTopFragment() {
    return stack.peekLast();
  }

  public void setTopFragment(Class fragment, String tag) {
    setTopFragment(fragment, tag, null);
  }

  /**
   * Clears the stack and displays the fragment.
   *
   * @param fragment The fragment to display.
   * @param tag The tag of the fragment.
   */
  public void setTopFragment(Class fragment, String tag, Bundle args) {
    Fragment first = stack.peekFirst();
    if (first != null && tag.equals(first.getTag())) {
      while (stack.size() > 1) {
        popStack();
      }
      return;
    }

    Fragment f = fragmentManager.findFragmentByTag(tag);
    if (f == null) {
      f = Fragment.instantiate(activity, fragment.getName(), args);
    }

    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation);
    clearStack();
    attachFragment(f, tag);
    stack.add(f);
  }

  public void addFragment(Class fragment, String tag) {
    addFragment(fragment, tag, null);
  }

  /**
   * Adds a new fragment to the stack and displays it.
   *
   * @param fragment The fragment to display.
   * @param tag The tag of the fragment.
   */
  public void addFragment(Class fragment, String tag, Bundle args) {
    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation);
    detachTop();

    Fragment f = fragmentManager.findFragmentByTag(tag);

    if (f == null) {
      f = Fragment.instantiate(activity, fragment.getName(), args);
    }

    attachFragment(f, tag);
    stack.add(f);
  }

  private void detachTop() {
    Fragment f = stack.peekLast();
    detachFragment(f);
  }

  /**
   * Removes the fragment at the top of the stack and displays the previous one. This will not do
   * anything if there is
   * only one fragment in the stack.
   *
   * @return Whether a transaction has been enqueued.
   */
  public boolean popStack() {
    return popStack(false);
  }

  /**
   * Removes the fragment at the top of the stack and displays the previous one. This will not do
   * anything if there is
   * only one fragment in the stack.
   *
   * @param commit Whether the transaction should be committed.
   * @return Whether a transaction has been enqueued.
   */
  public boolean popStack(boolean commit) {
    LogWrapper.d("FragmentStack", "Stack size: " + stack.size());
    if (stack.size() > 1) {
      ensureTransaction();
      fragmentTransaction.setCustomAnimations(popStackEnterAnimation, popStackExitAnimation);
      removeFragment(stack.pollLast());
      Fragment f = stack.peekLast();
      attachFragment(f, f.getTag());

      if (commit) commit();

      return true;
    }

    return false;
  }

  /**
   * Removes all fragment in the stack. The fragment at the base of the stack will stay added to
   * the
   * {@link FragmentManager}, but its view will be detached.
   */
  private void clearStack() {
    Fragment first = stack.peekFirst();
    for (Fragment f : stack) {
      if (f == first) {
        detachFragment(f);
      } else {
        removeFragment(f);
      }
    }

    stack.clear();
  }

  private void dispatchOnStackChangedEvent() {
    if (callback != null && stack.size() > 0) {
      callback.onStackChanged(stack.size(), stack.peekLast());
    }
  }

  private FragmentTransaction ensureTransaction() {
    if (fragmentTransaction == null) fragmentTransaction = fragmentManager.beginTransaction();
    handler.removeCallbacks(execPendingTransactions);
    return fragmentTransaction;
  }

  private void attachFragment(Fragment fragment, String tag) {
    if (fragment != null) {
      if (fragment.isDetached()) {
        ensureTransaction();

        fragmentTransaction.attach(fragment);
      } else if (!fragment.isAdded()) {
        ensureTransaction();

        fragmentTransaction.add(containerId, fragment, tag);
      }
    }
  }

  private void detachFragment(Fragment fragment) {
    if (fragment != null && !fragment.isDetached()) {
      ensureTransaction();
      fragmentTransaction.detach(fragment);
    }
  }

  private void removeFragment(Fragment fragment) {
    if (fragment != null && fragment.isAdded()) {
      ensureTransaction();
      fragmentTransaction.remove(fragment);
    }
  }

  public void setAnimation(int enter, int exit) {
    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enter, exit);
  }

  public void setAnimation(int enter, int exit, int popEnter, int popExit) {
    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enter, exit, popEnter, popExit);
  }

  /**
   * Commit pending transactions. This will be posted, not executed immediately.
   *
   * @return Whether there were any transactions to commit.
   */
  public boolean commit() {
    if (fragmentTransaction != null && !fragmentTransaction.isEmpty()) {
      handler.removeCallbacks(execPendingTransactions);
      handler.post(execPendingTransactions);
      return true;
    }

    return false;
  }

  public boolean executePendingTransactions() {
    if (fragmentTransaction != null && !fragmentTransaction.isEmpty()) {
      handler.removeCallbacks(execPendingTransactions);
      fragmentTransaction.commit();
      boolean result = fragmentManager.executePendingTransactions();
      if (result) {
        dispatchOnStackChangedEvent();
        return true;
      }
    }

    return false;
  }
}
