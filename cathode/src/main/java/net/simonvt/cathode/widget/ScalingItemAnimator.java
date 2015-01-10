/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import java.util.List;
import net.simonvt.cathode.BuildConfig;

/**
 * This implementation of {@link android.support.v7.widget.RecyclerView.ItemAnimator} provides
 * basic
 * animations on remove, add, and move events that happen to the items in
 * a RecyclerView. RecyclerView uses a DefaultItemAnimator by default.
 *
 * @see android.support.v7.widget.RecyclerView#setItemAnimator(android.support.v7.widget.RecyclerView.ItemAnimator)
 */
public class ScalingItemAnimator extends RecyclerView.ItemAnimator {

  private static final boolean DEBUG = BuildConfig.DEBUG;

  private ArrayList<ViewHolder> pendingRemovals = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> pendingAdditions = new ArrayList<ViewHolder>();
  private ArrayList<MoveInfo> pendingMoves = new ArrayList<MoveInfo>();
  private ArrayList<ChangeInfo> pendingChanges = new ArrayList<ChangeInfo>();

  private ArrayList<ArrayList<ViewHolder>> additionsList = new ArrayList<ArrayList<ViewHolder>>();
  private ArrayList<ArrayList<MoveInfo>> movesList = new ArrayList<ArrayList<MoveInfo>>();
  private ArrayList<ArrayList<ChangeInfo>> changesList = new ArrayList<ArrayList<ChangeInfo>>();

  private ArrayList<ViewHolder> addAnimations = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> moveAnimations = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> removeAnimations = new ArrayList<ViewHolder>();
  private ArrayList<ViewHolder> changeAnimations = new ArrayList<ViewHolder>();

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

  private static class ChangeInfo {
    public ViewHolder oldHolder, newHolder;
    public int fromX, fromY, toX, toY;

    private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder) {
      this.oldHolder = oldHolder;
      this.newHolder = newHolder;
    }

    private ChangeInfo(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY, int toX,
        int toY) {
      this(oldHolder, newHolder);
      this.fromX = fromX;
      this.fromY = fromY;
      this.toX = toX;
      this.toY = toY;
    }

