package net.simonvt.trakt.service;

import com.google.android.apps.dashclock.api.DashClockExtension;
import com.google.android.apps.dashclock.api.ExtensionData;

import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.util.DateUtils;

import android.database.Cursor;

public class DashClockService extends DashClockExtension {

    @Override
    protected void onUpdateData(int reason) {

        Cursor c = getContentResolver().query(TraktContract.Episodes.CONTENT_URI, null,
                TraktContract.Episodes.FIRST_AIRED + ">? AND " + TraktContract.Episodes.WATCHED + "=0", new String[] {
                String.valueOf(System.currentTimeMillis()),
        }, TraktContract.Episodes.FIRST_AIRED + " ASC LIMIT 1");

        if (c.moveToFirst()) {
            final int episode = c.getInt(c.getColumnIndex(TraktContract.Episodes.EPISODE));
            final int season = c.getInt(c.getColumnIndex(TraktContract.Episodes.SEASON));
            final String title = c.getString(c.getColumnIndex(TraktContract.Episodes.TITLE));
            final long showId = c.getLong(c.getColumnIndex(TraktContract.Episodes.SHOW_ID));
            final long firstAired = c.getLong(c.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));

            final String date = DateUtils.millisToString(this, firstAired, false);

            Cursor show = getContentResolver().query(TraktContract.Shows.buildShowUri(showId), null, null, null, null);
            if (!show.moveToFirst()) return; // Wat
            final String showTitle = show.getString(show.getColumnIndex(TraktContract.Shows.TITLE));

            ExtensionData data = new ExtensionData()
                    .visible(true)
                    .status(date)
                    .expandedTitle(showTitle + " - " + date)
                    .expandedBody(season + "x" + episode + " - " + title);

            publishUpdate(data);
        } else {
            publishUpdate(new ExtensionData().visible(false));
        }
    }
}
