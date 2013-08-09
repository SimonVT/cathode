package net.simonvt.cathode.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.LinearLayout;

public class NoClickThroughLinearLayout extends LinearLayout {

  public NoClickThroughLinearLayout(Context context) {
    super(context);
  }

  public NoClickThroughLinearLayout(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public NoClickThroughLinearLayout(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  public boolean onTouchEvent(MotionEvent event) {
    return true;
  }
}
