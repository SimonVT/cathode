package net.simonvt.trakt.ui.adapter;

import butterknife.InjectView;
import butterknife.Views;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.scheduler.EpisodeTaskScheduler;
import net.simonvt.trakt.util.DateUtils;
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

public class EpisodeWatchlistAdapter extends CursorAdapter {

    private static final String TAG = "EpisodeWatchlistAdapter";

    @Inject EpisodeTaskScheduler mEpisodeScheduler;

    public EpisodeWatchlistAdapter(Context context, Cursor c, int flags) {
        super(context, c, flags);
        TraktApp.inject(context, this);
    }

    @Override
    public View newView(Context context, Cursor cursor, ViewGroup parent) {
        View v = LayoutInflater.from(context).inflate(R.layout.list_row_watchlist_episode, parent, false);

        ViewHolder vh = new ViewHolder(v);
        v.setTag(vh);

        vh.mOverflow.addItem(R.id.action_watched, R.string.action_watched);
        vh.mOverflow.addItem(R.id.action_watchlist_remove, R.string.action_watchlist_remove);

        return v;
    }

    @Override
    public void bindView(View view, Context context, Cursor cursor) {
        ViewHolder vh = (ViewHolder) view.getTag();

        final long id = cursor.getLong(cursor.getColumnIndex(TraktContract.Episodes._ID));
        final String posterUrl = cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SCREEN));
        final String title = cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.TITLE));
        final long firstAired = cursor.getLong(cursor.getColumnIndexOrThrow(TraktContract.Episodes.FIRST_AIRED));
        final int season = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SEASON));
        final int episode = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.EPISODE));

        vh.mScreen.setImage(posterUrl);
        vh.mTitle.setText(title);
        vh.mFirstAired.setText(DateUtils.secondsToDate(context, firstAired));
        vh.mEpisode.setText(season + "x" + episode);
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
                        mEpisodeScheduler.setWatched(id, true);
                        break;

                    case R.id.action_watchlist_remove:
                        mEpisodeScheduler.setIsInWatchlist(id, false);
                        break;
                }
            }
        });
    }

    static class ViewHolder {

        @InjectView(R.id.screen) RemoteImageView mScreen;
        @InjectView(R.id.title) TextView mTitle;
        @InjectView(R.id.firstAired) TextView mFirstAired;
        @InjectView(R.id.episode) TextView mEpisode;
        @InjectView(R.id.overflow) OverflowView mOverflow;

        public ViewHolder(View v) {
            Views.inject(this, v);
        }
    }
}
