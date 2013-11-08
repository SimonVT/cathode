package net.simonvt.cathode.service;

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
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.cathode.util.LogWrapper;

public class CalendarSyncAdapter extends AbstractThreadedSyncAdapter {

  private static final String TAG = "CalendarSyncAdapter";

  private static class Event {

    long id;

    long start;

    long end;

    long episodeId;

    public Event(Cursor c) {
      id = c.getLong(c.getColumnIndex(CalendarContract.Events._ID));
      start = c.getLong(c.getColumnIndex(CalendarContract.Events.DTSTART));
      end = c.getLong(c.getColumnIndex(CalendarContract.Events.DTEND));
      episodeId = c.getLong(c.getColumnIndex(CalendarContract.Events.SYNC_DATA1));
    }
  }

  private Context context;

  public CalendarSyncAdapter(Context context) {
    super(context, true);
    this.context = context;
  }

  @Override public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    LogWrapper.v(TAG, "[onPerformSync]");
    final long calendarId = getCalendar(account);

    Cursor c =
        context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, new String[] {
            CalendarContract.Events._ID, CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND, CalendarContract.Events.SYNC_DATA1,
        }, CalendarContract.Events.CALENDAR_ID + "=?", new String[] {
            String.valueOf(calendarId),
        }, null);

    LongSparseArray<Event> events = new LongSparseArray<Event>();
    while (c.moveToNext()) {
      events.put(c.getLong(c.getColumnIndex(CalendarContract.Events.SYNC_DATA1)), new Event(c));
    }
    c.close();

    final long time = System.currentTimeMillis();

    ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

    Cursor shows =
        context.getContentResolver().query(CathodeContract.Shows.CONTENT_URI, new String[] {
            CathodeContract.Shows._ID,
        }, CathodeContract.Shows.WATCHED_COUNT
            + ">0 OR "
            + CathodeContract.Shows.IN_WATCHLIST_COUNT
            + ">0", null, null);

    while (shows.moveToNext()) {
      Cursor episodes =
          context.getContentResolver().query(CathodeContract.Episodes.CONTENT_URI, new String[] {
              CathodeContract.Episodes._ID, CathodeContract.Episodes.SHOW_ID,
              CathodeContract.Episodes.TITLE, CathodeContract.Episodes.SEASON,
              CathodeContract.Episodes.EPISODE, CathodeContract.Episodes.FIRST_AIRED,
          }, CathodeContract.Episodes.SHOW_ID
              + "=? AND "
              + CathodeContract.Episodes.FIRST_AIRED
              + ">?", new String[] {
              String.valueOf(shows.getLong(0)), String.valueOf(time - 30 * DateUtils.DAY_IN_MILLIS),
          }, null);

      while (episodes.moveToNext()) {
        final long id = episodes.getLong(episodes.getColumnIndex(CathodeContract.Episodes._ID));
        final long showId =
            episodes.getLong(episodes.getColumnIndex(CathodeContract.Episodes.SHOW_ID));
        final String title =
            episodes.getString(episodes.getColumnIndex(CathodeContract.Episodes.TITLE));
        final int season =
            episodes.getInt(episodes.getColumnIndex(CathodeContract.Episodes.SEASON));
        final int episode =
            episodes.getInt(episodes.getColumnIndex(CathodeContract.Episodes.EPISODE));
        final long firstAired =
            episodes.getLong(episodes.getColumnIndex(CathodeContract.Episodes.FIRST_AIRED));

        Cursor show = context.getContentResolver()
            .query(CathodeContract.Shows.buildFromId(showId), new String[] {
                CathodeContract.Shows.TITLE, CathodeContract.Shows.RUNTIME,
            }, null, null, null);
        show.moveToFirst();

        final String showTitle = show.getString(show.getColumnIndex(CathodeContract.Shows.TITLE));
        final long runtime = show.getLong(show.getColumnIndex(CathodeContract.Shows.RUNTIME));

        String eventTitle = showTitle + " - " + season + "x" + episode + " " + title;

        Event event = events.get(id);
        if (event != null) {
          ContentProviderOperation op =
              ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI)
                  .withValue(CalendarContract.Events.DTSTART, firstAired)
                  .withValue(CalendarContract.Events.DTEND,
                      firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
                  .withValue(CalendarContract.Events.TITLE, eventTitle)
                  .withSelection(CalendarContract.Events._ID + "=?", new String[] {
                      String.valueOf(event.id),
                  })
                  .build();
          ops.add(op);
          events.remove(id);
        } else {
          ContentProviderOperation op = ContentProviderOperation.newInsert(CalendarContract.Events
              .CONTENT_URI
              .buildUpon()
              .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
              .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME,
                  context.getString(R.string.accountName))
              .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,
                  context.getString(R.string.accountType))
              .build())
              .withValue(CalendarContract.Events.DTSTART, firstAired)
              .withValue(CalendarContract.Events.DTEND,
                  firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
              .withValue(CalendarContract.Events.TITLE, eventTitle)
              .withValue(CalendarContract.Events.SYNC_DATA1, id)
              .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
              .build();
          ops.add(op);
        }
      }
      episodes.close();
    }

    for (int i = 0, size = events.size(); i < size; i++) {
      Event event = events.valueAt(i);
      ContentProviderOperation op =
          ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
              .withSelection(CalendarContract.Events._ID + "=?", new String[] {
                  String.valueOf(event.id),
              })
              .build();
      ops.add(op);
    }

    try {
      context.getContentResolver().applyBatch(CalendarContract.AUTHORITY, ops);
    } catch (RemoteException e) {
      e.printStackTrace();
    } catch (OperationApplicationException e) {
      e.printStackTrace();
    }
  }

  private long getCalendar(Account account) {
    Cursor c = null;
    try {
      Uri calenderUri = CalendarContract.Calendars
          .CONTENT_URI
          .buildUpon()
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
          .build();
      c = context.getContentResolver().query(calenderUri, new String[] {
          BaseColumns._ID,
      }, null, null, null);
      if (c.moveToNext()) {
        return c.getLong(0);
      } else {
        ArrayList<ContentProviderOperation> operationList =
            new ArrayList<ContentProviderOperation>();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            CalendarContract.Calendars
                .CONTENT_URI
                .buildUpon()
                .appendQueryParameter(CalendarContract.CALLER_IS_SYNCADAPTER, "true")
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
                .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
                .build());
        builder.withValue(CalendarContract.Calendars.ACCOUNT_NAME, account.name);
        builder.withValue(CalendarContract.Calendars.ACCOUNT_TYPE, account.type);
        builder.withValue(CalendarContract.Calendars.NAME, context.getString(R.string.app_name));
        builder.withValue(CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
            context.getString(R.string.calendarName));
        builder.withValue(CalendarContract.Calendars.CALENDAR_COLOR, 0xD51007);
        builder.withValue(CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
            CalendarContract.Calendars.CAL_ACCESS_READ);
        builder.withValue(CalendarContract.Calendars.OWNER_ACCOUNT, account.name);
        builder.withValue(CalendarContract.Calendars.SYNC_EVENTS, 1);
        operationList.add(builder.build());
        try {
          context.getContentResolver().applyBatch(CalendarContract.AUTHORITY, operationList);
        } catch (Exception e) {
          e.printStackTrace();
          return -1L;
        }
        return getCalendar(account);
      }
    } finally {
      if (c != null) c.close();
    }
  }
}
