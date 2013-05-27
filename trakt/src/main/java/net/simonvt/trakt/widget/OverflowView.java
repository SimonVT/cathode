package net.simonvt.trakt.widget;

import net.simonvt.trakt.R;

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

public class OverflowView extends ImageView {

    public interface OverflowActionListener {

        void onPopupShown();

        void onPopupDismissed();

        void onActionSelected(int action);
    }

    public static class OverflowItem {

        private int mAction;

        private int mTitle;

        public OverflowItem(int action, int title) {
            mAction = action;
            mTitle = title;
        }
    }

    private List<OverflowItem> mItems = new ArrayList<OverflowItem>();

    private OverflowActionListener mListener;

    private Rect mOverflowRect = new Rect();
    private TouchDelegate mOverflowDelegate;

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

        if (changed || mOverflowDelegate == null) {
            final int extraDim =
                    (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 40,
                            getResources().getDisplayMetrics());

            mOverflowRect.left = right - width - extraDim;
            mOverflowRect.top = top;
            mOverflowRect.right = right;
            mOverflowRect.bottom = bottom + extraDim;
            mOverflowDelegate = new TouchDelegate(mOverflowRect, this);
            ((ViewGroup) getParent()).setTouchDelegate(mOverflowDelegate);
        }
    }

    private void init(final Context context) {
        setImageResource(R.drawable.item_overflow);
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                PopupMenu popupMenu = new PopupMenu(context, OverflowView.this);

                for (OverflowItem item : mItems) {
                    popupMenu.getMenu().add(0, item.mAction, 0, item.mTitle);
                }

                popupMenu.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                    @Override
                    public boolean onMenuItemClick(MenuItem item) {
                        if (mListener != null) mListener.onActionSelected(item.getItemId());
                        return true;
                    }
                });

                popupMenu.setOnDismissListener(new PopupMenu.OnDismissListener() {
                    @Override
                    public void onDismiss(PopupMenu menu) {
                        if (mListener != null) mListener.onPopupDismissed();
                    }
                });

                popupMenu.show();
                if (mListener != null) mListener.onPopupShown();
            }
        });
    }

    public void removeItems() {
        mItems.clear();
    }

    public void addItem(int action, int title) {
        mItems.add(new OverflowItem(action, title));
    }

    public void setListener(OverflowActionListener listener) {
        mListener = listener;
    }
}
