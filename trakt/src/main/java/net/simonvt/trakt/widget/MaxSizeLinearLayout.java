package net.simonvt.trakt.widget;

import net.simonvt.trakt.R;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.LinearLayout;

public class MaxSizeLinearLayout extends LinearLayout {

    private int mMaxWidth = -1;

    private int mMaxHeight = -1;

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
        TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.MaxSizeLinearLayout, 0, defStyle);

        mMaxWidth = a.getDimensionPixelSize(R.styleable.MaxSizeLinearLayout_maxWidth, -1);
        mMaxHeight = a.getDimensionPixelSize(R.styleable.MaxSizeLinearLayout_maxHeight, -1);

        a.recycle();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        final int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);

        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        if (mMaxWidth != -1 && widthMode != MeasureSpec.UNSPECIFIED) {
            widthSize = Math.min(widthSize, mMaxWidth);
        }

        if (mMaxHeight != -1 && heightMode != MeasureSpec.UNSPECIFIED) {
            heightSize = Math.min(heightSize, mMaxHeight);
        }

        super.onMeasure(MeasureSpec.makeMeasureSpec(widthSize, widthMode),
                MeasureSpec.makeMeasureSpec(heightSize, heightMode));
    }
}
