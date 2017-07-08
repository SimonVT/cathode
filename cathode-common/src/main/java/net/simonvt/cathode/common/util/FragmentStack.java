/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.common.util;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import net.simonvt.cathode.common.util.guava.Preconditions;
import timber.log.Timber;

/** A class that manages a stack of {@link Fragment}s in a single container. */
public final class FragmentStack {

  public interface Callback {

    void onStackChanged(int stackSize, Fragment topFragment);
  }

  public static class StackEntry implements Parcelable {

    Class fragment;

    String tag;

    Bundle args;

    public StackEntry(Class fragment, String tag) {
      this.fragment = fragment;
      this.tag = tag;
    }

    public StackEntry(Class fragment, String tag, Bundle args) {
      this.fragment = fragment;
      this.tag = tag;
      this.args = args;
    }

    public StackEntry(Parcel in) {
      fragment = (Class) in.readValue(getClass().getClassLoader());
      tag = in.readString();
      args = in.readBundle(getClass().getClassLoader());
    }

    @Override public int describeContents() {
      return 0;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      dest.writeValue(fragment);
      dest.writeString(tag);
      dest.writeBundle(args);
    }

    public static final Parcelable.Creator<StackEntry> CREATOR =
        new Parcelable.Creator<StackEntry>() {
          @Override public StackEntry createFromParcel(Parcel in) {
            return new StackEntry(in);
          }

          @Override public StackEntry[] newArray(int size) {
            return new StackEntry[size];
          }
        };
  }

  /** Create an instance for a specific container. */
  public static FragmentStack forContainer(FragmentActivity activity, int containerId) {
    return forContainer(activity, containerId, null);
  }

  /** Create an instance for a specific container. */
  public static FragmentStack forContainer(FragmentActivity activity, int containerId,
      Callback callback) {
    return new FragmentStack(activity, containerId, callback);
  }

  private static final String STATE_STACK = "net.simonvt.util.FragmentStack.stack";

  private LinkedList<Fragment> stack = new LinkedList<>();
  private Set<String> topLevelTags = new HashSet<>();

  private Activity activity;

  private FragmentManager fragmentManager;
  private FragmentTransaction fragmentTransaction;

  private int containerId;

  private Callback callback;

  private int enterAnimation;
  private int exitAnimation;
  private int popStackEnterAnimation;
  private int popStackExitAnimation;

  private boolean paused;

  private FragmentStack(FragmentActivity activity, int containerId, Callback callback) {
    this.activity = activity;
    fragmentManager = activity.getSupportFragmentManager();
    this.containerId = containerId;
    this.callback = callback;
  }

  public void pause() {
    paused = true;
  }

  public void resume() {
    paused = false;
    commit();
  }

  private boolean allowTransactions() {
    if (paused || fragmentManager.isDestroyed()) {
      return false;
    }

    return true;
  }

  public int positionInstack(Fragment fragment) {
    return stack.indexOf(fragment);
  }

  /** Removes all added fragments and clears the stack. */
  public void destroy() {
    commit();

    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation);

    final Fragment topFragment = stack.peekFirst();
    for (Fragment f : stack) {
      if (f != topFragment) removeFragment(f);
    }
    stack.clear();

    for (String tag : topLevelTags) {
      removeFragment(fragmentManager.findFragmentByTag(tag));
    }

