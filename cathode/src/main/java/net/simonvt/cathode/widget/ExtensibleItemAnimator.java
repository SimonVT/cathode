/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.widget;

import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorCompat;
import android.support.v4.view.ViewPropertyAnimatorListener;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.RecyclerView.ViewHolder;
import android.view.View;
import java.util.ArrayList;

public abstract class ExtensibleItemAnimator extends RecyclerView.ItemAnimator {

  private ArrayList<ViewHolder> mPendingRemovals = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> mPendingAdditions = new ArrayList<ViewHolder>();
  private ArrayList<MoveInfo> mPendingMoves = new ArrayList<MoveInfo>();

  private ArrayList<ViewHolder> mAdditions = new ArrayList<ViewHolder>();
  private ArrayList<MoveInfo> mMoves = new ArrayList<MoveInfo>();

  private ArrayList<ViewHolder> mAddAnimations = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> mMoveAnimations = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> mRemoveAnimations = new ArrayList<ViewHolder>();

  private static class MoveInfo {
    public ViewHolder holder;
    public int fromX, fromY, toX, toY;

    private MoveInfo(ViewHolder holder, int fromX, int fromY, int toX, int toY) {
      this.holder = holder;
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }
  }

  public abstract void beforeAddAnimation(View view);

  public abstract void setupAddAnimation(ViewPropertyAnimatorCompat animator);

  public abstract void onAddAnimationEnd(View view);

  public abstract void onResetAddAnimation(View view);

  public abstract void beforeRemoveAnimation(View view);

  public abstract void setupRemoveAnimation(ViewPropertyAnimatorCompat animator);

  public abstract void onRemoveAnimationEnd(View view);

  public abstract void onResetRemoveAnimation(View view);

  public abstract void beforeMoveAnimation(View view, int fromX, int fromY, int toX, int toY);

  public abstract void setupMoveAnimation(ViewPropertyAnimatorCompat animator, int fromX, int fromY,
      int toX, int toY);

  public abstract void onMoveAnimationEnd(View view);

  public abstract void onResetMoveAnimation(View view);

