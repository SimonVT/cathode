package net.simonvt.trakt.widget;

import android.content.Context;
import android.graphics.Rect;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.TouchDelegate;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.trakt.R;

public class OverflowView extends ImageView {

  public interface OverflowActionListener {

    void onPopupShown();

    void onPopupDismissed();

    void onActionSelected(int action);
  }

  public static class OverflowItem {

    private int action;

    private int title;

    public OverflowItem(int action, int title) {
      this.action = action;
      this.title = title;
    }
  }

  private List<OverflowItem> items = new ArrayList<OverflowItem>();

  private OverflowActionListener listener;

  private Rect overflowRect = new Rect();
  private TouchDelegate overflowDelegate;

  public OverflowView(Context context) {
    super(context);
    init(context);
  }

  public OverflowView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public OverflowView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  @Override
  protected void onLayout(boolean changed, int left, int top, int right, int bottom) {
    super.onLayout(changed, left, top, right, bottom);

    final int height = bottom - top;
    final int width = right - left;

    if (changed || overflowDelegate == null) {
      final int extraDim = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
          getResources().getDisplayMetrics());

      overflowRect.left = right - width - extraDim;
      overflowRect.top = top;
      overflowRect.right = right;
      overflowRect.bottom = bottom + extraDim;
      overflowDelegate = new TouchDelegate(overflowRect, this);
      ((ViewGroup) getParent()).setTouchDelegate(overflowDelegate);
    }
  }

  private void init(final Context context) {
    if (!isInEditMode()) {
      setWillNotDraw(true);
    }
    if (getDrawable() == null) setImageResource(R.drawable.item_overflow);
    setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View v) {
        PopupMenu popupMenu = new PopupMenu(context, OverflowView.this);

        for (OverflowItem item : items) {
          popupMenu.getMenu().add(0, item.action, 0, item.title);
        }

        popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
          @Override
          public boolean onMenuItemClick(MenuItem item) {
            if (listener != null) listener.onActionSelected(item.getItemId());
            return true;
          }
        });

        popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
          @Override
          public void onDismiss(PopupMenu menu) {
            if (listener != null) listener.onPopupDismissed();
          }
        });

        popupMenu.show();
        if (listener != null) listener.onPopupShown();
      }
    });
  }

  public void removeItems() {
    items.clear();
    setWillNotDraw(true);
  }

  public void addItem(int action, int title) {
    items.add(new OverflowItem(action, title));
    setWillNotDraw(false);
  }

  public void setListener(OverflowActionListener listener) {
    this.listener = listener;
  }
}
