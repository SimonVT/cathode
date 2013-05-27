package net.simonvt.trakt.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.ViewGroup;

public abstract class AbsShowView extends ViewGroup {

    public AbsShowView(Context context) {
        super(context);
    }

    public AbsShowView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsShowView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }
}
