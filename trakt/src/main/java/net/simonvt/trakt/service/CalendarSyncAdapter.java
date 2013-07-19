package net.simonvt.trakt.service;

import net.simonvt.trakt.R;
import net.simonvt.trakt.provider.TraktContract;
import net.simonvt.trakt.util.DateUtils;
import net.simonvt.trakt.util.LogWrapper;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.util.LongSparseArray;

import java.util.ArrayList;

public class CalendarSyncAdapter extends AbstractThreadedSyncAdapter {

    private static final String TAG = "CalendarSyncAdapter";

    private static class Event {

        long mId;

        long mStart;

        long mEnd;

        long mEpisodeId;

        public Event(Cursor c) {
            mId = c.getLong(c.getColumnIndex(CalendarContract.Events._ID));
            mStart = c.getLong(c.getColumnIndex(CalendarContract.Events.DTSTART));
            mEnd = c.getLong(c.getColumnIndex(CalendarContract.Events.DTEND));
            mEpisodeId = c.getLong(c.getColumnIndex(CalendarContract.Events.SYNC_DATA1));
        }
    }

    private Context mContext;

    public CalendarSyncAdapter(Context context) {
        super(context, true);
        mContext = context;
    }

    @Override
    public void onPerformSync(Account account, Bundle extras, String authority, ContentProviderClient provider,
            SyncResult syncResult) {
        LogWrapper.v(TAG, "[onPerformSync]");
        final long calendarId = getCalendar(account);

        Cursor c = mContext.getContentResolver().query(CalendarContract.Events.CONTENT_URI, new String[] {
                CalendarContract.Events._ID,
                CalendarContract.Events.DTSTART,
                CalendarContract.Events.DTEND,
                CalendarContract.Events.SYNC_DATA1,
        }, CalendarContract.Events.CALENDAR_ID + "=?", new String[] {
                String.valueOf(calendarId),
        }, null);

        LongSparseArray<Event> events = new LongSparseArray<Event>();
        while (c.moveToNext()) {
            events.put(c.getLong(c.getColumnIndex(CalendarContract.Events.SYNC_DATA1)), new Event(c));
        }

        final long time = System.currentTimeMillis();

        Cursor episodes = mContext.getContentResolver().query(TraktContract.Episodes.CONTENT_URI, new String[] {
                TraktContract.Episodes._ID,
                TraktContract.Episodes.SHOW_ID,
                TraktContract.Episodes.TITLE,
                TraktContract.Episodes.SEASON,
                TraktContract.Episodes.EPISODE,
                TraktContract.Episodes.FIRST_AIRED,
        }, TraktContract.Episodes.FIRST_AIRED + ">?", new String[] {
                String.valueOf(time - 30 * DateUtils.DAY_IN_MILLIS),
        }, null);

        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

        while (episodes.moveToNext()) {
            final long id = episodes.getLong(episodes.getColumnIndex(TraktContract.Episodes._ID));
            final long showId = episodes.getLong(episodes.getColumnIndex(TraktContract.Episodes.SHOW_ID));
            final String title = episodes.getString(episodes.getColumnIndex(TraktContract.Episodes.TITLE));
            final int season = episodes.getInt(episodes.getColumnIndex(TraktContract.Episodes.SEASON));
            final int episode = episodes.getInt(episodes.getColumnIndex(TraktContract.Episodes.EPISODE));
            final long firstAired = episodes.getLong(episodes.getColumnIndex(TraktContract.Episodes.FIRST_AIRED));

            Cursor show = mContext.getContentResolver().query(TraktContract.Shows.buildShowUri(showId), new String[] {
                    TraktContract.Shows.TITLE,
                    TraktContract.Shows.RUNTIME,
            }, null, null, null);
            show.moveToFirst();

            final String showTitle = show.getString(show.getColumnIndex(TraktContract.Shows.TITLE));
            final long runtime = show.getLong(show.getColumnIndex(TraktContract.Shows.RUNTIME));

            String eventTitle = showTitle + " - " + season + "x" + episode + " " + title;

            Event event = events.get(id);
            if (event != null) {
                ContentProviderOperation op = ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI)
                        .withValue(CalendarContract.Events.DTSTART, firstAired)
                        .withValue(CalendarContract.Events.DTEND, firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
                        .withValue(CalendarContract.Events.TITLE, eventTitle)
                        .withSelection(CalendarContract.Events._ID + "=?", new String[] {
                                String.valueOf(event.mId),
                        })
                        .build();
                ops.add(op);
                events.remove(id);
            } else {
                ContentProviderOperation op = ContentProviderOperation.newInsert(
                        CalendarContract.Events.CONTENT_URI.buildUpon().appendQueryParameter(
                                ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME,
                                        mContext.getString(R.string.accountName))
                                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,
                                        mContext.getString(R.string.accountType)).build())
                        .withValue(CalendarContract.Events.DTSTART, firstAired)
                        .withValue(CalendarContract.Events.DTEND, firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
                        .withValue(CalendarContract.Events.TITLE, eventTitle)
                        .withValue(CalendarContract.Events.SYNC_DATA1, id)
                        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
                        .build();
                ops.add(op);
            }
        }

        for (int i = 0, size = events.size(); i < size; i++) {
            Event event = events.valueAt(i);
            ContentProviderOperation op = ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
                    .withSelection(CalendarContract.Events._ID + "=?", new String[] {
                            String.valueOf(event.mId),
                    })
                    .build();
            ops.add(op);
        }

        try {
            mContext.getContentResolver().applyBatch(CalendarContract.AUTHORITY, ops);
        } catch (RemoteException e) {
            e.printStackTrace();
        } catch (OperationApplicationException e) {
            e.printStackTrace();
        }
    }

    private long getCalendar(Account account) {
        Uri calenderUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
                .build();
        Cursor c = mContext.getContentResolver().query(calenderUri, new String[] {
                BaseColumns._ID,
        }, null, null, null);
        if (c.moveToNext()) {
            return c.getLong(0);
        } else {
            ArrayList<ContentProviderOperation> operationList = new ArrayList<ContentProviderOperation>();

            ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
                    CalendarContract.Calendars.CONTENT_URI.buildUpon()
                            .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                            .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
                            .build()
            );
            builder.withValue(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
            builder.withValue(CalendarContract.Calendars.ACCOUNT_TYPE, account.type);
            builder.withValue(CalendarContract.Calendars.NAME, mContext.getString(R.string.app_name));
            builder.withValue(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
                    mContext.getString(R.string.calendarName));
            builder.withValue(CalendarContract.Calendars.CALENDAR_COLOR, 0xD51007);
            builder.withValue(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
                    CalendarContract.Calendars.CAL_ACCESS_READ);
            builder.withValue(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
            builder.withValue(CalendarContract.Calendars.SYNC_EVENTS, 1);
            operationList.add(builder.build());
            try {
                mContext.getContentResolver().applyBatch(CalendarContract.AUTHORITY, operationList);
            } catch (Exception e) {
                e.printStackTrace();
                return -1L;
            }
            return getCalendar(account);
        }
    }
}
