/*
 * Copyright (C) 2013 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.service;

import android.accounts.Account;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SharedPreferences;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.preference.PreferenceManager;
import android.provider.BaseColumns;
import android.provider.CalendarContract;
import android.provider.ContactsContract;
import android.text.format.Time;
import android.util.LongSparseArray;
import java.util.ArrayList;
import net.simonvt.cathode.R;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.settings.Permissions;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.util.DateUtils;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class CalendarSyncAdapter extends AbstractThreadedSyncAdapter {

  private static class Event {

    long id;

    long start;

    long end;

    long episodeId;

    public Event(Cursor c) {
      id = Cursors.getLong(c, CalendarContract.Events._ID);
      start = Cursors.getLong(c, CalendarContract.Events.DTSTART);
      end = Cursors.getLong(c, CalendarContract.Events.DTEND);
      episodeId = Cursors.getLong(c, CalendarContract.Events.SYNC_DATA1);
    }
  }

  private static final long INVALID_ID = -1L;

  private Context context;

  public CalendarSyncAdapter(Context context) {
    super(context, true);
    this.context = context;
  }

  @Override public void onPerformSync(Account account, Bundle extras, String authority,
      ContentProviderClient provider, SyncResult syncResult) {
    Timber.d("onPerformSync");

    if (!Permissions.hasCalendarPermission(context)) {
      Timber.d("Calendar permission not granted");
      return;
    }

    final long calendarId = getCalendar(account);
    if (calendarId == INVALID_ID) {
      return;
    }

    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
    final boolean syncCalendar = settings.getBoolean(Settings.CALENDAR_SYNC, false);
    if (!syncCalendar) {
      Timber.d("Deleting calendar");
      //noinspection MissingPermission
      context.getContentResolver()
          .delete(CalendarContract.Events.CONTENT_URI, CalendarContract.Events.CALENDAR_ID + "=?",
              new String[] {
                  String.valueOf(calendarId),
              });

      Uri calenderUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
          .build();
      context.getContentResolver().delete(calenderUri, null, null);
      return;
    }

    //noinspection MissingPermission
    Cursor c =
        context.getContentResolver().query(CalendarContract.Events.CONTENT_URI, new String[] {
            CalendarContract.Events._ID, CalendarContract.Events.DTSTART,
            CalendarContract.Events.DTEND, CalendarContract.Events.SYNC_DATA1,
        }, CalendarContract.Events.CALENDAR_ID + "=?", new String[] {
            String.valueOf(calendarId),
        }, null);

    if (c == null) {
      return;
    }

    LongSparseArray<Event> events = new LongSparseArray<>();
    while (c.moveToNext()) {
      events.put(Cursors.getLong(c, CalendarContract.Events.SYNC_DATA1), new Event(c));
    }
    c.close();

    final long time = System.currentTimeMillis();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    Cursor shows = context.getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, "("
        + ShowColumns.WATCHED_COUNT
        + ">0 OR "
        + ShowColumns.IN_WATCHLIST_COUNT
        + ">0 OR "
        + ShowColumns.IN_COLLECTION_COUNT
        + ">0"
        + ") AND "
        + ShowColumns.HIDDEN_CALENDAR
        + "=0"
        + " AND "
        + ShowColumns.NEEDS_SYNC
        + "=0", null, null);

    while (shows.moveToNext()) {
      Cursor episodes = context.getContentResolver().query(Episodes.EPISODES, new String[] {
          EpisodeColumns.ID, EpisodeColumns.SHOW_ID, EpisodeColumns.TITLE, EpisodeColumns.SEASON,
          EpisodeColumns.EPISODE, EpisodeColumns.FIRST_AIRED,
      }, EpisodeColumns.SHOW_ID
          + "=? AND "
          + EpisodeColumns.FIRST_AIRED
          + ">? AND "
          + EpisodeColumns.NEEDS_SYNC
          + "=0", new String[] {
          String.valueOf(shows.getLong(0)), String.valueOf(time - 30 * DateUtils.DAY_IN_MILLIS),
      }, null);

      while (episodes.moveToNext()) {
        final long id = Cursors.getLong(episodes, EpisodeColumns.ID);
        final long showId = Cursors.getLong(episodes, EpisodeColumns.SHOW_ID);
        final String title = Cursors.getString(episodes, EpisodeColumns.TITLE);
        final int season = Cursors.getInt(episodes, EpisodeColumns.SEASON);
        final int episode = Cursors.getInt(episodes, EpisodeColumns.EPISODE);
        final long firstAired = Cursors.getLong(episodes, EpisodeColumns.FIRST_AIRED);

        Cursor show = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.TITLE, ShowColumns.RUNTIME,
        }, null, null, null);
        show.moveToFirst();

        final String showTitle = Cursors.getString(show, ShowColumns.TITLE);
        final long runtime = Cursors.getLong(show, ShowColumns.RUNTIME);

        show.close();

        String eventTitle = showTitle + " - " + season + "x" + episode + " " + title;

        Event event = events.get(id);
        if (event != null) {
          ContentProviderOperation op =
              ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI)
                  .withValue(CalendarContract.Events.DTSTART, firstAired)
                  .withValue(CalendarContract.Events.DTEND,
                      firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
                  .withValue(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
                  .withValue(CalendarContract.Events.TITLE, eventTitle)
                  .withSelection(CalendarContract.Events._ID + "=?", new String[] {
                      String.valueOf(event.id),
                  })
                  .build();
          ops.add(op);
          events.remove(id);
        } else {
          ContentProviderOperation op = ContentProviderOperation.newInsert(
              CalendarContract.Events.CONTENT_URI.buildUpon()
                  .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
                  .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME,
                      context.getString(R.string.accountName))
                  .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE,
                      context.getString(R.string.accountType))
                  .build())
              .withValue(CalendarContract.Events.DTSTART, firstAired)
              .withValue(CalendarContract.Events.DTEND,
                  firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
              .withValue(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
              .withValue(CalendarContract.Events.TITLE, eventTitle)
              .withValue(CalendarContract.Events.SYNC_DATA1, id)
              .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
              .build();
          ops.add(op);
        }
      }
      episodes.close();
    }
    shows.close();

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
    return getCalendar(account, true);
  }

  private long getCalendar(Account account, boolean recall) {
    Cursor c = null;
    try {
      Uri calenderUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
          .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
          .build();
      c = context.getContentResolver().query(calenderUri, new String[] {
          BaseColumns._ID,
      }, null, null, null);

      if (c == null) {
        return INVALID_ID;
      }

      if (c.moveToNext()) {
        return c.getLong(0);
      } else {
        if (!recall) {
          return INVALID_ID;
        }

        ArrayList<ContentProviderOperation> operationList = new ArrayList<>();

        ContentProviderOperation.Builder builder = ContentProviderOperation.newInsert(
            CalendarContract.Calendars.CONTENT_URI.buildUpon()
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
          return INVALID_ID;
        }
        return getCalendar(account, false);
      }
    } finally {
      if (c != null) c.close();
    }
  }
}
