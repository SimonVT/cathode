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
package net.simonvt.cathode.calendar

import android.accounts.Account
import android.content.AbstractThreadedSyncAdapter
import android.content.ContentProviderClient
import android.content.ContentProviderOperation
import android.content.ContentValues
import android.content.Context
import android.content.OperationApplicationException
import android.content.SyncResult
import android.database.Cursor
import android.os.Bundle
import android.os.RemoteException
import android.provider.BaseColumns
import android.provider.CalendarContract
import android.provider.ContactsContract
import android.text.format.DateUtils
import android.text.format.Time
import net.simonvt.cathode.CathodeApp
import net.simonvt.cathode.R
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getLongOrNull
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.settings.Permissions
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.ui.EpisodeDetailsActivity
import timber.log.Timber
import java.util.ArrayList

class CalendarSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true) {

  private val calendarColor: Int
    get() = Settings.get(context).getInt(Settings.CALENDAR_COLOR, Settings.CALENDAR_COLOR_DEFAULT)

  private class Event(cursor: Cursor) {

    val id = cursor.getLong(CalendarContract.Events._ID)
    val start = cursor.getLong(CalendarContract.Events.DTSTART)
    val end = cursor.getLong(CalendarContract.Events.DTEND)
    val episodeId = cursor.getLong(CalendarContract.Events.SYNC_DATA1)
  }

  override fun onPerformSync(
    account: Account,
    extras: Bundle,
    authority: String,
    provider: ContentProviderClient,
    syncResult: SyncResult
  ) {
    Timber.d("onPerformSync")
    // This might be true when Android restores backup on install.
    if (context.applicationContext !is CathodeApp) {
      return
    }

    if (!Permissions.hasCalendarPermission(context)) {
      Timber.d("Calendar permission not granted")
      return
    }

    val calendarId = getCalendar(account)
    if (calendarId == INVALID_ID) {
      return
    }

    val syncCalendar = Settings.get(context).getBoolean(Settings.CALENDAR_SYNC, false)
    if (!syncCalendar) {
      deleteCalendar(account, calendarId)
      return
    }

    val updateCalendarColor =
      Settings.get(context).getBoolean(Settings.CALENDAR_COLOR_NEEDS_UPDATE, false)
    if (updateCalendarColor) {
      updateCalendarColor(calendarId)
    }

    val ops = ArrayList<ContentProviderOperation>()

    val c = context.contentResolver.query(
      CalendarContract.Events.CONTENT_URI,
      arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.SYNC_DATA1
      ),
      CalendarContract.Events.CALENDAR_ID + "=?",
      arrayOf(calendarId.toString()),
      null
    )

    if (c == null) {
      return
    }

    val events = mutableMapOf<Long, Event>()
    while (c.moveToNext()) {
      val id = c.getLong(CalendarContract.Events._ID)
      val episodeId = c.getLongOrNull(CalendarContract.Events.SYNC_DATA1)
      if (episodeId != null) {
        val event = events[episodeId]
        val newEvent = Event(c)
        if (event == null) {
          events[episodeId] = newEvent
        } else {
          val op = ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
            .withSelection(CalendarContract.Events._ID + "=?", arrayOf(id.toString()))
            .build()
          ops.add(op)
        }
      } else {
        val op = ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
          .withSelection(CalendarContract.Events._ID + "=?", arrayOf(id.toString()))
          .build()
        ops.add(op)
      }
    }
    c.close()

    val time = System.currentTimeMillis()

    val shows = context.contentResolver.query(
      Shows.SHOWS,
      arrayOf(ShowColumns.ID, ShowColumns.TITLE, ShowColumns.RUNTIME),
      "(" + ShowColumns.WATCHED_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST_COUNT + ">0 OR " +
          ShowColumns.IN_COLLECTION_COUNT + ">0" + ") AND " +
          ShowColumns.HIDDEN_CALENDAR + "=0" + " AND " +
          ShowColumns.LAST_SYNC + ">0"
    )

