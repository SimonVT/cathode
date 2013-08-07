package net.simonvt.trakt.widget;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import net.simonvt.trakt.R;

public class MaxSizeLinearLayout extends LinearLayout {

  private int maxWidth = -1;

  private int maxHeight = -1;

  public MaxSizeLinearLayout(Context context) {
    super(context);
  }

  public MaxSizeLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context, attrs, 0);
  }

  public MaxSizeLinearLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context, attrs, defStyle);
  }

  private void init(Context context, AttributeSet attrs, int defStyle) {
    TypedArray a =
        context.obtainStyledAttributes(attrs, R.styleable.MaxSizeLinearLayout, 0, defStyle);

    maxWidth = a.getDimensionPixelSize(R.styleable.MaxSizeLinearLayout_maxWidth, -1);
    maxHeight = a.getDimensionPixelSize(R.styleable.MaxSizeLinearLayout_maxHeight, -1);

    a.recycle();
  }

  @Override
  protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
    final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int widthSize = MeasureSpec.getSize(widthMeasureSpec);

    final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
    int heightSize = MeasureSpec.getSize(heightMeasureSpec);

    if (maxWidth != -1 && widthMode != MeasureSpec.UNSPECIFIED) {
      widthSize = Math.min(widthSize, maxWidth);
    }

    if (maxHeight != -1 && heightMode != MeasureSpec.UNSPECIFIED) {
      heightSize = Math.min(heightSize, maxHeight);
    }

    super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, widthMode),
        MeasureSpec.makeMeasureSpec(heightSize, heightMode));
  }
}