    @Override
    public String toString() {
      return "ChangeInfo{"
          + "oldHolder="
          + oldHolder
          +
          ", newHolder="
          + newHolder
          + ", fromX="
          + fromX
          + ", fromY="
          + fromY
          + ", toX="
          + toX
          + ", toY="
          + toY
          + '}';
    }
  }

  public ScalingItemAnimator() {
    if (DEBUG) {
      setRemoveDuration(500);
      setMoveDuration(500);
      setAddDuration(500);
      setChangeDuration(500);
    }
  }

  @Override
  public void runPendingAnimations() {
    boolean removalsPending = !pendingRemovals.isEmpty();
    boolean movesPending = !pendingMoves.isEmpty();
    boolean changesPending = !pendingChanges.isEmpty();
    boolean additionsPending = !pendingAdditions.isEmpty();
    if (!removalsPending && !movesPending && !additionsPending && !changesPending) {
      // nothing to animate
      return;
    }
    // First, remove stuff
    for (ViewHolder holder : pendingRemovals) {
      animateRemoveImpl(holder);
    }
    pendingRemovals.clear();
    // Next, move stuff
    if (movesPending) {
      final ArrayList<MoveInfo> moves = new ArrayList<MoveInfo>();
      moves.addAll(pendingMoves);
      movesList.add(moves);
      pendingMoves.clear();
      Runnable mover = new Runnable() {
        @Override
        public void run() {
          for (MoveInfo moveInfo : moves) {
            animateMoveImpl(moveInfo.holder, moveInfo.fromX, moveInfo.fromY, moveInfo.toX,
                moveInfo.toY);
          }
          moves.clear();
          movesList.remove(moves);
        }
      };
      if (removalsPending) {
        View view = moves.get(0).holder.itemView;
        ViewCompat.postOnAnimationDelayed(view, mover, getRemoveDuration());
      } else {
        mover.run();
      }
    }
    // Next, change stuff, to run in parallel with move animations
    if (changesPending) {
      final ArrayList<ChangeInfo> changes = new ArrayList<ChangeInfo>();
      changes.addAll(pendingChanges);
      changesList.add(changes);
      pendingChanges.clear();
      Runnable changer = new Runnable() {
        @Override
        public void run() {
          for (ChangeInfo change : changes) {
            animateChangeImpl(change);
          }
          changes.clear();
          changesList.remove(changes);
        }
      };
      if (removalsPending) {
        ViewHolder holder = changes.get(0).oldHolder;
        ViewCompat.postOnAnimationDelayed(holder.itemView, changer, getRemoveDuration());
      } else {
        changer.run();
      }
    }
    // Next, add stuff
    if (additionsPending) {
      final ArrayList<ViewHolder> additions = new ArrayList<ViewHolder>();
      additions.addAll(pendingAdditions);
      additionsList.add(additions);
      pendingAdditions.clear();
      Runnable adder = new Runnable() {
        public void run() {
          for (ViewHolder holder : additions) {
            animateAddImpl(holder);
          }
          additions.clear();
          additionsList.remove(additions);
        }
      };
      if (removalsPending || movesPending || changesPending) {
        long removeDuration = removalsPending ? getRemoveDuration() : 0;
        long moveDuration = movesPending ? getMoveDuration() : 0;
        long changeDuration = changesPending ? getChangeDuration() : 0;
        long totalDelay = removeDuration + Math.max(moveDuration, changeDuration);
        View view = additions.get(0).itemView;
        ViewCompat.postOnAnimationDelayed(view, adder, totalDelay);
      } else {
        adder.run();
      }
    }
  }

  @Override
  public boolean animateRemove(final ViewHolder holder) {
    endAnimation(holder);
    pendingRemovals.add(holder);
    return true;
  }

  private void animateRemoveImpl(final ViewHolder holder) {
    final View view = holder.itemView;
    final ViewPropertyAnimatorCompat animation = ViewCompat.animate(view);
    animation.setDuration(getRemoveDuration())
        .alpha(0)
        .scaleY(0.5f)
        .setListener(new VpaListenerAdapter() {
          @Override
          public void onAnimationStart(View view) {
            dispatchRemoveStarting(holder);
          }

          @Override
          public void onAnimationEnd(View view) {
            animation.setListener(null);
            ViewCompat.setAlpha(view, 1);
            ViewCompat.setScaleY(view, 1);
            dispatchRemoveFinished(holder);
            removeAnimations.remove(holder);
            dispatchFinishedWhenDone();
          }
        })
        .start();
    removeAnimations.add(holder);
  }

  @Override
  public boolean animateAdd(final ViewHolder holder) {
    endAnimation(holder);
    ViewCompat.setAlpha(holder.itemView, 0);
    ViewCompat.setScaleY(holder.itemView, 0.5f);
    pendingAdditions.add(holder);
    return true;
  }

  private void animateAddImpl(final ViewHolder holder) {
    final View view = holder.itemView;
    addAnimations.add(holder);
    final ViewPropertyAnimatorCompat animation = ViewCompat.animate(view);
    animation.alpha(1).scaleY(1).setDuration(getAddDuration()).
        setListener(new VpaListenerAdapter() {
          @Override
          public void onAnimationStart(View view) {
            dispatchAddStarting(holder);
          }

          @Override
          public void onAnimationCancel(View view) {
            ViewCompat.setAlpha(view, 1);
            ViewCompat.setScaleY(view, 1);
          }

          @Override
          public void onAnimationEnd(View view) {
            animation.setListener(null);
            dispatchAddFinished(holder);
            addAnimations.remove(holder);
            dispatchFinishedWhenDone();
          }
        }).start();
  }

  @Override
  public boolean animateMove(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    final View view = holder.itemView;
    fromX += ViewCompat.getTranslationX(holder.itemView);
    fromY += ViewCompat.getTranslationY(holder.itemView);
    endAnimation(holder);
    int deltaX = toX - fromX;
    int deltaY = toY - fromY;
    if (deltaX == 0 && deltaY == 0) {
      dispatchMoveFinished(holder);
      return false;
    }
    if (deltaX != 0) {
      ViewCompat.setTranslationX(view, -deltaX);
    }
    if (deltaY != 0) {
      ViewCompat.setTranslationY(view, -deltaY);
    }
    pendingMoves.add(new MoveInfo(holder, fromX, fromY, toX, toY));
    return true;
  }

  private void animateMoveImpl(final ViewHolder holder, int fromX, int fromY, int toX, int toY) {
    final View view = holder.itemView;
    final int deltaX = toX - fromX;
    final int deltaY = toY - fromY;
    if (deltaX != 0) {
      ViewCompat.animate(view).translationX(0);
    }
    if (deltaY != 0) {
      ViewCompat.animate(view).translationY(0);
    }
    // TODO: make EndActions end listeners instead, since end actions aren't called when
    // vpas are canceled (and can't end them. why?)
    // need listener functionality in VPACompat for this. Ick.
    moveAnimations.add(holder);
    final ViewPropertyAnimatorCompat animation = ViewCompat.animate(view);
    animation.setDuration(getMoveDuration()).setListener(new VpaListenerAdapter() {
      @Override
      public void onAnimationStart(View view) {
        dispatchMoveStarting(holder);
      }

      @Override
      public void onAnimationCancel(View view) {
        if (deltaX != 0) {
          ViewCompat.setTranslationX(view, 0);
        }
        if (deltaY != 0) {
          ViewCompat.setTranslationY(view, 0);
        }
      }

      @Override
      public void onAnimationEnd(View view) {
        animation.setListener(null);
        dispatchMoveFinished(holder);
        moveAnimations.remove(holder);
        dispatchFinishedWhenDone();
      }
    }).start();
  }

  @Override
  public boolean animateChange(ViewHolder oldHolder, ViewHolder newHolder, int fromX, int fromY,
      int toX, int toY) {
    final float prevTranslationX = ViewCompat.getTranslationX(oldHolder.itemView);
    final float prevTranslationY = ViewCompat.getTranslationY(oldHolder.itemView);
    final float prevAlpha = ViewCompat.getAlpha(oldHolder.itemView);
    endAnimation(oldHolder);
    int deltaX = (int) (toX - fromX - prevTranslationX);
    int deltaY = (int) (toY - fromY - prevTranslationY);
    // recover prev translation state after ending animation
    ViewCompat.setTranslationX(oldHolder.itemView, prevTranslationX);
    ViewCompat.setTranslationY(oldHolder.itemView, prevTranslationY);
    ViewCompat.setAlpha(oldHolder.itemView, prevAlpha);
    if (newHolder != null && newHolder.itemView != null) {
      // carry over translation values
      endAnimation(newHolder);
      ViewCompat.setTranslationX(newHolder.itemView, -deltaX);
      ViewCompat.setTranslationY(newHolder.itemView, -deltaY);
      ViewCompat.setAlpha(newHolder.itemView, 0);
    }
    pendingChanges.add(new ChangeInfo(oldHolder, newHolder, fromX, fromY, toX, toY));
    return true;
  }

  private void animateChangeImpl(final ChangeInfo changeInfo) {
    final ViewHolder holder = changeInfo.oldHolder;
    final View view = holder.itemView;
    final ViewHolder newHolder = changeInfo.newHolder;
    final View newView = newHolder != null ? newHolder.itemView : null;
    changeAnimations.add(changeInfo.oldHolder);

    final ViewPropertyAnimatorCompat oldViewAnim =
        ViewCompat.animate(view).setDuration(getChangeDuration());
    oldViewAnim.translationX(changeInfo.toX - changeInfo.fromX);
    oldViewAnim.translationY(changeInfo.toY - changeInfo.fromY);
    oldViewAnim.alpha(0).setListener(new VpaListenerAdapter() {
      @Override
      public void onAnimationStart(View view) {
        dispatchChangeStarting(changeInfo.oldHolder, true);
      }

      @Override
      public void onAnimationEnd(View view) {
        oldViewAnim.setListener(null);
        ViewCompat.setAlpha(view, 1);
        ViewCompat.setTranslationX(view, 0);
        ViewCompat.setTranslationY(view, 0);
        dispatchChangeFinished(changeInfo.oldHolder, true);
        changeAnimations.remove(changeInfo.oldHolder);
        dispatchFinishedWhenDone();
      }
    }).start();
    if (newView != null) {
      changeAnimations.add(changeInfo.newHolder);
      final ViewPropertyAnimatorCompat newViewAnimation = ViewCompat.animate(newView);
      newViewAnimation.translationX(0).translationY(0).setDuration(getChangeDuration()).
          alpha(1).setListener(new VpaListenerAdapter() {
        @Override
        public void onAnimationStart(View view) {
          dispatchChangeStarting(changeInfo.newHolder, false);
        }

        @Override
        public void onAnimationEnd(View view) {
          newViewAnimation.setListener(null);
          ViewCompat.setAlpha(newView, 1);
          ViewCompat.setTranslationX(newView, 0);
          ViewCompat.setTranslationY(newView, 0);
          dispatchChangeFinished(changeInfo.newHolder, false);
          changeAnimations.remove(changeInfo.newHolder);
          dispatchFinishedWhenDone();
        }
      }).start();
    }
  }

  private void endChangeAnimation(List<ChangeInfo> infoList, ViewHolder item) {
    for (int i = infoList.size() - 1; i >= 0; i--) {
      ChangeInfo changeInfo = infoList.get(i);
      if (endChangeAnimationIfNecessary(changeInfo, item)) {
        if (changeInfo.oldHolder == null && changeInfo.newHolder == null) {
          infoList.remove(changeInfo);
        }
      }
    }
  }

  private void endChangeAnimationIfNecessary(ChangeInfo changeInfo) {
    if (changeInfo.oldHolder != null) {
      endChangeAnimationIfNecessary(changeInfo, changeInfo.oldHolder);
    }
    if (changeInfo.newHolder != null) {
      endChangeAnimationIfNecessary(changeInfo, changeInfo.newHolder);
    }
  }

  private boolean endChangeAnimationIfNecessary(ChangeInfo changeInfo, ViewHolder item) {
    boolean oldItem = false;
    if (changeInfo.newHolder == item) {
      changeInfo.newHolder = null;
    } else if (changeInfo.oldHolder == item) {
      changeInfo.oldHolder = null;
      oldItem = true;
    } else {
      return false;
    }
    ViewCompat.setAlpha(item.itemView, 1);
    ViewCompat.setTranslationX(item.itemView, 0);
    ViewCompat.setTranslationY(item.itemView, 0);
    dispatchChangeFinished(item, oldItem);
    return true;
  }

  @Override
  public void endAnimation(ViewHolder item) {
    final View view = item.itemView;
    // this will trigger end callback which should set properties to their target values.
    ViewCompat.animate(view).cancel();
    // TODO if some other animations are chained to end, how do we cancel them as well?
    for (int i = pendingMoves.size() - 1; i >= 0; i--) {
      MoveInfo moveInfo = pendingMoves.get(i);
      if (moveInfo.holder == item) {
        ViewCompat.setTranslationY(view, 0);
        ViewCompat.setTranslationX(view, 0);
        dispatchMoveFinished(item);
        pendingMoves.remove(item);
      }
    }
    endChangeAnimation(pendingChanges, item);
    if (pendingRemovals.remove(item)) {
      ViewCompat.setAlpha(view, 1);
      dispatchRemoveFinished(item);
    }
    if (pendingAdditions.remove(item)) {
      ViewCompat.setAlpha(view, 1);
      dispatchAddFinished(item);
    }

    for (int i = changesList.size() - 1; i >= 0; i--) {
      ArrayList<ChangeInfo> changes = changesList.get(i);
      endChangeAnimation(changes, item);
      if (changes.isEmpty()) {
        changesList.remove(changes);
      }
    }
    for (int i = movesList.size() - 1; i >= 0; i--) {
      ArrayList<MoveInfo> moves = movesList.get(i);
      for (int j = moves.size() - 1; j >= 0; j--) {
        MoveInfo moveInfo = moves.get(j);
        if (moveInfo.holder == item) {
          ViewCompat.setTranslationY(view, 0);
          ViewCompat.setTranslationX(view, 0);
          dispatchMoveFinished(item);
          moves.remove(j);
          if (moves.isEmpty()) {
            movesList.remove(moves);
          }
          break;
        }
      }
    }
    for (int i = additionsList.size() - 1; i >= 0; i--) {
      ArrayList<ViewHolder> additions = additionsList.get(i);
      if (additions.remove(item)) {
        ViewCompat.setAlpha(view, 1);
        dispatchAddFinished(item);
        if (additions.isEmpty()) {
          additionsList.remove(additions);
        }
      }
    }

    // animations should be ended by the cancel above.
    if (removeAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "removeAnimations list");
    }

    if (addAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "addAnimations list");
    }

    if (changeAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "changeAnimations list");
    }

    if (moveAnimations.remove(item) && DEBUG) {
      throw new IllegalStateException(
          "after animation is cancelled, item should not be in " + "moveAnimations list");
    }
    dispatchFinishedWhenDone();
  }

  @Override
  public boolean isRunning() {
    return (!pendingAdditions.isEmpty()
        || !pendingChanges.isEmpty()
        || !pendingMoves.isEmpty()
        || !pendingRemovals.isEmpty()
        || !moveAnimations.isEmpty()
        || !removeAnimations.isEmpty()
        || !addAnimations.isEmpty()
        || !changeAnimations.isEmpty()
        || !movesList.isEmpty()
        || !additionsList.isEmpty()
        || !changesList.isEmpty());
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
    int count = pendingMoves.size();
    for (int i = count - 1; i >= 0; i--) {
      MoveInfo item = pendingMoves.get(i);
      View view = item.holder.itemView;
      ViewCompat.setTranslationY(view, 0);
      ViewCompat.setTranslationX(view, 0);
      dispatchMoveFinished(item.holder);
      pendingMoves.remove(i);
    }
    count = pendingRemovals.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = pendingRemovals.get(i);
      dispatchRemoveFinished(item);
      pendingRemovals.remove(i);
    }
    count = pendingAdditions.size();
    for (int i = count - 1; i >= 0; i--) {
      ViewHolder item = pendingAdditions.get(i);
      View view = item.itemView;
      ViewCompat.setAlpha(view, 1);
      dispatchAddFinished(item);
      pendingAdditions.remove(i);
    }
    count = pendingChanges.size();
    for (int i = count - 1; i >= 0; i--) {
      endChangeAnimationIfNecessary(pendingChanges.get(i));
    }
    pendingChanges.clear();
    if (!isRunning()) {
      return;
    }

    int listCount = movesList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<MoveInfo> moves = movesList.get(i);
      count = moves.size();
      for (int j = count - 1; j >= 0; j--) {
        MoveInfo moveInfo = moves.get(j);
        ViewHolder item = moveInfo.holder;
        View view = item.itemView;
        ViewCompat.setTranslationY(view, 0);
        ViewCompat.setTranslationX(view, 0);
        dispatchMoveFinished(moveInfo.holder);
        moves.remove(j);
        if (moves.isEmpty()) {
          movesList.remove(moves);
        }
      }
    }
    listCount = additionsList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<ViewHolder> additions = additionsList.get(i);
      count = additions.size();
      for (int j = count - 1; j >= 0; j--) {
        ViewHolder item = additions.get(j);
        View view = item.itemView;
        ViewCompat.setAlpha(view, 1);
        dispatchAddFinished(item);
        additions.remove(j);
        if (additions.isEmpty()) {
          additionsList.remove(additions);
        }
      }
    }
    listCount = changesList.size();
    for (int i = listCount - 1; i >= 0; i--) {
      ArrayList<ChangeInfo> changes = changesList.get(i);
      count = changes.size();
      for (int j = count - 1; j >= 0; j--) {
        endChangeAnimationIfNecessary(changes.get(j));
        if (changes.isEmpty()) {
          changesList.remove(changes);
        }
      }
    }

    cancelAll(removeAnimations);
    cancelAll(moveAnimations);
    cancelAll(addAnimations);
    cancelAll(changeAnimations);

    dispatchAnimationsFinished();
  }

  void cancelAll(List<ViewHolder> viewHolders) {
    for (int i = viewHolders.size() - 1; i >= 0; i--) {
      ViewCompat.animate(viewHolders.get(i).itemView).cancel();
    }
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
