package net.simonvt.trakt.ui.adapter;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class MovieSearchAdapter extends CursorAdapter {

    private static final String TAG = "MovieSearchAdapter";

    public MovieSearchAdapter(Context context) {
        super(context, null, 0);
        mContext = context;
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.list_row_search_movie, parent, false);
        v.setTag(new ViewHolder(v));
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder vh = (ViewHolder) view.getTag();

        vh.mPoster.setImage(cursor.getString(cursor.getColumnIndex(TraktContract.Movies.POSTER)));
        vh.mTitle.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Movies.TITLE)));
        vh.mOverview.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Movies.OVERVIEW)));
        vh.mOverflow.setListener(new OverflowView.OverflowActionListener() {
            @Override
            public void onPopupShown() {
            }

            @Override
            public void onPopupDismissed() {
            }

            @Override
            public void onActionSelected(int action) {
                switch (action) {

                }
            }
        });
    }

    static class ViewHolder {

        @InjectView(R.id.poster) RemoteImageView mPoster;
        @InjectView(R.id.title) TextView mTitle;
        @InjectView(R.id.overview) TextView mOverview;
        @InjectView(R.id.overflow) OverflowView mOverflow;

        ViewHolder(View v) {
            Views.inject(this, v);
        }
    }
}
