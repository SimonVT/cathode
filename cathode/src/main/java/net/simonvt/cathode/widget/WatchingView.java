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

import android.animation.ObjectAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.os.Build;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.widget.ProgressBar;
import android.widget.TextView;
import butterknife.ButterKnife;
import butterknife.InjectView;
import net.simonvt.cathode.R;
import net.simonvt.cathode.util.ViewUtils;
import timber.log.Timber;

public class WatchingView extends ViewGroup {

  public interface WatchingViewListener {

    void onExpand(WatchingView view);

    void onCollapse(WatchingView view);

    void onEpisodeClicked(WatchingView view, long episodeId, String showTitle);

    void onMovieClicked(WatchingView view, long movieId, String movieTitle);

    void onAnimatingIn(WatchingView view);

    void onAnimatingOut(WatchingView view);
  }

  public static enum Type {
    SHOW,
    MOVIE;
  }

  private static final int EXPAND_DURATION = 300;
  private static final int EXPAND_DURATION_OFFSET = 0;

  private static final int RADIUS_DURATION = 250;
  private static final int RADIUS_DURATION_OFFSET = 300;

  private static final int ANIMATION_DURATION =
      Math.max(EXPAND_DURATION + EXPAND_DURATION_OFFSET, RADIUS_DURATION + RADIUS_DURATION_OFFSET);

  private int maxWidth;

  private boolean isExpanded;

  private float animationProgress;

  private int collapsedDiameter;

  private int expandedDiameter;

  private int diameter;

  private int topBottomOffset;

  private int collapsedRadius;

  ObjectAnimator animator;

  private Paint backgroundPaint = new Paint();

  @InjectView(R.id.poster) RemoteImageView posterView;

  @InjectView(R.id.infoParent) View infoParent;

  @InjectView(R.id.title) TextView titleView;

  @InjectView(R.id.progress) ProgressBar progress;

  @InjectView(R.id.subtitle) TextView subtitleView;

  private static final boolean IS_LOLLIPOP = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP;

  private WatchingViewListener watchingViewListener;

  private Type type;

  private long showId;

  private String showTitle;

  private long episodeId;

  private String episodeTitle;

  private long movieId;

  private String movieTitle;

  private String poster;

  private long startTime;

  private long endTime;

  private Handler handler;

  private Runnable updateProgress = new Runnable() {
    @Override public void run() {
      Timber.d("Updating watched progress");
      progress.setProgress((int) (System.currentTimeMillis() - startTime));
      handler.postDelayed(this, 30 * 1000L);
    }
  };

  public WatchingView(Context context) {
    super(context);
    init(context);
  }