    while (shows.moveToNext()) {
      val showId = Cursors.getLong(shows, ShowColumns.ID)
      val showTitle = Cursors.getString(shows, ShowColumns.TITLE)
      val runtime = Cursors.getLong(shows, ShowColumns.RUNTIME)

      val episodes = context.contentResolver.query(
        Episodes.fromShow(showId),
        arrayOf(
          EpisodeColumns.ID,
          EpisodeColumns.TITLE,
          EpisodeColumns.SEASON,
          EpisodeColumns.EPISODE,
          EpisodeColumns.WATCHED,
          EpisodeColumns.FIRST_AIRED
        ),
        EpisodeColumns.FIRST_AIRED + ">?",
        arrayOf((time - 30 * DateUtils.DAY_IN_MILLIS).toString()),
        null
      )

      while (episodes!!.moveToNext()) {
        val episodeId = Cursors.getLong(episodes, EpisodeColumns.ID)
        val season = Cursors.getInt(episodes, EpisodeColumns.SEASON)
        val episode = Cursors.getInt(episodes, EpisodeColumns.EPISODE)
        val watched = Cursors.getBoolean(episodes, EpisodeColumns.WATCHED)
        val episodeTitle =
          DataHelper.getEpisodeTitle(context, episodes, season, episode, watched, true)
        val firstAired = DataHelper.getFirstAired(episodes)

        val eventTitle = "$showTitle - $episodeTitle"

        addEvent(ops, events, calendarId, episodeId, eventTitle, firstAired, runtime)
      }
      episodes.close()
    }
    shows.close()

    val watchlistShows = context.contentResolver.query(
      Shows.SHOWS_WATCHLIST,
      arrayOf(ShowColumns.ID, ShowColumns.TITLE, ShowColumns.RUNTIME)
    )

    while (watchlistShows.moveToNext()) {
      val showId = Cursors.getLong(watchlistShows, ShowColumns.ID)
      val showTitle = Cursors.getString(watchlistShows, ShowColumns.TITLE)
      val runtime = Cursors.getLong(watchlistShows, ShowColumns.RUNTIME)

      val episodes = context.contentResolver.query(
        Episodes.fromShow(showId),
        arrayOf(
          EpisodeColumns.ID,
          EpisodeColumns.TITLE,
          EpisodeColumns.SEASON,
          EpisodeColumns.EPISODE,
          EpisodeColumns.WATCHED,
          EpisodeColumns.FIRST_AIRED
        ),
        EpisodeColumns.FIRST_AIRED + ">? AND " +
            EpisodeColumns.EPISODE + "=1 AND " +
            EpisodeColumns.SEASON + ">0",
        arrayOf((time - 30 * DateUtils.DAY_IN_MILLIS).toString()),
        EpisodeColumns.SEASON + " ASC LIMIT 1"
      )

      if (episodes!!.moveToFirst()) {
        val episodeId = Cursors.getLong(episodes, EpisodeColumns.ID)
        val season = Cursors.getInt(episodes, EpisodeColumns.SEASON)
        val episode = Cursors.getInt(episodes, EpisodeColumns.EPISODE)
        val watched = Cursors.getBoolean(episodes, EpisodeColumns.WATCHED)
        val title = DataHelper.getEpisodeTitle(context, episodes, season, episode, watched)
        val firstAired = DataHelper.getFirstAired(episodes)

        val eventTitle = "$showTitle - ${season}x$episode $title"

        addEvent(ops, events, calendarId, episodeId, eventTitle, firstAired, runtime)
      }

      episodes.close()
    }

    watchlistShows.close()

    for (event in events.values) {
      val op = ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
        .withSelection(CalendarContract.Events._ID + "=?", arrayOf(event.id.toString()))
        .build()
      ops.add(op)
    }

