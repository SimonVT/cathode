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

package net.simonvt.cathode.common.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.TextView;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.RecyclerView;
import net.simonvt.cathode.common.R;
import net.simonvt.cathode.common.util.Views;

public abstract class RecyclerViewFragment<T extends RecyclerView.ViewHolder> extends BaseFragment {

  private static final String SAVED_EMPTY_TEXT = "savedEmptyText";

  private static final int STATE_NONE = -1;
  private static final int STATE_PROGRESS_VISIBLE = 0;
  private static final int STATE_CONTENT_VISIBLE = 1;

  private RecyclerView.Adapter<T> adapter;

  private View progressContainer;
  private View listContainer;
  private RecyclerView recyclerView;
  private TextView empty;

  private Context appContext;

  private String emptyText;

  private boolean animating;

  private int currentState = STATE_PROGRESS_VISIBLE;
  private int pendingStateChange = STATE_NONE;

  private boolean forceDisplayProgress;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    appContext = requireContext().getApplicationContext();

    if (inState != null) {
      emptyText = inState.getString(SAVED_EMPTY_TEXT);
    }
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    outState.putString(SAVED_EMPTY_TEXT, emptyText);
    super.onSaveInstanceState(outState);
  }

  protected RecyclerView.ItemAnimator getItemAnimator() {
    return null;
  }

  protected void addItemDecorations(RecyclerView recyclerView) {
  }

  protected abstract RecyclerView.LayoutManager getLayoutManager();

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_recyclerview, container, false);
  }

  public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    progressContainer = view.findViewById(R.id.progressContainer);
    listContainer = view.findViewById(R.id.listContainer);
    recyclerView = Views.findRequired(view, android.R.id.list);
    empty = Views.findRequired(view, android.R.id.empty);

    recyclerView.setLayoutManager(getLayoutManager());
    RecyclerView.ItemAnimator itemAnimator = getItemAnimator();
    if (itemAnimator != null) {
      recyclerView.setItemAnimator(itemAnimator);
    } else {
      ((DefaultItemAnimator) recyclerView.getItemAnimator()).setSupportsChangeAnimations(false);
    }
    addItemDecorations(recyclerView);

    if (empty != null) {
      if (emptyText != null) {
        empty.setText(emptyText);
      }

      if (adapter != null && adapter.getItemCount() > 0) {
        empty.setVisibility(View.GONE);
      } else {
        empty.setVisibility(View.VISIBLE);
      }
    }

    if (adapter != null) {
      recyclerView.setAdapter(adapter);
    }

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

  @Override public void onDestroyView() {
    progressContainer = null;
    listContainer = null;
    recyclerView = null;
    empty = null;
    super.onDestroyView();
  }

  @Override public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    Animation animation = null;
    if (nextAnim != 0) {
      animation = AnimationUtils.loadAnimation(requireContext(), nextAnim);
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

    updateViewVisiblity(animate);
  }

  private void updateViewVisiblity(boolean animate) {
    if (currentState == STATE_NONE) {
      return;
    }

    if (forceDisplayProgress || currentState == STATE_PROGRESS_VISIBLE) {
      if (listContainer.getVisibility() != View.GONE) {
        hideContent(animate);
      }

      displayProgress(animate);
    } else {
      if (progressContainer.getVisibility() != View.GONE) {
        hideProgress(animate);
      }

      displayContent(animate);
    }
  }

  private void displayProgress(boolean animate) {
    final boolean wasGone = progressContainer.getVisibility() == View.GONE;
    progressContainer.setVisibility(View.VISIBLE);

    if (animate) {
      if (wasGone) {
        progressContainer.setAlpha(0.0f);
      }

      progressContainer.animate().alpha(1.0f);
    }
  }

  private void hideProgress(boolean animate) {
    final View progressContainer = this.progressContainer;
    if (!animate) {
      progressContainer.setVisibility(View.GONE);
    } else {
      if (progressContainer.getVisibility() != View.GONE) {
        progressContainer.animate().alpha(0.0f).withEndAction(new Runnable() {
          @Override public void run() {
            progressContainer.setVisibility(View.GONE);
          }
        });
      }
    }
  }

  private void displayContent(boolean animate) {
    final boolean wasGone = listContainer.getVisibility() == View.GONE;
    listContainer.setVisibility(View.VISIBLE);

    if (animate) {
      if (wasGone) {
        listContainer.setAlpha(0.0f);
      }

      listContainer.animate().alpha(1.0f);
    }
  }

  private void hideContent(boolean animate) {
    final View listContainer = this.listContainer;

    if (!animate) {
      listContainer.setVisibility(View.GONE);
    } else {
      if (listContainer.getVisibility() != View.GONE) {
        listContainer.animate().alpha(0.0f).withEndAction(new Runnable() {
          @Override public void run() {
            listContainer.setVisibility(View.GONE);
          }
        });
      }
    }
  }

  public void setForceDisplayProgress(boolean forceDisplayProgress) {
    if (forceDisplayProgress == this.forceDisplayProgress) {
      return;
    }

    this.forceDisplayProgress = forceDisplayProgress;

    updateViewVisiblity(true);
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

      if (recyclerView != null) {
        recyclerView.setAdapter(this.adapter);
      }

      if (this.adapter != null) {
        if (recyclerView != null) {
          changeState(STATE_CONTENT_VISIBLE, true);
        }

        adapter.registerAdapterDataObserver(adapterObserver);

        if (empty != null) {
          if (adapter.getItemCount() > 0) {
            empty.setVisibility(View.GONE);
          } else {
            empty.setVisibility(View.VISIBLE);
          }
        }
      } else if (recyclerView != null) {
        recyclerView.setAdapter(null);
        changeState(STATE_PROGRESS_VISIBLE, true);
      }
    }
  }

  private void showEmptyView(boolean show) {
    final View empty = this.empty;

    if (empty == null) {
      return;
    }

    if (show && empty.getVisibility() == View.GONE) {
      empty.setAlpha(0.0f);
      empty.animate().alpha(1.0f).withStartAction(new Runnable() {
        @Override public void run() {
          empty.setVisibility(View.VISIBLE);
        }
      });
    } else if (!show && empty.getVisibility() == View.VISIBLE) {
      empty.animate().alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          empty.setAlpha(1.0f);
          empty.setVisibility(View.GONE);
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

  public final void setEmptyText(String text) {
    if (text == null) {
      text = "";
    }
    emptyText = text;
    if (empty != null) {
      empty.setText(emptyText);
    }
  }

  public final void setEmptyText(int resId) {
    String text = appContext.getString(resId);
    if (text != null) {
      emptyText = text;
      if (empty != null) {
        empty.setText(emptyText);
      }
    }
  }

  public final void setEmptyText(int resId, Object... formatArgs) {
    String text = appContext.getString(resId, formatArgs);
    if (text != null) {
      emptyText = text;
      if (empty != null) {
        empty.setText(emptyText);
      }
    }
  }
}