  public WatchingView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public WatchingView(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  public WatchingView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private void init(Context context) {
    setWillNotDraw(false);

    backgroundPaint.setColor(0xFFFAFAFA);

    collapsedDiameter = ViewUtils.dpToPx(context, 48);
    expandedDiameter = ViewUtils.dpToPx(context, 16);
    diameter = expandedDiameter;

    if (IS_LOLLIPOP) {
      setOutlineProvider(new ViewOutlineProvider() {
        @Override public void getOutline(View view, Outline outline) {
          Rect outlineRect = new Rect();
          outlineRect.left = (int) (getPaddingLeft() + posterView.getTranslationX());
          outlineRect.top = getPaddingTop() + topBottomOffset;
          outlineRect.right = getWidth() - getPaddingRight();
          outlineRect.bottom = getHeight() - getPaddingBottom() - topBottomOffset;
          outline.setRoundRect(outlineRect, diameter / 2);
        }
      });
      setClipToOutline(true);
    }

    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN_MR2) {
      setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    if (getResources().getBoolean(R.bool.isTablet)) {
      maxWidth = ViewUtils.dpToPx(context, 400);
    }

    animationProgress = 1.0f;
    setVisibility(GONE);

    handler = new Handler();
  }

  public void setWatchingViewListener(WatchingViewListener watchingViewListener) {
    this.watchingViewListener = watchingViewListener;
  }

  public void watchingShow(long showId, String showTitle, long episodeId, String episodeTitle,
      String poster, long startTime, long endTime) {
    clearVariables();

    this.type = Type.SHOW;

    this.showId = showId;
    this.showTitle = showTitle;
    this.episodeId = episodeId;
    this.episodeTitle = episodeTitle;
    this.poster = poster;
    this.startTime = startTime;
    this.endTime = endTime;

    posterView.setImage(poster);
    titleView.setText(showTitle);
    subtitleView.setVisibility(VISIBLE);
    subtitleView.setText(episodeTitle);
    progress.setMax((int) (endTime - startTime));
    progress.setProgress((int) (System.currentTimeMillis() - startTime));

    animateIn();
  }

  public void watchingMovie(long movieId, String movieTitle, String poster, long startTime,
      long endTime) {
    clearVariables();

    this.type = Type.MOVIE;

    this.movieId = movieId;
    this.movieTitle = movieTitle;
    this.poster = poster;
    this.startTime = startTime;
    this.endTime = endTime;

    posterView.setImage(poster);
    titleView.setText(movieTitle);
    subtitleView.setVisibility(GONE);

    animateIn();
  }

  public void clearWatching() {
    clearVariables();
    animateOut();
  }

  private void clearVariables() {
    type = null;
    showId = -1L;
    showTitle = null;
    episodeId = -1L;
    episodeTitle = null;
    movieId = -1L;
    movieTitle = null;
    poster = null;
    startTime = 0L;
    endTime = 0L;

    isExpanded = false;
  }

  private void animateIn() {
    if (watchingViewListener != null) {
      watchingViewListener.onAnimatingIn(this);
    }
    animate().alpha(1.0f).withStartAction(new Runnable() {
      @Override public void run() {
        if (getVisibility() == GONE) {
          setAlpha(0.0f);
          setVisibility(VISIBLE);
        }
      }
    });
  }

  private void animateOut() {
    if (watchingViewListener != null) {
      watchingViewListener.onAnimatingOut(this);
    }
    if (getVisibility() != GONE) {
      animate().alpha(0.0f).withEndAction(new Runnable() {
        @Override public void run() {
          setVisibility(GONE);
          collapse();
        }
      });
    }
  }

  private OnClickListener expandedClickListener = new OnClickListener() {
    @Override public void onClick(View v) {
      if (type == Type.SHOW) {
        watchingViewListener.onEpisodeClicked(WatchingView.this, episodeId, showTitle);
      } else {
        watchingViewListener.onMovieClicked(WatchingView.this, movieId, movieTitle);
      }
    }
  };

  private OnClickListener collapsedClickListener = new OnClickListener() {
    @Override public void onClick(View v) {
      expand();
    }
  };

  private void setIsExpanded(boolean isExpanded) {
    this.isExpanded = isExpanded;

    handler.removeCallbacks(updateProgress);

    if (isExpanded) {
      setOnClickListener(expandedClickListener);
      posterView.setOnClickListener(null);

      if (watchingViewListener != null) {
        watchingViewListener.onExpand(this);
      }

      updateProgress.run();
    } else {
      setOnClickListener(null);
      setClickable(false);
      posterView.setOnClickListener(collapsedClickListener);

      if (watchingViewListener != null) {
        watchingViewListener.onCollapse(this);
      }
    }
  }

  public boolean isExpanded() {
    return isExpanded;
  }

  public void expand() {
    if (isExpanded()) {
      return;
    }

    if (animator != null) {
      animator.cancel();
      animator = null;
    }

    animator = ObjectAnimator.ofFloat(WatchingView.this, "animationProgress", 0.0f);
    animator.setDuration(ANIMATION_DURATION);
    animator.start();

    setIsExpanded(true);
  }

  public void collapse() {
    if (!isExpanded) {
      return;
    }

    if (animator != null) {
      animator.cancel();
      animator = null;
    }

    animator = ObjectAnimator.ofFloat(WatchingView.this, "animationProgress", 1.0f);
    animator.setDuration(ANIMATION_DURATION);
    animator.start();

    setIsExpanded(false);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.inject(this);

    setIsExpanded(isExpanded);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();

    if (isExpanded) {
      handler.removeCallbacks(updateProgress);
      handler.post(updateProgress);
    }
  }

  @Override protected void onDetachedFromWindow() {
    handler.removeCallbacks(updateProgress);
    super.onDetachedFromWindow();
  }

  private void expandAnimation(float progress) {
    final int width = getWidth();
    final int pl = getPaddingLeft();
    final int pr = getPaddingRight();
    final int posterWidth = posterView.getWidth();
    final int totalPosterOffset = width - pl - pr - posterWidth;

    float offsetRatio = animationProgress * (EXPAND_DURATION + RADIUS_DURATION) / EXPAND_DURATION;
    offsetRatio = Math.min(offsetRatio, 1.0f);

    final int offset = (int) (totalPosterOffset * offsetRatio);

    posterView.setTranslationX(offset);
    infoParent.setTranslationX(offset);
    infoParent.setAlpha(1.0f - progress);
  }

  private void radiusAnimation(float progress) {
    final int posterWidth = posterView.getWidth();
    final int posterHeight = posterView.getHeight();
    final int maxRadius = posterWidth;

    final int diff = (int) (progress * (maxRadius - expandedDiameter));
    diameter = expandedDiameter + diff;
    topBottomOffset = (int) ((posterHeight - posterWidth) * progress / 2);
  }

  public void setAnimationProgress(float animationProgress) {
    this.animationProgress = animationProgress;

    final float expandRatio = 1.0f * EXPAND_DURATION / ANIMATION_DURATION;
    final float expandOffset = 1.0f * EXPAND_DURATION_OFFSET / ANIMATION_DURATION;
    final float expandProgress =
        Math.max(Math.min((animationProgress - expandOffset) / expandRatio, 1.0f), 0.0f);
    expandAnimation(expandProgress);

    final float radiusRatio = 1.0f * RADIUS_DURATION / ANIMATION_DURATION;

    final float radiusOffset = 1.0f * RADIUS_DURATION_OFFSET / ANIMATION_DURATION;
    final float radiusProgress =
        Math.max(Math.min((animationProgress - radiusOffset) / radiusRatio, 1.0f), 0.0f);
    radiusAnimation(radiusProgress);

    invalidate();
    if (IS_LOLLIPOP) {
      invalidateOutline();
    }
  }

  public float getAnimationProgress() {
    return animationProgress;
  }

  @Override protected void onDraw(Canvas canvas) {
    super.onDraw(canvas);
    if (IS_LOLLIPOP) {
      canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
          getHeight() - getPaddingBottom(), backgroundPaint);
    }
  }