    try {
      context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ops)
    } catch (e: RemoteException) {
      Timber.d(e)
    } catch (e: OperationApplicationException) {
      Timber.d(e)
    }
  }

  private fun addEvent(
    ops: MutableList<ContentProviderOperation>,
    events: MutableMap<Long, Event>,
    calendarId: Long,
    episodeId: Long,
    eventTitle: String,
    firstAired: Long,
    runtime: Long
  ) {
    val event = events[episodeId]
    if (event != null) {
      val op = ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI)
        .withValue(CalendarContract.Events.DTSTART, firstAired)
        .withValue(CalendarContract.Events.DTEND, firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
        .withValue(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
        .withValue(CalendarContract.Events.TITLE, eventTitle)
        .withSelection(CalendarContract.Events._ID + "=?", arrayOf(event.id.toString()))
        .build()
      ops.add(op)
      events.remove(episodeId)
    } else {
      val op = ContentProviderOperation.newInsert(
        CalendarContract.Events.CONTENT_URI.buildUpon()
          .appendQueryParameter(ContactsContract.CALLER_IS_SYNCADAPTER, "true")
          .appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_NAME,
            context.getString(R.string.accountName)
          )
          .appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_TYPE,
            context.getString(R.string.accountType)
          )
          .build()
      )
        .withValue(CalendarContract.Events.DTSTART, firstAired)
        .withValue(CalendarContract.Events.DTEND, firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
        .withValue(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
        .withValue(CalendarContract.Events.TITLE, eventTitle)
        .withValue(CalendarContract.Events.SYNC_DATA1, episodeId)
        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
        .withValue(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
        .withValue(
          CalendarContract.Events.CUSTOM_APP_URI,
          EpisodeDetailsActivity.createUri(episodeId).toString()
        )
        .build()
      ops.add(op)
    }
  }

  private fun updateCalendarColor(calendarId: Long) {
    val calendarColor = calendarColor

    val values = ContentValues()
    values.put(CalendarContract.Calendars.CALENDAR_COLOR, calendarColor)

    context.contentResolver.update(
      CalendarContract.Calendars.CONTENT_URI,
      values,
      BaseColumns._ID + "=?",
      arrayOf(calendarId.toString())
    )

    Settings.get(context).edit().putBoolean(Settings.CALENDAR_COLOR_NEEDS_UPDATE, false).apply()
  }

  private fun getCalendar(account: Account, recall: Boolean = true): Long {
    var c: Cursor? = null
    try {
      val calenderUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
        .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
        .build()
      c = context.contentResolver.query(calenderUri, arrayOf(BaseColumns._ID), null, null, null)

      if (c == null) {
        return INVALID_ID
      }

      if (c.moveToNext()) {
        return c.getLong(0)
      } else {
        if (!recall) {
          return INVALID_ID
        }

        val ops = ArrayList<ContentProviderOperation>()

        val builder = ContentProviderOperation.newInsert(
          CalendarContract.Calendars.CONTENT_URI.buildUpon().appendQueryParameter(
            CalendarContract.CALLER_IS_SYNCADAPTER,
            "true"
          ).appendQueryParameter(
            CalendarContract.Calendars.ACCOUNT_NAME,
            account.name
          ).appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type).build()
        )
        builder.withValue(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
        builder.withValue(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
        builder.withValue(CalendarContract.Calendars.NAME, context.getString(R.string.app_name))
        builder.withValue(
          CalendarContract.Calendars.CALENDAR_DISPLAY_NAME,
          context.getString(R.string.calendarName)
        )
        builder.withValue(CalendarContract.Calendars.CALENDAR_COLOR, calendarColor)
        builder.withValue(
          CalendarContract.Calendars.CALENDAR_ACCESS_LEVEL,
          CalendarContract.Calendars.CAL_ACCESS_READ
        )
        builder.withValue(CalendarContract.Calendars.OWNER_ACCOUNT, account.name)
        builder.withValue(CalendarContract.Calendars.SYNC_EVENTS, 1)
        ops.add(builder.build())
        try {
          context.contentResolver.applyBatch(CalendarContract.AUTHORITY, ops)
        } catch (e: Exception) {
          Timber.e(e, "Unable to create calendar")
          return INVALID_ID
        }

        return getCalendar(account, false)
      }
    } finally {
      c?.close()
    }
  }

  private fun deleteCalendar(account: Account, calendarId: Long) {
    Timber.d("Deleting calendar")

    context.contentResolver.delete(
      CalendarContract.Events.CONTENT_URI,
      CalendarContract.Events.CALENDAR_ID + "=?",
      arrayOf(calendarId.toString())
    )

    val calenderUri = CalendarContract.Calendars.CONTENT_URI.buildUpon()
      .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_NAME, account.name)
      .appendQueryParameter(CalendarContract.Calendars.ACCOUNT_TYPE, account.type)
      .build()
    context.contentResolver.delete(calenderUri, null, null)
  }

  companion object {

    private const val INVALID_ID = -1L
  }
}