  @Override
  public void runPendingAnimations() {
    boolean removalsPending = !mPendingRemovals.isEmpty();
    boolean movesPending = !mPendingMoves.isEmpty();
    boolean additionsPending = !mPendingAdditions.isEmpty();
    if (!removalsPending && !movesPending && !additionsPending) {
      // nothing to animate
      return;
    }
    // First, remove stuff
    for (ViewHolder holder : mPendingRemovals) {
      animateRemoveImpl(holder);
    }
    mPendingRemovals.clear();
    // Next, move stuff
    if (movesPending) {
      mMoves.addAll(mPendingMoves);
      mPendingMoves.clear();
      Runnable mover = new Runnable() {
        @Override
        public void run() {
          for (MoveInfo moveInfo : mMoves) {
            animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX,
                moveInfo.toY);
          }
          mMoves.clear();
        }
      };
      if (removalsPending) {
        View view = mMoves.get(0).holder.itemView;
        ViewCompat.postOnAnimationDelayed(view, mover, getRemoveDuration());
      } else {
        mover.run();
      }
    }
    // Next, add stuff
    if (additionsPending) {
      mAdditions.addAll(mPendingAdditions);
      mPendingAdditions.clear();
      Runnable adder = new Runnable() {
        public void run() {
          for (ViewHolder holder : mAdditions) {
            animateAddImpl(holder);
          }
          mAdditions.clear();
        }
      };
      if (removalsPending || movesPending) {
        View view = mAdditions.get(0).itemView;
        ViewCompat.postOnAnimationDelayed(view, adder,
            (removalsPending ? getRemoveDuration() : 0) + (movesPending ? getMoveDuration() : 0));
      } else {
        adder.run();
      }
    }
  }

  @Override
  public boolean animateRemove(final ViewHolder holder) {
    beforeRemoveAnimation(holder.itemView);
    mPendingRemovals.add(holder);
    return true;
  }

  private void animateRemoveImpl(final ViewHolder holder) {
    final View view = holder.itemView;
    ViewCompat.animate(view).cancel();

    setupRemoveAnimation(ViewCompat.animate(view));

    ViewCompat.animate(view).setDuration(getRemoveDuration()).setListener(new VpaListenerAdapter() {
      @Override
      public void onAnimationEnd(View view) {
        onRemoveAnimationEnd(view);
        dispatchRemoveFinished(holder);
        mRemoveAnimations.remove(holder);
        dispatchFinishedWhenDone();
      }
    }).start();
    mRemoveAnimations.add(holder);
  }

  @Override
  public boolean animateAdd(final ViewHolder holder) {
    beforeAddAnimation(holder.itemView);
    mPendingAdditions.add(holder);
    return true;
  }

  private void animateAddImpl(final ViewHolder holder) {
    final View view = holder.itemView;
    ViewCompat.animate(view).cancel();

    setupAddAnimation(ViewCompat.animate(view));

    ViewCompat.animate(view).setDuration(getAddDuration()).
        setListener(new VpaListenerAdapter() {
          @Override
          public void onAnimationCancel(View view) {
            onResetAddAnimation(view);
          }

          @Override
          public void onAnimationEnd(View view) {
            onAddAnimationEnd(view);
            dispatchAddFinished(holder);
            mAddAnimations.remove(holder);
            dispatchFinishedWhenDone();
          }
        }).start();
    mAddAnimations.add(holder);
  }

  @Override
  public boolean animateMove(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    int deltaX = toX - fromX;
    int deltaY = toY - fromY;
    if (deltaX == 0 && deltaY == 0) {
      dispatchMoveFinished(holder);
      return false;
    }

    beforeMoveAnimation(holder.itemView, fromX, fromY, toX, toY);

    mPendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
    return true;
  }

  private void animateMoveImpl(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    final View view = holder.itemView;
    ViewCompat.animate(view).cancel();

    setupMoveAnimation(ViewCompat.animate(view), fromX, fromY, toX, toY);

    // TODO: make EndActions end listeners instead, since end actions aren't called when
    // vpas are canceled (and can't end them. why?)
    // need listener functionality in VPACompat for this. Ick.
    ViewCompat.animate(view).setDuration(getMoveDuration()).setListener(new VpaListenerAdapter() {
      @Override
      public void onAnimationCancel(View view) {
        onResetMoveAnimation(view);
      }

      @Override
      public void onAnimationEnd(View view) {
        onMoveAnimationEnd(view);
        dispatchMoveFinished(holder);
        mMoveAnimations.remove(holder);
        dispatchFinishedWhenDone();
      }
    }).start();
    mMoveAnimations.add(holder);
  }

  @Override
  public void endAnimation(ViewHolder item) {
    final View view = item.itemView;
    ViewCompat.animate(view).cancel();
    if (mPendingMoves.contains(item)) {
      onResetMoveAnimation(item.itemView);
      dispatchMoveFinished(item);
      mPendingMoves.remove(item);
    }
    if (mPendingRemovals.contains(item)) {
      onResetRemoveAnimation(item.itemView);
      dispatchRemoveFinished(item);
      mPendingRemovals.remove(item);
    }
    if (mPendingAdditions.contains(item)) {
      onResetAddAnimation(item.itemView);
      dispatchAddFinished(item);
      mPendingAdditions.remove(item);
    }
    if (mMoveAnimations.contains(item)) {
      onResetMoveAnimation(item.itemView);
      dispatchMoveFinished(item);
      mMoveAnimations.remove(item);
    }
    if (mRemoveAnimations.contains(item)) {
      onResetRemoveAnimation(item.itemView);
      dispatchRemoveFinished(item);
      mRemoveAnimations.remove(item);
    }
    if (mAddAnimations.contains(item)) {
      onResetAddAnimation(item.itemView);
      dispatchAddFinished(item);
      mAddAnimations.remove(item);
    }
    dispatchFinishedWhenDone();
  }

  @Override
  public boolean isRunning() {
    return (!mMoveAnimations.isEmpty()
        || !mRemoveAnimations.isEmpty()
        || !mAddAnimations.isEmpty()
        || !mMoves.isEmpty()
        || !mAdditions.isEmpty());
  }

  /**
   * Check the state of currently pending and running animations. If there are none
   * pending/running, call {@link #dispatchAnimationsFinished()} to notify any
   * listeners.
   */
  private void dispatchFinishedWhenDone() {
    if (!isRunning()) {
      dispatchAnimationsFinished();
    }
  }

  @Override
  public void endAnimations() {
    int count = mPendingMoves.size();
    for (int i = count - 1; i >= 0; i--) {
      MoveInfo item = mPendingMoves.get(i);
      View view = item.holder.itemView;
      ViewCompat.animate(view).cancel();
      onResetMoveAnimation(view);
      dispatchMoveFinished(item.holder);
      mPendingMoves.remove(item);
    }
    count = mPendingRemovals.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mPendingRemovals.get(i);
      onResetRemoveAnimation(item.itemView);
      dispatchRemoveFinished(item);
      mPendingRemovals.remove(item);
    }
    count = mPendingAdditions.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mPendingAdditions.get(i);
      View view = item.itemView;
      onResetAddAnimation(view);
      dispatchAddFinished(item);
      mPendingAdditions.remove(item);
    }
    if (!isRunning()) {
      return;
    }
    count = mMoveAnimations.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mMoveAnimations.get(i);
      View view = item.itemView;
      ViewCompat.animate(view).cancel();
      onResetMoveAnimation(view);
      dispatchMoveFinished(item);
      mMoveAnimations.remove(item);
    }
    count = mRemoveAnimations.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mRemoveAnimations.get(i);
      View view = item.itemView;
      ViewCompat.animate(view).cancel();
      onResetRemoveAnimation(view);
      dispatchRemoveFinished(item);
      mRemoveAnimations.remove(item);
    }
    count = mAddAnimations.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = mAddAnimations.get(i);
      View view = item.itemView;
      ViewCompat.animate(view).cancel();
      onResetAddAnimation(view);
      dispatchAddFinished(item);
      mAddAnimations.remove(item);
    }
    mMoves.clear();
    mAdditions.clear();
    dispatchAnimationsFinished();
  }

  private static class VpaListenerAdapter implements ViewPropertyAnimatorListener {
    @Override
    public void onAnimationStart(View view) {
    }

    @Override
    public void onAnimationEnd(View view) {
    }

    @Override
    public void onAnimationCancel(View view) {
    }
  }
}
