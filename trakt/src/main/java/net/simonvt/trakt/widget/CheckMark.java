package net.simonvt.trakt.widget;

import net.simonvt.trakt.R;
import net.simonvt.trakt.ui.LibraryType;

import android.content.Context;
import android.util.AttributeSet;
import android.widget.TextView;

public class CheckMark extends TextView {

    private LibraryType mType;

    public CheckMark(Context context) {
        super(context);
    }

    public CheckMark(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CheckMark(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setType(LibraryType type) {
        mType = type;

        switch (type) {
            case COLLECTION:
                setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark_collection, 0);
                setText(R.string.checkmark_collection);
                setTextColor(getResources().getColor(android.R.color.holo_orange_dark));
                break;

            case WATCHED:
                setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark_watched, 0);
                setText(R.string.checkmark_watched);
                setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
                break;

            case WATCHLIST:
                setCompoundDrawablesWithIntrinsicBounds(0, 0, R.drawable.ic_checkmark_watchlist, 0);
                setText(R.string.checkmark_watchlist);
                setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                break;
        }
    }
}
