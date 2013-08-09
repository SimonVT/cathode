package net.simonvt.cathode.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.ScrollView;

public class ObservableScrollView extends ScrollView {

  private ScrollListener listener;

  public ObservableScrollView(Context context) {
    super(context);
  }

  public ObservableScrollView(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public ObservableScrollView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  @Override
  protected void onScrollChanged(int l, int t, int oldl, int oldt) {
    super.onScrollChanged(l, t, oldl, oldt);
    if (listener != null) {
      listener.onScrollChanged(l, t);
    }
  }

  public void setListener(ScrollListener listener) {
    this.listener = listener;
  }

  public interface ScrollListener {

    void onScrollChanged(int l, int t);
  }
}