    fragmentTransaction.commitNow();
    fragmentTransaction = null;
  }

  public Bundle saveState() {
    commit();

    final int stackSize = stack.size();
    String[] stackTags = new String[stackSize];

    int i = 0;
    for (Fragment f : stack) {
      String tag = f.getTag();
      Preconditions.checkNotNull(tag, "Null tag for Fragment %s", f.getClass().getName());

      stackTags[i++] = tag;
    }

    Bundle outState = new Bundle();
    outState.putStringArray(STATE_STACK, stackTags);
    return outState;
  }

  public void restoreState(Bundle inState) {
    String[] stackTags = inState.getStringArray(STATE_STACK);
    for (String tag : stackTags) {
      Fragment f = fragmentManager.findFragmentByTag(tag);
      stack.add(f);
    }
    dispatchOnStackChangedEvent();
  }

  public int size() {
    return stack.size();
  }

  public Fragment peek() {
    return stack.peekLast();
  }

  public Fragment peekFirst() {
    return stack.peekFirst();
  }

  /** Replaces the entire stack with this fragment. */
  public void replace(Class fragment, String tag) {
    replace(fragment, tag, null);
  }

  /**
   * Replaces the entire stack with this fragment.
   *
   * @param args Arguments to be set on the fragment using {@link Fragment#setArguments(android.os.Bundle)}.
   */
  public void replace(Class fragment, String tag, Bundle args) {
    Preconditions.checkNotNull(tag, "Passed null tag for Fragment %s",
        fragment.getClass().getName());

    if (!allowTransactions()) {
      return;
    }

    Fragment first = stack.peekFirst();
    if (first != null && tag.equals(first.getTag())) {
      if (stack.size() > 1) {
        ensureTransaction();
        fragmentTransaction.setCustomAnimations(popStackEnterAnimation, popStackExitAnimation);
        while (stack.size() > 1) {
          removeFragment(stack.pollLast());
        }

        attachFragment(stack.peek(), tag);
      }

      commit();
      return;
    }

    Fragment f = fragmentManager.findFragmentByTag(tag);
    if (f == null) {
      f = Fragment.instantiate(activity, fragment.getName(), args);
    }

    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation);
    clear();
    attachFragment(f, tag);
    stack.add(f);

    topLevelTags.add(tag);

    commit();
  }

  public void replaceStack(List<StackEntry> stackEntries) {
    if (!allowTransactions()) {
      return;
    }

    ensureTransaction();
    fragmentTransaction.setCustomAnimations(popStackEnterAnimation, popStackExitAnimation);

    final int stackSize = stackEntries.size();

    if (stackSize > 1) {
      while (stack.size() > 1) {
        removeFragment(stack.pollLast());
      }
    }

    StackEntry topLevel = stackEntries.get(0);
    Fragment firstFragment = fragmentManager.findFragmentByTag(topLevel.tag);

    Fragment first = stack.peekFirst();
    if (firstFragment == null) {
      Fragment f = Fragment.instantiate(activity, topLevel.fragment.getName(), topLevel.args);
      attachFragment(f, topLevel.tag);
      commit();

      stack.clear();
      stack.add(f);
    }

    if (stackEntries.size() == 1) {
      if (firstFragment != null) {
        ensureTransaction();
        attachFragment(firstFragment, topLevel.tag);
        commit();

        stack.clear();
        stack.add(firstFragment);
      }
    } else {
      ensureTransaction();
      if (firstFragment == first) {
        detachFragment(first);
      } else {
        detachFragment(first);
        detachFragment(firstFragment);
      }
      commit();

      if (firstFragment != null) {
        stack.clear();
        stack.add(firstFragment);
      }

      for (int i = 1; i < stackSize; i++) {
        StackEntry entry = stackEntries.get(i);
        Fragment f = Fragment.instantiate(activity, entry.fragment.getName(), entry.args);

        ensureTransaction();
        attachFragment(f, entry.tag);
        commit();

        stack.add(f);

        if (i + 1 < stackSize) {
          ensureTransaction();
          detachFragment(f);
          commit();
        }
      }
    }
  }

  public void push(Class fragment, String tag) {
    push(fragment, tag, null);
  }

  /** Adds a new fragment to the stack and displays it. */
  public void push(Class fragment, String tag, Bundle args) {
    Preconditions.checkNotNull(tag, "Passed null tag for Fragment %s",
        fragment.getClass().getName());

    if (!allowTransactions()) {
      return;
    }

    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation);
    detachTop();

    Fragment f = fragmentManager.findFragmentByTag(tag);

    if (f == null) {
      f = Fragment.instantiate(activity, fragment.getName(), args);
    }

    attachFragment(f, tag);
    stack.add(f);

    commit();
  }

  /**
   * Removes the fragment at the top of the stack and displays the previous one. This will not do
   * anything if there is only one fragment in the stack.
   *
   * @return Whether a transaction was committed.
   */
  public boolean pop() {
    if (!allowTransactions()) {
      return false;
    }

    if (stack.size() > 1) {
      ensureTransaction();
      fragmentTransaction.setCustomAnimations(popStackEnterAnimation, popStackExitAnimation);
      removeFragment(stack.pollLast());
      Fragment f = stack.peekLast();
      attachFragment(f, f.getTag());

      commit();

      return true;
    }

    return false;
  }

  /**
   * Removes the top fragment if there are more than one in the stack.
   *
   * @return Whether a transaction was committed.
   */
  public boolean removeTop() {
    if (!allowTransactions()) {
      return false;
    }

    if (stack.size() > 1) {
      ensureTransaction();
      fragmentTransaction.setCustomAnimations(popStackEnterAnimation, popStackExitAnimation);
      removeFragment(stack.pollLast());
      commit();

      return true;
    }

    return false;
  }

  /**
   * Adds a fragment to the top of the stack and attaches it.
   */
  public void putFragment(Class fragment, String tag, Bundle args) {
    Preconditions.checkNotNull(tag, "Passed null tag for Fragment %s",
        fragment.getClass().getName());

    if (!allowTransactions()) {
      return;
    }

    ensureTransaction();
    fragmentTransaction.setCustomAnimations(enterAnimation, exitAnimation);

    if (fragmentManager.findFragmentByTag(tag) != null) {
      throw new IllegalStateException("Fragment with tag " + tag + " already exists");
    }

    Fragment f = Fragment.instantiate(activity, fragment.getName(), args);

    attachFragment(f, tag);
    stack.add(f);

    commit();
  }

  public void attachTop() {
    Fragment f = stack.peekLast();
    attachFragment(f, f.getTag());
  }

  private void detachTop() {
    Fragment f = stack.peekLast();
    detachFragment(f);
  }

  private void clear() {
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

  @SuppressLint("CommitTransaction") private FragmentTransaction ensureTransaction() {
    if (fragmentTransaction == null) {
      fragmentTransaction = fragmentManager.beginTransaction();
    }

    return fragmentTransaction;
  }

  private void attachFragment(Fragment fragment, String tag) {
    Preconditions.checkNotNull(tag, "Passed null tag for Fragment %s",
        fragment.getClass().getName());

    Timber.d("Attaching fragment: %s", tag);

    if (fragment.isDetached()) {
      ensureTransaction();

      fragmentTransaction.attach(fragment);
    } else if (!fragment.isAdded()) {
      ensureTransaction();

      fragmentTransaction.add(containerId, fragment, tag);
    }
  }

  private void detachFragment(Fragment fragment) {
    if (fragment != null && !fragment.isDetached()) {
      Timber.d("Detaching fragment: %s", fragment.getTag());

      ensureTransaction();
      fragmentTransaction.detach(fragment);
    }
  }

  private void removeFragment(Fragment fragment) {
    if (fragment != null && (fragment.isAdded() || fragment.isDetached())) {
      Timber.d("Removing fragment: %s", fragment.getTag());

      ensureTransaction();
      fragmentTransaction.remove(fragment);
    }
  }

  public void setDefaultAnimation(int enter, int exit, int popEnter, int popExit) {
    enterAnimation = enter;
    exitAnimation = exit;
    popStackEnterAnimation = popEnter;
    popStackExitAnimation = popExit;
  }

  public void commit() {
    if (!allowTransactions()) {
      return;
    }

    if (fragmentTransaction != null && !fragmentTransaction.isEmpty()) {
      fragmentTransaction.commitNow();
    }

    fragmentTransaction = null;
  }
}
