/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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

import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.BaseAdapter;
import android.widget.GridView;
import android.widget.ListView;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class AnimatorHelper {

  private static final int REMOVE_DURATION = 250;
  private static final int MOVE_DURATION = 300;
  private static final int FADE_OUT_DURATION = 300;
  private static final int FADE_IN_DURATION = 400;

  public interface Callback {

    void removeItem(int position);

    void onAnimationEnd();
  }

  private AnimatorHelper() {
  }

  public static void removeView(final GridView gridView, final View viewToRemove,
      final Callback callback) {
    gridView.setEnabled(false);

    viewToRemove.animate().setDuration(REMOVE_DURATION).alpha(0.0f).withEndAction(new Runnable() {
      @Override public void run() {
        viewToRemove.setAlpha(1.0f);
        final Map<Long, Integer> itemIdTopMap = new HashMap<Long, Integer>();
        final Map<Long, Integer> itemIdLeftMap = new HashMap<Long, Integer>();

        final BaseAdapter adapter = (BaseAdapter) gridView.getAdapter();

        final int oldFirstVisiblePos = gridView.getFirstVisiblePosition();
        final int columnCount = gridView.getFirstVisiblePosition();
        for (int i = 0; i < gridView.getChildCount(); i++) {
          View child = gridView.getChildAt(i);
          if (child != viewToRemove) {
            int position = oldFirstVisiblePos + i;
            long itemId = adapter.getItemId(position);
            itemIdTopMap.put(itemId, child.getTop());
            itemIdLeftMap.put(itemId, child.getLeft());
          }
        }

        int position = gridView.getPositionForView(viewToRemove);
        callback.removeItem(position);
        adapter.notifyDataSetChanged();

        final ViewTreeObserver observer = gridView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
          @Override public boolean onPreDraw() {
            observer.removeOnPreDrawListener(this);
            boolean firstAnim = true;
            final List<View> fadeIn = new ArrayList<View>();
            final int firstVisiblePosition = gridView.getFirstVisiblePosition();

            int childCount = gridView.getChildCount();
            for (int i = 0; i < childCount; i++) {
              final View child = gridView.getChildAt(i);
              int position = firstVisiblePosition + i;
              long itemId = adapter.getItemId(position);
              Integer startTop = itemIdTopMap.get(itemId);
              Integer startLeft = itemIdLeftMap.get(itemId);

              if (startTop == null && i < columnCount) {
                final int childHeight = child.getHeight();
                startTop = child.getTop() - childHeight;
                startLeft = child.getLeft();
              }

              if (startTop != null) {
                int top = child.getTop();
                int left = child.getLeft();

                if (left <= startLeft) {
                  int dX = startLeft - left;
                  int dY = startTop - top;

                  child.setTranslationX(dX);
                  child.setTranslationY(dY);
                  child.animate().setDuration(MOVE_DURATION).translationX(0).translationY(0);
                  if (firstAnim) {
                    firstAnim = false;
                    child.animate().withEndAction(new Runnable() {
                      @Override public void run() {
                        if (!fadeIn.isEmpty()) {
                          for (View v : fadeIn) {
                            v.animate()
                                .setDuration(FADE_IN_DURATION)
                                .alpha(1.0f)
                                .withEndAction(new Runnable() {
                                  @Override public void run() {
                                    gridView.setEnabled(true);
                                    callback.onAnimationEnd();
                                  }
                                });
                          }
                        } else {
                          gridView.setEnabled(true);
                          callback.onAnimationEnd();
                        }
                      }
                    });
                  }
                } else {
                  int dX = startLeft - left;
                  int dY = startTop - top;

                  child.setTranslationX(dX);
                  child.setTranslationY(dY);
                  child.animate()
                      .alpha(0.0f)
                      .setDuration(FADE_OUT_DURATION)
                      .withEndAction(new Runnable() {
                        @Override public void run() {
                          child.setTranslationX(0.0f);
                          child.setTranslationY(0.0f);
                          child.animate().alpha(1.0f).setDuration(FADE_IN_DURATION);
                        }
                      });
                }
              } else {
                child.setAlpha(0.0f);
                fadeIn.add(child);
              }
            }

            itemIdTopMap.clear();
            itemIdLeftMap.clear();
            return true;
          }
        });
      }
    });
  }

  public static void removeView(final ListView listView, final View viewToRemove,
      final Callback callback) {
    listView.setEnabled(false);

    viewToRemove.animate().setDuration(REMOVE_DURATION).alpha(0.0f).withEndAction(new Runnable() {
      @Override public void run() {
        viewToRemove.setAlpha(1.0f);
        viewToRemove.setTranslationX(0);

        final Map<Long, Integer> itemIdTopMap = new HashMap<Long, Integer>();

        final BaseAdapter adapter = (BaseAdapter) listView.getAdapter();

        int firstVisiblePosition = listView.getFirstVisiblePosition();
        for (int i = 0; i < listView.getChildCount(); ++i) {
          View child = listView.getChildAt(i);
          if (child != viewToRemove) {
            int position = firstVisiblePosition + i;
            long itemId = adapter.getItemId(position);
            itemIdTopMap.put(itemId, child.getTop());
          }
        }
        // Delete the item from the adapter
        int position = listView.getPositionForView(viewToRemove);
        callback.removeItem(position);
        adapter.notifyDataSetChanged();

        final ViewTreeObserver observer = listView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
          public boolean onPreDraw() {
            observer.removeOnPreDrawListener(this);
            boolean firstAnimation = true;
            int firstVisiblePosition = listView.getFirstVisiblePosition();
            for (int i = 0; i < listView.getChildCount(); ++i) {
              final View child = listView.getChildAt(i);
              int position = firstVisiblePosition + i;
              long itemId = adapter.getItemId(position);
              Integer startTop = itemIdTopMap.get(itemId);
              int top = child.getTop();
              if (startTop != null) {
                if (startTop != top) {
                  int delta = startTop - top;
                  child.setTranslationY(delta);
                  child.animate().setDuration(MOVE_DURATION).translationY(0);
                  if (firstAnimation) {
                    child.animate().withEndAction(new Runnable() {
                      public void run() {
                        callback.onAnimationEnd();
                        listView.setEnabled(true);
                      }
                    });
                    firstAnimation = false;
                  }
                }
              } else {
                int childHeight = child.getHeight() + listView.getDividerHeight();
                startTop = top + (i > 0 ? childHeight : -childHeight);
                int delta = startTop - top;
                child.setTranslationY(delta);
                child.animate().setDuration(MOVE_DURATION).translationY(0);
                if (firstAnimation) {
                  child.animate().withEndAction(new Runnable() {
                    public void run() {
                      callback.onAnimationEnd();
                      listView.setEnabled(true);
                    }
                  });
                  firstAnimation = false;
                }
              }
            }
            return true;
          }
        });
      }
    });
  }

  public static void removeView(final StaggeredGridView gridView, final View viewToRemove,
      final Callback callback) {
    gridView.setEnabled(false);

    viewToRemove.animate().setDuration(REMOVE_DURATION).alpha(0.0f).withEndAction(new Runnable() {
      @Override public void run() {
        viewToRemove.setAlpha(1.0f);
        final Map<Long, Integer> itemIdTopMap = new HashMap<Long, Integer>();
        final Map<Long, Integer> itemIdLeftMap = new HashMap<Long, Integer>();

        final BaseAdapter adapter = (BaseAdapter) gridView.getAdapter();

        final int oldFirstVisiblePos = gridView.getFirstPosition();
        final int columnCount = gridView.getColumnCount();
        for (int i = 0; i < gridView.getChildCount(); i++) {
          View child = gridView.getChildAt(i);
          if (child != viewToRemove) {
            int position = oldFirstVisiblePos + i;
            long itemId = adapter.getItemId(position);
            itemIdTopMap.put(itemId, child.getTop());
            itemIdLeftMap.put(itemId, child.getLeft());
          }
        }

        int position = gridView.getPositionForView(viewToRemove);
        callback.removeItem(position);
        adapter.notifyDataSetChanged();

        final ViewTreeObserver observer = gridView.getViewTreeObserver();
        observer.addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
          @Override public boolean onPreDraw() {
            observer.removeOnPreDrawListener(this);
            boolean firstAnim = true;
            final List<View> fadeIn = new ArrayList<View>();
            final int firstVisiblePosition = gridView.getFirstPosition();

            int childCount = gridView.getChildCount();
            for (int i = 0; i < childCount; i++) {
              final View child = gridView.getChildAt(i);
              int position = firstVisiblePosition + i;
              long itemId = adapter.getItemId(position);
              Integer startTop = itemIdTopMap.get(itemId);
              Integer startLeft = itemIdLeftMap.get(itemId);

              if (startTop == null && i < columnCount) {
                final int childHeight = child.getHeight();
                startTop = child.getTop() - childHeight;
                startLeft = child.getLeft();
              }

              if (startTop != null) {
                int top = child.getTop();
                int left = child.getLeft();

                if (left <= startLeft) {
                  int dX = startLeft - left;
                  int dY = startTop - top;

                  child.setTranslationX(dX);
                  child.setTranslationY(dY);
                  child.animate().setDuration(MOVE_DURATION).translationX(0).translationY(0);
                  if (firstAnim) {
                    firstAnim = false;
                    child.animate().withEndAction(new Runnable() {
                      @Override public void run() {
                        if (!fadeIn.isEmpty()) {
                          for (View v : fadeIn) {
                            v.animate()
                                .setDuration(FADE_IN_DURATION)
                                .alpha(1.0f)
                                .withEndAction(new Runnable() {
                                  @Override public void run() {
                                    gridView.setEnabled(true);
                                    callback.onAnimationEnd();
                                  }
                                });
                          }
                        } else {
                          gridView.setEnabled(true);
                          callback.onAnimationEnd();
                        }
                      }
                    });
                  }
                } else {
                  int dX = startLeft - left;
                  int dY = startTop - top;

                  child.setTranslationX(dX);
                  child.setTranslationY(dY);
                  child.animate()
                      .alpha(0.0f)
                      .setDuration(FADE_OUT_DURATION)
                      .withEndAction(new Runnable() {
                        @Override public void run() {
                          child.setTranslationX(0.0f);
                          child.setTranslationY(0.0f);
                          child.animate().alpha(1.0f).setDuration(FADE_IN_DURATION);
                        }
                      });
                }
              } else {
                child.setAlpha(0.0f);
                fadeIn.add(child);
              }
            }

            itemIdTopMap.clear();
            itemIdLeftMap.clear();
            return true;
          }
        });
      }
    });
  }
}
