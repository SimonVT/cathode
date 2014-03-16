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
package net.simonvt.cathode.ui.fragment;

import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.AbsListView;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.TextView;
import butterknife.InjectView;
import net.simonvt.cathode.R;

public abstract class AbsAdapterFragment extends BaseFragment {

  private static final String SAVED_EMPTY_TEXT = "savedEmptyText";

  private static final int STATE_NONE = -1;
  private static final int STATE_PROGRESS_VISIBLE = 0;
  private static final int STATE_LIST_VISIBLE = 1;

  private static final int ANIMATION_DURATION = 600;

  private BaseAdapter adapter;

  @InjectView(R.id.progressContainer) View progressContainer;
  @InjectView(R.id.listContainer) View listContainer;
  @InjectView(android.R.id.list) AbsListView adapterView;
  @InjectView(android.R.id.empty) TextView empty;

  private Context appContext;

  private String emptyText;

  protected boolean attachLongClickListener;

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

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    if (emptyText != null) empty.setText(emptyText);

    adapterView.setOnItemClickListener(onClickListener);
    if (attachLongClickListener) adapterView.setOnItemLongClickListener(onLongClickListener);
    adapterView.setEmptyView(empty);
    if (adapter != null) adapterView.setAdapter(adapter);

    view.getViewTreeObserver().addOnGlobalLayoutListener(layoutListener);
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
            currentState = STATE_LIST_VISIBLE;
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

    if (newState == STATE_LIST_VISIBLE && !animate) {
      listContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.GONE);
    } else if (newState == STATE_PROGRESS_VISIBLE && !animate) {
      listContainer.setVisibility(View.GONE);
      progressContainer.setVisibility(View.VISIBLE);
    } else {
      listContainer.setVisibility(View.VISIBLE);
      progressContainer.setVisibility(View.VISIBLE);

      final Runnable onAnimationEnd = new Runnable() {
        @Override public void run() {
          if (progressContainer == null) {
            // In case fragment is removed before animation is done
            return;
          }
          if (newState == STATE_LIST_VISIBLE) {
            progressContainer.setVisibility(View.GONE);
          } else {
            listContainer.setVisibility(View.GONE);
            if (adapter == null) {
              adapterView.setAdapter(null);
            }
          }
          progressContainer.setAlpha(1.0f);
          listContainer.setAlpha(1.0f);
        }
      };

      if (newState == STATE_LIST_VISIBLE) {
        listContainer.animate().alpha(1.0f).withEndAction(onAnimationEnd);
        progressContainer.animate().alpha(0.0f);
      } else {
        listContainer.animate().alpha(0.0f).withEndAction(onAnimationEnd);
        progressContainer.animate().alpha(1.0f);
      }
    }
  }

  private final AdapterView.OnItemClickListener onClickListener =
      new AdapterView.OnItemClickListener() {
        @Override public void onItemClick(AdapterView<?> parent, View v, int position, long id) {
          AbsAdapterFragment.this.onItemClick(parent, v, position, id);
        }
      };

  protected void onItemClick(AdapterView l, View v, int position, long id) {
  }

  private final AdapterView.OnItemLongClickListener onLongClickListener =
      new AdapterView.OnItemLongClickListener() {
        @Override
        public boolean onItemLongClick(AdapterView<?> adapterView, View v, int position, long id) {
          return AbsAdapterFragment.this.onItemLongClick(adapterView, v, position, id);
        }
      };

  protected boolean onItemLongClick(AdapterView l, View v, int position, long id) {
    return false;
  }

  public AbsListView getAdapterView() {
    return adapterView;
  }

  public void setAdapter(BaseAdapter adapter) {
    if (adapter != this.adapter) {
      this.adapter = adapter;
      if (this.adapter != null) {
        if (adapterView != null) {
          adapterView.setAdapter(this.adapter);
          if (listContainer.getVisibility() != View.VISIBLE) {
            changeState(STATE_LIST_VISIBLE, true);
          }
        }
      } else if (adapterView != null) {
        changeState(STATE_PROGRESS_VISIBLE, true);
      }
    }
  }

  public BaseAdapter getAdapter() {
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
