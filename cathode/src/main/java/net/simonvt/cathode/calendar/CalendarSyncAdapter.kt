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
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getString
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.common.util.toRanges
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.query
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.settings.Permissions
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.ui.EpisodeDetailsActivity
import net.simonvt.cathode.ui.SeasonDetailsActivity
import timber.log.Timber
import java.util.ArrayList

class CalendarSyncAdapter(context: Context) : AbstractThreadedSyncAdapter(context, true) {

  private val calendarColor: Int
    get() = Settings.get(context).getInt(Settings.CALENDAR_COLOR, Settings.CALENDAR_COLOR_DEFAULT)

  private class Event(cursor: Cursor) {

    val id = cursor.getLong(CalendarContract.Events._ID)
  }

  class CalendarShow(val id: Long, val title: String, val runtime: Int) {
    val entries = mutableMapOf<Long, MutableMap<Int, CalendarSeason>>()
  }

  class CalendarSeason(val id: Long) {
    val episodes = mutableListOf<CalendarEpisode>()
  }

  class CalendarEpisode(val id: Long, val number: Int, val title: String)

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

    // User has not not granted calendar permission.
    if (!Permissions.hasCalendarPermission(context)) {
      Timber.d("Calendar permission not granted")
      return
    }

    // Unable to query calendar provider for a calendar ID.
    val calendarId = getCalendar(account)
    if (calendarId == INVALID_ID) {
      return
    }

    // If user has disabled calendar integration in settings, attempt to delete the calendar.
    // Unfortunately this requires the user has granted the permission.
    val syncCalendar = Settings.get(context).getBoolean(Settings.CALENDAR_SYNC, false)
    if (!syncCalendar) {
      deleteCalendar(account, calendarId)
      return
    }

    // If calendar color has been changed, update it.
    val updateCalendarColor =
      Settings.get(context).getBoolean(Settings.CALENDAR_COLOR_NEEDS_UPDATE, false)
    if (updateCalendarColor) {
      updateCalendarColor(calendarId)
    }

    val ops = ArrayList<ContentProviderOperation>()

    val existingEventsCursor = context.contentResolver.query(
      CalendarContract.Events.CONTENT_URI,
      arrayOf(
        CalendarContract.Events._ID,
        CalendarContract.Events.DTSTART,
        CalendarContract.Events.DTEND,
        CalendarContract.Events.CUSTOM_APP_URI
      ),
      CalendarContract.Events.CALENDAR_ID + "=?",
      arrayOf(calendarId.toString()),
      null
    )

    if (existingEventsCursor == null) {
      return
    }

    val existingEvents = mutableMapOf<String, Event>()
    while (existingEventsCursor.moveToNext()) {
      val id = existingEventsCursor.getLong(CalendarContract.Events._ID)
      val uri = existingEventsCursor.getStringOrNull(CalendarContract.Events.CUSTOM_APP_URI)

      if (!uri.isNullOrEmpty()) {
        val event = existingEvents[uri]
        if (event == null) {
          existingEvents[uri] = Event(existingEventsCursor)
        } else {
          // Delete duplicate events.
          val op = ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
            .withSelection(CalendarContract.Events._ID + "=?", arrayOf(id.toString()))
            .build()
          ops.add(op)
        }
      } else {
        // If event has no uri, delete it.
        val op = ContentProviderOperation.newDelete(CalendarContract.Events.CONTENT_URI)
          .withSelection(CalendarContract.Events._ID + "=?", arrayOf(id.toString()))
          .build()
        ops.add(op)
      }
    }
    existingEventsCursor.close()

    val upcomingEpisodes = getUpcoming()
    for (show in upcomingEpisodes) {
      for ((firstAired, seasons) in show.entries) {
        for ((seasonNumber, season) in seasons) {
          if (season.episodes.size > 2) {
            // Merge episodes into one entry.
            val episodesString = season.episodes.map { it.number }.toRanges()
            addSeasonEvent(
              ops,
              existingEvents,
              calendarId,
              season.id,
              seasonNumber,
              show.title,
              episodesString,
              firstAired,
              show.runtime
            )
          } else {
            // Create event for single episodes
            for (episode in season.episodes) {
              addEpisodeEvent(
                ops,
                existingEvents,
                calendarId,
                episode.id,
                episode.title,
                show.title,
                firstAired,
                show.runtime
              )
            }
          }
        }
      }
    }

