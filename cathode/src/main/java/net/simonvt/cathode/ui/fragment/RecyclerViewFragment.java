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

package net.simonvt.cathode.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import butterknife.InjectView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.widget.ScalingItemAnimator;

public abstract class RecyclerViewFragment<T extends RecyclerView.ViewHolder> extends BaseFragment {

  private static final String SAVED_EMPTY_TEXT = "savedEmptyText";

  private static final int STATE_NONE = -1;
  private static final int STATE_PROGRESS_VISIBLE = 0;
  private static final int STATE_CONTENT_VISIBLE = 1;

  private static final int ANIMATION_DURATION = 600;

  private RecyclerView.Adapter<T> adapter;

  @InjectView(R.id.progressContainer) View progressContainer;
  @InjectView(R.id.listContainer) View listContainer;
  @InjectView(android.R.id.list) RecyclerView recyclerView;
  @InjectView(android.R.id.empty) TextView empty;

  private Context appContext;

  private String emptyText;

  private boolean animating;

  private int currentState = STATE_PROGRESS_VISIBLE;
  private int pendingStateChange = STATE_NONE;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    appContext = getActivity().getApplicationContext();

    if (inState != null) {
      emptyText = inState.getString(SAVED_EMPTY_TEXT);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    outState.putString(SAVED_EMPTY_TEXT, emptyText);
    super.onSaveInstanceState(outState);
  }

  protected RecyclerView.ItemAnimator getItemAnimator() {
    return new ScalingItemAnimator();
  }

  protected void addItemDecorations(RecyclerView recyclerView) {
  }