  @Override protected void dispatchDraw(Canvas canvas) {
    if (IS_LOLLIPOP) {
      super.dispatchDraw(canvas);
    } else {
      Path clipPath = new Path();

      final int width = getWidth();
      final int height = getHeight();

      final int posterOffset = (int) posterView.getTranslationX();

      RectF topLeft = new RectF();
      topLeft.left = getPaddingLeft() + posterOffset;
      topLeft.top = getPaddingTop() + topBottomOffset;
      topLeft.right = topLeft.left + diameter;
      topLeft.bottom = topLeft.top + diameter;

      RectF topRight = new RectF();
      topRight.right = width - getPaddingRight();
      topRight.top = getPaddingTop() + topBottomOffset;
      topRight.left = topRight.right - diameter;
      topRight.bottom = topRight.top + diameter;

      RectF bottomRight = new RectF();
      bottomRight.right = width - getPaddingRight();
      bottomRight.bottom = height - getPaddingBottom() - topBottomOffset;
      bottomRight.left = bottomRight.right - diameter;
      bottomRight.top = bottomRight.bottom - diameter;

      RectF bottomLeft = new RectF();
      bottomLeft.left = getPaddingLeft() + posterOffset;
      bottomLeft.bottom = height - getPaddingBottom() - topBottomOffset;
      bottomLeft.right = bottomLeft.left + diameter;
      bottomLeft.top = bottomLeft.bottom - diameter;

      clipPath.moveTo(getPaddingLeft() + posterOffset,
          getPaddingTop() + diameter + topBottomOffset);
      clipPath.arcTo(topLeft, 180f, 90f, false);
      clipPath.lineTo(width - getPaddingRight() - diameter, getPaddingTop() + topBottomOffset);
      clipPath.arcTo(topRight, 270f, 90f, false);
      clipPath.lineTo(width - getPaddingRight(),
          height - getPaddingBottom() - diameter - topBottomOffset);
      clipPath.arcTo(bottomRight, 0f, 90f, false);
      clipPath.lineTo(getPaddingLeft() + diameter + posterOffset,
          height - getPaddingBottom() - topBottomOffset);
      clipPath.arcTo(bottomLeft, 90f, 90f, false);
      clipPath.lineTo(getPaddingLeft() + posterOffset,
          getPaddingTop() + diameter + topBottomOffset);
      clipPath.close();

      final int save = canvas.save();
      canvas.clipPath(clipPath);

      canvas.drawRect(getPaddingLeft(), getPaddingTop(), getWidth() - getPaddingRight(),
          getHeight() - getPaddingBottom(), backgroundPaint);

      super.dispatchDraw(canvas);

      canvas.restoreToCount(save);
    }
  }

