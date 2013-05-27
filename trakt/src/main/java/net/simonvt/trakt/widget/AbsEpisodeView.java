package net.simonvt.trakt.widget;

import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.ui.LibraryType;

import android.content.Context;
import android.database.Cursor;
import android.util.AttributeSet;
import android.view.ViewGroup;

public abstract class AbsEpisodeView extends ViewGroup {

    protected long mEpisodeId;
    protected String mEpisodeTitle;
    protected int mSeasonNumber;
    protected int mEpisodeNumber;
    protected boolean mEpisodeWatched;
    protected boolean mEpisodeInCollection;
    protected boolean mEpisodeInWatchlist;
    protected long mEpisodeAired;
    protected String mEpisodeScreenUrl;

    protected LibraryType mType;

    public AbsEpisodeView(Context context) {
        super(context);
    }

    public AbsEpisodeView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AbsEpisodeView(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
    }

    public void setType(LibraryType type) {
        mType = type;
    }

    public void bindData(Cursor cursor) {
        mEpisodeId = cursor.getLong(cursor.getColumnIndexOrThrow(TraktContract.Episodes._ID));
        mEpisodeTitle = cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.TITLE));
        mSeasonNumber = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SEASON));
        mEpisodeNumber = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.EPISODE));
        mEpisodeWatched = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.WATCHED)) == 1;
        mEpisodeInCollection = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.IN_COLLECTION)) == 1;
        mEpisodeInWatchlist = cursor.getInt(cursor.getColumnIndexOrThrow(TraktContract.Episodes.IN_WATCHLIST)) == 1;
        mEpisodeAired = cursor.getLong(cursor.getColumnIndexOrThrow(TraktContract.Episodes.FIRST_AIRED));
        mEpisodeScreenUrl = cursor.getString(cursor.getColumnIndexOrThrow(TraktContract.Episodes.SCREEN));
        onDataBound();
    }

    protected abstract void onDataBound();
}