  protected abstract RecyclerView.LayoutManager getLayoutManager();

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_recyclerview, container, false);
  }

  public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    recyclerView.setLayoutManager(getLayoutManager());
    RecyclerView.ItemAnimator itemAnimator = getItemAnimator();
    if (itemAnimator != null) {
      recyclerView.setItemAnimator(itemAnimator);
    }
    addItemDecorations(recyclerView);

    if (empty != null) {
      if (emptyText != null) {
        empty.setText(emptyText);
      }

      if (adapter != null && adapter.getItemCount() > 0) {
        empty.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
      } else {
        empty.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
      }
    }

    if (adapter != null) {
      recyclerView.setAdapter(adapter);
    }

    view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
  }

  ViewTreeObserver.OnGlobalLayoutListener layoutListener =
      new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override public void onGlobalLayout() {
          getView().getViewTreeObserver().removeOnGlobalLayoutListener(layoutListener);
          if (adapter == null) {
            listContainer.setVisibility(View.GONE);
            progressContainer.setVisibility(View.VISIBLE);
            currentState = STATE_PROGRESS_VISIBLE;
          } else {
            currentState = STATE_CONTENT_VISIBLE;
            listContainer.setVisibility(View.VISIBLE);
            progressContainer.setVisibility(View.GONE);
          }
        }
      };

  @Override public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    Animation animation = null;
    if (nextAnim != 0) {
      animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
      animation.setAnimationListener(new Animation.AnimationListener() {
        @Override public void onAnimationStart(Animation animation) {
          animating = true;
        }

        @Override public void onAnimationEnd(Animation animation) {
          animating = false;
          if (pendingStateChange != STATE_NONE) {
            changeState(pendingStateChange, true);
            pendingStateChange = STATE_NONE;
          }
        }

        @Override public void onAnimationRepeat(Animation animation) {
        }
      });
    }

    return animation;
  }

  private void changeState(final int newState, final boolean animate) {
    if (newState == currentState) {
      return;
    }

    if (animating) {
      pendingStateChange = newState;
      return;
    }

    currentState = newState;

    if (newState == STATE_PROGRESS_VISIBLE && listContainer.getVisibility() != View.VISIBLE) {
      return;
    }

    if (newState == STATE_CONTENT_VISIBLE && !animate) {
      listContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.GONE);
    } else if (newState == STATE_PROGRESS_VISIBLE && !animate) {
      listContainer.setVisibility(View.GONE);
      progressContainer.setVisibility(View.VISIBLE);
    } else {
      listContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.VISIBLE);

      final Animation fadeIn = new AlphaAnimation(0.0f, 1.0f);
      fadeIn.setDuration(ANIMATION_DURATION);
      final Animation fadeOut = new AlphaAnimation(1.0f, 0.0f);
      fadeOut.setDuration(ANIMATION_DURATION);
      fadeOut.setAnimationListener(new Animation.AnimationListener() {
        @Override public void onAnimationStart(Animation animation) {
        }

        @Override public void onAnimationEnd(Animation animation) {
          if (progressContainer == null) {
            // In case fragment is removed before animation is done
            return;
          }
          if (newState == STATE_CONTENT_VISIBLE) {
            progressContainer.setVisibility(View.GONE);
          } else {
            listContainer.setVisibility(View.GONE);
            if (adapter == null) {
              recyclerView.setAdapter(null);
            }
          }
        }

        @Override public void onAnimationRepeat(Animation animation) {
        }
      });

      getView().postOnAnimation(new Runnable() {
        @Override public void run() {
          if (listContainer != null) {
            listContainer.startAnimation(newState == STATE_CONTENT_VISIBLE ? fadeIn : fadeOut);
            progressContainer.startAnimation(newState == STATE_CONTENT_VISIBLE ? fadeOut : fadeIn);
          }
        }
      });
    }
  }

  public RecyclerView getRecyclerView() {
    return recyclerView;
  }

  public void setAdapter(RecyclerView.Adapter<T> adapter) {
    if (adapter != this.adapter) {
      if (this.adapter != null) {
        this.adapter.unregisterAdapterDataObserver(adapterObserver);
      }

      this.adapter = adapter;
      if (this.adapter != null) {
        if (recyclerView != null) {
          recyclerView.setAdapter(this.adapter);
          if (listContainer.getVisibility() != View.VISIBLE) {
            changeState(STATE_CONTENT_VISIBLE, true);
          }
        }

        adapter.registerAdapterDataObserver(adapterObserver);
        if (empty != null) {
          if (adapter.getItemCount() > 0) {
            empty.setVisibility(View.GONE);
            recyclerView.setVisibility(View.VISIBLE);
          } else {
            empty.setVisibility(View.VISIBLE);
            recyclerView.setVisibility(View.GONE);
          }
        }
      } else if (recyclerView != null) {
        changeState(STATE_PROGRESS_VISIBLE, true);
      }
    }
  }

  private void showEmptyView(boolean show) {
    if (empty == null) {
      return;
    }

    final View empty = this.empty;
    final View recyclerView = this.recyclerView;

    if (show && empty.getVisibility() == View.GONE) {
      empty.setAlpha(0.0f);
      empty.animate().alpha(1.0f).withStartAction(new Runnable() {
        @Override public void run() {
          empty.setVisibility(View.VISIBLE);
        }
      });
      recyclerView.animate().alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          recyclerView.setVisibility(View.GONE);
          recyclerView.setAlpha(1.0f);
        }
      });
    } else if (!show && empty.getVisibility() == View.VISIBLE) {
      empty.animate().alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          empty.setAlpha(1.0f);
          empty.setVisibility(View.GONE);
        }
      });
      recyclerView.animate().alpha(1.0f).withStartAction(new Runnable() {
        @Override public void run() {
          recyclerView.setAlpha(0.0f);
          recyclerView.setVisibility(View.VISIBLE);
        }
      });
    }
  }

  private RecyclerView.AdapterDataObserver adapterObserver =
      new RecyclerView.AdapterDataObserver() {
        @Override public void onChanged() {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeChanged(int positionStart, int itemCount) {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeInserted(int positionStart, int itemCount) {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }

        @Override public void onItemRangeRemoved(int positionStart, int itemCount) {
          final int adapterItemCount = adapter.getItemCount();
          showEmptyView(adapterItemCount == 0);
        }
      };

  public RecyclerView.Adapter<T> getAdapter() {
    return adapter;
  }

  public void clearEmptyText() {
    emptyText = "";
    if (empty != null) {
      empty.setText(emptyText);
    }
  }

  final void setEmptyText(String text) {
    if (text == null) {
      text = "";
    }
    emptyText = text;
    if (empty != null) {
      empty.setText(emptyText);
    }
  }

  final void setEmptyText(int resId) {
    String text = appContext.getString(resId);
    if (text != null) {
      emptyText = text;
      if (empty != null) {
        empty.setText(emptyText);
      }
    }
  }

  final void setEmptyText(int resId, Object... formatArgs) {
    String text = appContext.getString(resId, formatArgs);
    if (text != null) {
      emptyText = text;
      if (empty != null) {
        empty.setText(emptyText);
      }
    }
  }
}
