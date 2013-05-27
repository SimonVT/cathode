package net.simonvt.trakt.ui.adapter;

import net.simonvt.trakt.R;
import net.simonvt.trakt.ui.LibraryType;
import net.simonvt.trakt.widget.AbsEpisodeView;

import android.content.Context;
import android.database.Cursor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CursorAdapter;

public class SeasonAdapter extends CursorAdapter {

    private static final String TAG = "SeasonAdapter";

    private LibraryType mType;

    public SeasonAdapter(Context context, LibraryType type) {
        super(context, null, 0);
        mType = type;
    }

    @Override
    public void changeCursor(Cursor cursor) {
        super.changeCursor(cursor);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        AbsEpisodeView v =
                (AbsEpisodeView) LayoutInflater.from(context).inflate(R.layout.list_row_episode, parent, false);
        v.setType(mType);
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ((AbsEpisodeView) view).bindData(cursor);
    }
}
