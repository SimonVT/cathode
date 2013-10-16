package net.simonvt.cathode.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;
import net.simonvt.cathode.util.DateUtils;

public class TimeStamp extends TextView {

  private long timeInMillis;

  private boolean extended;

  public TimeStamp(Context context) {
    super(context);
  }

  public TimeStamp(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TimeStamp(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setTimeInMillis(long timeInMillis) {
    this.timeInMillis = timeInMillis;

    removeCallbacks(updater);
    updater.run();
  }

  public void setExtended(boolean extended) {
    this.extended = extended;
  }

  private final Runnable updater = new Runnable() {
    @Override public void run() {
      final String timeStamp = DateUtils.millisToString(getContext(), timeInMillis, extended);
      setText(timeStamp);

      final long nextUpdate = DateUtils.timeUntilUpdate(timeInMillis);
      postDelayed(this, nextUpdate);
    }
  };

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (timeInMillis > 0) updater.run();
  }

  @Override protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    removeCallbacks(updater);
  }
}
