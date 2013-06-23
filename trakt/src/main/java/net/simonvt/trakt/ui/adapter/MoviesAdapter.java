package net.simonvt.trakt.ui.adapter;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.MovieTaskScheduler;
import net.simonvt.trakt.widget.OverflowView;
import net.simonvt.trakt.widget.RemoteImageView;

import android.content.Context;
import android.database.Cursor;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import javax.inject.Inject;

public class MoviesAdapter extends CursorAdapter {

    private static final String TAG = "MoviesAdapter";

    @Inject MovieTaskScheduler mMovieScheduler;

    public MoviesAdapter(Context context, Cursor c) {
        super(context, c, 0);
        TraktApp.inject(context, this);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(mContext).inflate(R.layout.list_row_movie, parent, false);
        v.setTag(new ViewHolder(v));
        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder vh = (ViewHolder) view.getTag();

        final int tmdbId = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.TMDB_ID));
        final boolean watched = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.WATCHED)) == 1;
        final boolean inCollection = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.IN_COLLECTION)) == 1;
        final boolean inWatchlist = cursor.getInt(cursor.getColumnIndex(TraktContract.Movies.IN_WATCHLIST)) == 1;

        vh.mPoster.setImage(cursor.getString(cursor.getColumnIndex(TraktContract.Movies.POSTER)));
        vh.mTitle.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Movies.TITLE)));
        vh.mOverview.setText(cursor.getString(cursor.getColumnIndex(TraktContract.Movies.OVERVIEW)));

        vh.mOverflow.removeItems();
        if (watched) {
            vh.mOverflow.addItem(R.id.action_unwatched, R.string.action_unwatched);
        } else if (inWatchlist) {
            vh.mOverflow.addItem(R.id.action_watched, R.string.action_watched);
            vh.mOverflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);
        } else {
            vh.mOverflow.addItem(R.id.action_watched, R.string.action_watched);
            vh.mOverflow.addItem(R.id.action_watchlist_add, R.string.action_watchlist_add);
        }

        if (inCollection) {
            vh.mOverflow.addItem(R.id.action_collection_remove, R.string.action_collection_remove);
        } else {
            vh.mOverflow.addItem(R.id.action_collection_add, R.string.action_collection_add);
        }

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
                    case R.id.action_watched:
                        mMovieScheduler.setWatched(tmdbId, true);
                        break;

                    case R.id.action_unwatched:
                        mMovieScheduler.setWatched(tmdbId, false);
                        break;

                    case R.id.action_watchlist_add:
                        mMovieScheduler.setIsInWatchlist(tmdbId, true);
                        break;

                    case R.id.action_watchlist_remove:
                        mMovieScheduler.setIsInWatchlist(tmdbId, false);
                        break;

                    case R.id.action_collection_add:
                        mMovieScheduler.setIsInCollection(tmdbId, true);
                        break;

                    case R.id.action_collection_remove:
                        mMovieScheduler.setIsInCollection(tmdbId, false);
                        break;
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