  @Override protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int height = b - t;
    final int width = r - l;

    LayoutParams posterParams = (LayoutParams) posterView.getLayoutParams();
    LayoutParams infoParams = (LayoutParams) infoParent.getLayoutParams();

    final int posterLeft = getPaddingLeft() + posterParams.leftMargin;
    final int posterTop = getPaddingTop() + posterParams.topMargin;
    final int posterRight = posterLeft + posterView.getMeasuredWidth();
    final int posterBottom = posterTop + posterView.getMeasuredHeight();
    posterView.layout(posterLeft, posterTop, posterRight, posterBottom);

    final int infoLeft = posterRight + posterParams.rightMargin + infoParams.leftMargin;
    final int infoRight = width - getPaddingRight() - infoParams.rightMargin;
    final int infoHeight = infoParent.getMeasuredHeight();
    final int infoTop = (height - infoHeight) / 2 + getPaddingTop();
    final int infoBottom = infoTop + infoHeight;

    infoParent.layout(infoLeft, infoTop, infoRight, infoBottom);

    if (changed) {
      setAnimationProgress(animationProgress);
    }
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);

    if (widthMode == MeasureSpec.UNSPECIFIED) {
      throw new IllegalStateException("Must measure with an exact width");
    }

    int width = MeasureSpec.getSize(widthMeasureSpec);
    if (maxWidth > 0) {
      width = Math.min(width, maxWidth);
    }

    measureChild(posterView, widthMeasureSpec, heightMeasureSpec);
    LayoutParams posterParams = (LayoutParams) posterView.getLayoutParams();

    int leftoverWidth = width
        - getPaddingLeft()
        - posterParams.leftMargin
        - posterView.getMeasuredWidth()
        - posterParams.rightMargin
        - getPaddingRight();

    LayoutParams infoParams = (LayoutParams) infoParent.getLayoutParams();
    int infoWidth = leftoverWidth - infoParams.leftMargin - infoParams.rightMargin;
    final int infoWidthSpec = MeasureSpec.makeMeasureSpec(infoWidth, MeasureSpec.EXACTLY);
    final int infoHeightSpec =
        MeasureSpec.makeMeasureSpec(posterView.getMeasuredHeight(), MeasureSpec.AT_MOST);
    infoParent.measure(infoWidthSpec, infoHeightSpec);

    final int height = getPaddingTop()
        + posterParams.topMargin
        + posterView.getMeasuredHeight()
        + posterParams.bottomMargin
        + getPaddingBottom();

    setMeasuredDimension(width, height);
  }

  @Override public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  }

  @Override protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @Override protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  public static class LayoutParams extends MarginLayoutParams {

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }

    public LayoutParams(ViewGroup.LayoutParams source) {
      super(source);
    }
  }

  @Override protected Parcelable onSaveInstanceState() {
    Parcelable superState = super.onSaveInstanceState();
    SavedState state = new SavedState(superState);

    state.type = type;
    state.showId = showId;
    state.showTitle = showTitle;
    state.episodeId = episodeId;
    state.episodeTitle = episodeTitle;
    state.movieId = movieId;
    state.movieTitle = movieTitle;
    state.poster = poster;
    state.startTime = startTime;
    state.endTime = endTime;
    state.startTime = startTime;
    state.endTime = endTime;
    state.isExpanded = isExpanded();

    return state;
  }

  @Override protected void onRestoreInstanceState(Parcelable state) {
    SavedState savedState = (SavedState) state;
    super.onRestoreInstanceState(savedState.getSuperState());

    if (savedState.type == null) {
      setVisibility(GONE);
    } else {
      setVisibility(VISIBLE);
    }

    if (savedState.type == Type.SHOW) {
      watchingShow(savedState.showId, savedState.showTitle, savedState.episodeId,
          savedState.episodeTitle, savedState.poster, savedState.startTime, savedState.endTime);
    } else if (savedState.type == Type.MOVIE) {
      watchingMovie(savedState.movieId, savedState.movieTitle, savedState.poster,
          savedState.startTime, savedState.endTime);
    }

    setIsExpanded(savedState.isExpanded);
    if (savedState.isExpanded) {
      setAnimationProgress(0.0f);
    } else {
      setAnimationProgress(1.0f);
    }
  }

  static class SavedState extends BaseSavedState {

    private Type type;

    private long showId;

    private String showTitle;

    private long episodeId;

    private String episodeTitle;

    private long movieId;

    private String movieTitle;

    private String poster;

    private long startTime;

    private long endTime;

    private boolean isExpanded;

    public SavedState(Parcelable superState) {
      super(superState);
    }

    public SavedState(Parcel in) {
      super(in);
      type = (Type) in.readSerializable();
      showId = in.readLong();
      showTitle = in.readString();
      episodeId = in.readLong();
      episodeTitle = in.readString();
      movieId = in.readLong();
      movieTitle = in.readString();
      poster = in.readString();
      startTime = in.readLong();
      endTime = in.readLong();
      isExpanded = in.readInt() == 1;
    }

    @Override public void writeToParcel(Parcel dest, int flags) {
      super.writeToParcel(dest, flags);
      dest.writeSerializable(type);
      dest.writeLong(showId);
      dest.writeString(showTitle);
      dest.writeLong(episodeId);
      dest.writeString(episodeTitle);
      dest.writeLong(movieId);
      dest.writeString(movieTitle);
      dest.writeString(poster);
      dest.writeLong(startTime);
      dest.writeLong(endTime);
      dest.writeInt(isExpanded ? 1 : 0);
    }

    @SuppressWarnings("UnusedDeclaration")
    public static final Creator<SavedState> CREATOR = new Creator<SavedState>() {
      @Override public SavedState createFromParcel(Parcel in) {
        return new SavedState(in);
      }

      @Override public SavedState[] newArray(int size) {
        return new SavedState[size];
      }
    };
  }
}
