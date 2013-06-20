package net.simonvt.trakt.provider;

import net.simonvt.trakt.util.DateUtils;

import android.content.Context;
import android.database.Cursor;
import android.database.MergeCursor;
import android.support.v4.content.AsyncTaskLoader;

public class CollectLoader extends AsyncTaskLoader<Cursor> {

    private static final String TAG = "CollectLoader";

    final ForceLoadContentObserver mObserver;

    private long mShowId;

    Cursor mCursor;

    public CollectLoader(Context context, long showId) {
        super(context);
        mShowId = showId;
        mObserver = new ForceLoadContentObserver();
    }

    @Override
    public Cursor loadInBackground() {
        Cursor toCollect =
                getContext().getContentResolver().query(TraktContract.Episodes.buildFromShowId(mShowId), null,
                        TraktContract.Episodes.IN_COLLECTION + "=0 AND " + TraktContract.Episodes.FIRST_AIRED + ">"
                                + DateUtils.YEAR_IN_SECONDS + " AND " + TraktContract.Episodes.SEASON + ">0", null,
                        TraktContract.Episodes.SEASON + " ASC, " + TraktContract.Episodes.EPISODE + " ASC LIMIT 1");
        if (toCollect.getCount() == 0) {
            return toCollect;
        }

        Cursor lastCollected =
                getContext().getContentResolver().query(TraktContract.Episodes.buildFromShowId(mShowId), null,
                        TraktContract.Episodes.IN_COLLECTION + "=1", null, TraktContract.Episodes.SEASON + " DESC, "
                        + TraktContract.Episodes.EPISODE + " DESC LIMIT 1");
        lastCollected.getCount();

        return new MergeCursor(new Cursor[] {
                toCollect,
                lastCollected,
        });
    }

    @Override
    protected void onStartLoading() {
        if (mCursor != null) {
            deliverResult(mCursor);
        }
        if (takeContentChanged() || mCursor == null) {
            forceLoad();
        }
    }

    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

    @Override
    public void onCanceled(Cursor cursor) {
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
    }

    @Override
    protected void onReset() {
        super.onReset();

        // Ensure the loader is stopped
        onStopLoading();

        if (mCursor != null && !mCursor.isClosed()) {
            mCursor.close();
        }
        mCursor = null;
    }
}
