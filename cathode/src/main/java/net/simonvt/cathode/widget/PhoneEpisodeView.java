package net.simonvt.cathode.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;
import butterknife.InjectView;
import butterknife.Views;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;

public class PhoneEpisodeView extends ViewGroup {

  private static final float SCREEN_RATIO = 680.f / 1000.f;

  @InjectView(R.id.screen) RemoteImageView screen;

  @InjectView(R.id.infoParent) ViewGroup infoParent;
  @InjectView(R.id.overflow) OverflowView overflow;
  @InjectView(R.id.checkbox) CheckMark checkbox;

  private int minHeight;

  public PhoneEpisodeView(Context context) {
    super(context);
    init(context);
  }

  public PhoneEpisodeView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public PhoneEpisodeView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    if (!isInEditMode()) CathodeApp.inject(context, this);
    minHeight = getResources().getDimensionPixelSize(R.dimen.showItemMinHeight);
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    Views.inject(this);
  }

  @Override
  protected void onLayout(boolean changed, int l, int t, int r, int b) {
    final int width = r - l;
    final int height = b - t;

    final LayoutParams posterLp = (LayoutParams) screen.getLayoutParams();
    final int posterWidth = screen.getMeasuredWidth();
    screen.layout(posterLp.leftMargin, posterLp.topMargin, posterWidth + posterLp.leftMargin,
        height - posterLp.bottomMargin);

    final LayoutParams infoParentLp = (LayoutParams) infoParent.getLayoutParams();
    final int infoParentHeight = infoParent.getMeasuredHeight();
    final int infoParentTop =
        (height - infoParentHeight) / 2 + getPaddingTop() + infoParentLp.topMargin;
    final int infoParentBottom = infoParentTop + infoParentHeight;
    final int paddingRight = getPaddingRight();
    final int infoParentLeft =
        posterLp.leftMargin + posterWidth + posterLp.rightMargin + infoParentLp.leftMargin;
    final int infoParentRight = width - getPaddingRight() - infoParentLp.rightMargin;
    infoParent.layout(infoParentLeft, infoParentTop, infoParentRight, infoParentBottom);

    overflow.layout(width - overflow.getMeasuredWidth(), 0, width, overflow.getMeasuredHeight());

    LayoutParams watchedParams = (LayoutParams) checkbox.getLayoutParams();
    final int watchedRight = width - watchedParams.rightMargin - getPaddingRight();
    final int watchedBottom = height - watchedParams.bottomMargin - getPaddingBottom();
    checkbox.layout(watchedRight - checkbox.getMeasuredWidth(),
        watchedBottom - checkbox.getMeasuredHeight(), watchedRight, watchedBottom);
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int mode = MeasureSpec.getMode(widthMeasureSpec);
    if (mode != MeasureSpec.EXACTLY) {
      throw new RuntimeException("PhoneShowView width must measure as EXACTLY.");
    }

    LayoutParams posterLp = (LayoutParams) screen.getLayoutParams();
    LayoutParams infoParentLp = (LayoutParams) infoParent.getLayoutParams();

    // Measure the height of show and next episode info
    measureChild(infoParent, widthMeasureSpec, heightMeasureSpec);
    final int infoParentHeight =
        infoParent.getMeasuredHeight() + infoParentLp.topMargin + infoParentLp.bottomMargin;

    final int viewWidth = MeasureSpec.getSize(widthMeasureSpec);
    final int viewHeight =
        Math.max(infoParentHeight + getPaddingTop() + getPaddingBottom(), minHeight);

    int posterHeight = viewHeight - posterLp.topMargin - posterLp.bottomMargin;
    int posterWidth = (int) (viewHeight * SCREEN_RATIO);
    final int posterWidthMeasureSpec =
        MeasureSpec.makeMeasureSpec(posterWidth, MeasureSpec.EXACTLY);
    final int posterHeightMeasureSpec =
        MeasureSpec.makeMeasureSpec(posterHeight, MeasureSpec.EXACTLY);
    screen.measure(posterWidthMeasureSpec, posterHeightMeasureSpec);

    final int infoParentWidth = viewWidth
        - posterWidth
        - posterLp.leftMargin
        - posterLp.rightMargin
        - infoParentLp.leftMargin
        - infoParentLp.rightMargin
        - getPaddingLeft()
        - getPaddingRight();
    final int infoParentWidthMeasureSpec =
        MeasureSpec.makeMeasureSpec(infoParentWidth, MeasureSpec.EXACTLY);
    final int infoParentHeightMeasureSpec =
        MeasureSpec.makeMeasureSpec(ViewGroup.LayoutParams.WRAP_CONTENT, MeasureSpec.UNSPECIFIED);
    infoParent.measure(infoParentWidthMeasureSpec, infoParentHeightMeasureSpec);

    measureChild(overflow, widthMeasureSpec, heightMeasureSpec);
    measureChild(checkbox, widthMeasureSpec, heightMeasureSpec);

    setMeasuredDimension(viewWidth, viewHeight);
  }

  @Override
  public LayoutParams generateLayoutParams(AttributeSet attrs) {
    return new LayoutParams(getContext(), attrs);
  }

  @Override
  protected LayoutParams generateDefaultLayoutParams() {
    return new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
  }

  @Override
  protected LayoutParams generateLayoutParams(ViewGroup.LayoutParams p) {
    return new LayoutParams(p);
  }

  @Override
  protected boolean checkLayoutParams(ViewGroup.LayoutParams p) {
    return p instanceof LayoutParams;
  }

  public static class LayoutParams extends MarginLayoutParams {

    public LayoutParams(Context c, AttributeSet attrs) {
      super(c, attrs);
    }

    public LayoutParams(int width, int height) {
      super(width, height);
    }

    public LayoutParams(ViewGroup.LayoutParams p) {
      super(p);
    }

    public LayoutParams(MarginLayoutParams source) {
      super(source);
    }
  }
}