    // And now delete old events.
    for (event in existingEvents.values) {
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

  private fun getUpcoming(): List<CalendarShow> {
    val upcoming = mutableListOf<CalendarShow>()
    val currentTime = System.currentTimeMillis()

    val shows = context.contentResolver.query(
      Shows.SHOWS,
      arrayOf(ShowColumns.ID, ShowColumns.TITLE, ShowColumns.RUNTIME),
      "(" + ShowColumns.WATCHED_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST_COUNT + ">0 OR " +
          ShowColumns.IN_COLLECTION_COUNT + ">0 OR " +
          ShowColumns.IN_WATCHLIST + ") AND " +
          ShowColumns.HIDDEN_CALENDAR + "=0" + " AND " +
          ShowColumns.LAST_SYNC + ">0"
    )

    while (shows.moveToNext()) {
      val showId = shows.getLong(ShowColumns.ID)
      val showTitle = shows.getString(ShowColumns.TITLE)
      val runtime = shows.getInt(ShowColumns.RUNTIME)

      val episodes = context.contentResolver.query(
        Episodes.fromShow(showId),
        arrayOf(
          EpisodeColumns.ID,
          EpisodeColumns.SEASON_ID,
          EpisodeColumns.TITLE,
          EpisodeColumns.SEASON,
          EpisodeColumns.EPISODE,
          EpisodeColumns.WATCHED,
          EpisodeColumns.FIRST_AIRED
        ),
        EpisodeColumns.FIRST_AIRED + ">?",
        arrayOf((currentTime - 30 * DateUtils.DAY_IN_MILLIS).toString())
      )

      if (episodes.count > 0) {
        val calendarShow = CalendarShow(showId, showTitle, runtime)
        upcoming.add(calendarShow)

        while (episodes.moveToNext()) {
          val episodeId = episodes.getLong(EpisodeColumns.ID)
          val seasonId = episodes.getLong(EpisodeColumns.SEASON_ID)
          val season = episodes.getInt(EpisodeColumns.SEASON)
          val episode = episodes.getInt(EpisodeColumns.EPISODE)
          val watched = episodes.getBoolean(EpisodeColumns.WATCHED)
          val episodeTitle =
            DataHelper.getEpisodeTitle(context, episodes, season, episode, watched, true)
          val firstAired = DataHelper.getFirstAired(episodes)

          var seasonEntry: CalendarSeason? = null
          var seasonEntries = calendarShow.entries[firstAired]
          if (seasonEntries != null) {
            seasonEntry = seasonEntries[season]
          } else {
            seasonEntries = mutableMapOf()
            calendarShow.entries[firstAired] = seasonEntries
          }

          if (seasonEntry == null) {
            seasonEntry = CalendarSeason(seasonId)
            seasonEntries[season] = seasonEntry
          }

          seasonEntry.episodes.add(CalendarEpisode(episodeId, episode, episodeTitle))
        }
      }
      episodes.close()
    }
    shows.close()

    return upcoming
  }

  private fun addSeasonEvent(
    ops: MutableList<ContentProviderOperation>,
    events: MutableMap<String, Event>,
    calendarId: Long,
    seasonId: Long,
    seasonNumber: Int,
    showTitle: String,
    episodesString: String,
    firstAired: Long,
    runtime: Int
  ) {
    val eventTitle =
      context.getString(R.string.calendar_entry_season, showTitle, seasonNumber, episodesString)
    val seasonUri = SeasonDetailsActivity.createUri(seasonId).toString()

    val event = events[seasonUri]
    if (event != null) {
      val op = ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI)
        .withValue(CalendarContract.Events.DTSTART, firstAired)
        .withValue(CalendarContract.Events.DTEND, firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
        .withValue(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
        .withValue(CalendarContract.Events.TITLE, eventTitle)
        .withSelection(CalendarContract.Events._ID + "=?", arrayOf(event.id.toString()))
        .build()
      ops.add(op)
      events.remove(seasonUri)
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
        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
        .withValue(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
        .withValue(
          CalendarContract.Events.CUSTOM_APP_URI,
          seasonUri
        )
        .build()
      ops.add(op)
    }
  }

  private fun addEpisodeEvent(
    ops: MutableList<ContentProviderOperation>,
    events: MutableMap<String, Event>,
    calendarId: Long,
    episodeId: Long,
    episodeTitle: String,
    showTitle: String,
    firstAired: Long,
    runtime: Int
  ) {
    val eventTitle = "$showTitle - $episodeTitle"
    val episodeUri = EpisodeDetailsActivity.createUri(episodeId).toString()

    val event = events[episodeUri]
    if (event != null) {
      val op = ContentProviderOperation.newUpdate(CalendarContract.Events.CONTENT_URI)
        .withValue(CalendarContract.Events.DTSTART, firstAired)
        .withValue(CalendarContract.Events.DTEND, firstAired + runtime * DateUtils.MINUTE_IN_MILLIS)
        .withValue(CalendarContract.Events.EVENT_TIMEZONE, Time.TIMEZONE_UTC)
        .withValue(CalendarContract.Events.TITLE, eventTitle)
        .withSelection(CalendarContract.Events._ID + "=?", arrayOf(event.id.toString()))
        .build()
      ops.add(op)
      events.remove(episodeUri)
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
        .withValue(CalendarContract.Events.CALENDAR_ID, calendarId)
        .withValue(CalendarContract.Events.CUSTOM_APP_PACKAGE, context.packageName)
        .withValue(
          CalendarContract.Events.CUSTOM_APP_URI,
          episodeUri
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
