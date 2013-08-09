package net.simonvt.cathode.ui.fragment;

import android.os.Bundle;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import butterknife.InjectView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.util.LogWrapper;

public abstract class ProgressFragment extends BaseFragment {

  private static final String TAG = "ProgressFragment";

  private static final int STATE_NONE = -1;
  private static final int STATE_PROGRESS_VISIBLE = 0;
  private static final int STATE_CONTENT_VISIBLE = 1;

  private static final int ANIMATION_DURATION = 600;

  @InjectView(R.id.contentContainer) View content;

  @InjectView(R.id.progressContainer) View progress;

  private boolean animating;

  private boolean wait;

  private int currentState = STATE_PROGRESS_VISIBLE;
  private int pendingStateChange = STATE_NONE;

  @Override
  public void onViewCreated(View view, Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);

    wait = true;
    view.getViewTreeObserver()
        .addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override
          public void onGlobalLayout() {
            LogWrapper.v(TAG, "[onGlobalLayout] State: " + currentState);
            getView().getViewTreeObserver().removeOnGlobalLayoutListener(this);
            wait = false;

            if (currentState == STATE_CONTENT_VISIBLE) {
              content.setAlpha(1.0f);
              content.setVisibility(View.VISIBLE);
              progress.setVisibility(View.GONE);
            } else {
              content.setVisibility(View.GONE);
              progress.setAlpha(1.0f);
              progress.setVisibility(View.VISIBLE);
            }
          }
        });
  }

  @Override
  public Animation onCreateAnimation(int transit, boolean enter, int nextAnim) {
    Animation animation = null;
    if (nextAnim != 0) {
      animation = AnimationUtils.loadAnimation(getActivity(), nextAnim);
      animation.setAnimationListener(new Animation.AnimationListener() {
        @Override
        public void onAnimationStart(Animation animation) {
          animating = true;
        }

        @Override
        public void onAnimationEnd(Animation animation) {
          animating = false;
          if (pendingStateChange != STATE_NONE) {
            changeState(pendingStateChange, true);
            pendingStateChange = STATE_NONE;
          }
        }

        @Override
        public void onAnimationRepeat(Animation animation) {
        }
      });
    }

    return animation;
  }

  protected void setContentVisible(boolean contentVisible) {
    LogWrapper.v(TAG, "[setContentVisible]");
    if (getView() == null) {
      currentState = contentVisible ? STATE_CONTENT_VISIBLE : STATE_PROGRESS_VISIBLE;
      return;
    }

    if (contentVisible) {
      changeState(STATE_CONTENT_VISIBLE, true);
    } else {
      changeState(STATE_PROGRESS_VISIBLE, true);
    }
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

    if (wait || progress == null) {
      return;
    }

    if (newState == STATE_PROGRESS_VISIBLE && content.getVisibility() != View.VISIBLE) {
      return;
    }

    if (newState == STATE_CONTENT_VISIBLE && !animate) {
      content.setVisibility(View.VISIBLE);
      progress.setVisibility(View.GONE);
    } else if (newState == STATE_PROGRESS_VISIBLE && !animate) {
      content.setVisibility(View.GONE);
      progress.setVisibility(View.VISIBLE);
    } else {
      content.setVisibility(View.VISIBLE);
      progress.setVisibility(View.VISIBLE);

      if (newState == STATE_CONTENT_VISIBLE) {
        progress.animate().alpha(0.0f);
        if (content.getAlpha() == 1.0f) content.setAlpha(0.0f);
        content.animate().alpha(1.0f).withEndAction(new Runnable() {
          @Override
          public void run() {
            if (progress == null) {
              // In case fragment is removed before animation is done
              return;
            }
            progress.setVisibility(View.GONE);
            ProgressFragment.this.onAnimationEnd(newState);
          }
        });
      } else {
        if (progress.getAlpha() == 1.0f) progress.setAlpha(0.0f);
        progress.animate().alpha(1.0f);
        content.animate().alpha(0.0f).withEndAction(new Runnable() {
          @Override
          public void run() {
            if (progress == null) {
              // In case fragment is removed before animation is done
              return;
            }
            content.setVisibility(View.GONE);
            ProgressFragment.this.onAnimationEnd(newState);
          }
        });
      }
    }
  }

  protected void onAnimationEnd(int newState) {
  }
}
