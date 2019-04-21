package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.api.enumeration.ShowStatus
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getFloat
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.entity.Show
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables

object ShowMapper : MappedCursorLiveData.CursorMapper<Show> {

  override fun map(cursor: Cursor): Show? {
    return if (cursor.moveToFirst()) mapShow(cursor) else null
  }

  fun mapShow(cursor: Cursor): Show {
    val id = cursor.getLong(ShowColumns.ID)
    val title = cursor.getStringOrNull(ShowColumns.TITLE)
    val titleNoArticle = cursor.getStringOrNull(ShowColumns.TITLE_NO_ARTICLE)
    val year = cursor.getInt(ShowColumns.YEAR)
    val firstAired = cursor.getLong(ShowColumns.FIRST_AIRED)
    val country = cursor.getStringOrNull(ShowColumns.COUNTRY)
    val overview = cursor.getStringOrNull(ShowColumns.OVERVIEW)
    val runtime = cursor.getInt(ShowColumns.RUNTIME)
    val network = cursor.getStringOrNull(ShowColumns.NETWORK)
    val airDay = cursor.getStringOrNull(ShowColumns.AIR_DAY)
    val airTime = cursor.getStringOrNull(ShowColumns.AIR_TIME)
    val airTimezone = cursor.getStringOrNull(ShowColumns.AIR_TIMEZONE)
    val certification = cursor.getStringOrNull(ShowColumns.CERTIFICATION)
    val slug = cursor.getStringOrNull(ShowColumns.SLUG)
    val traktId = cursor.getLong(ShowColumns.TRAKT_ID)
    val imdbId = cursor.getStringOrNull(ShowColumns.IMDB_ID)
    val tvdbId = cursor.getInt(ShowColumns.TVDB_ID)
    val tmdbId = cursor.getInt(ShowColumns.TMDB_ID)
    val tvrageId = cursor.getLong(ShowColumns.TVRAGE_ID)
    val lastUpdated = cursor.getLong(ShowColumns.LAST_UPDATED)
    val trailer = cursor.getStringOrNull(ShowColumns.TRAILER)
    val homepage = cursor.getStringOrNull(ShowColumns.HOMEPAGE)
    val statusString = cursor.getStringOrNull(ShowColumns.STATUS)
    val status: ShowStatus? = if (!statusString.isNullOrEmpty()) {
      ShowStatus.fromValue(statusString)
    } else {
      null
    }
    val userRating = cursor.getInt(ShowColumns.USER_RATING)
    val ratedAt = cursor.getLong(ShowColumns.RATED_AT)
    val rating = cursor.getFloat(ShowColumns.RATING)
    val votes = cursor.getInt(ShowColumns.VOTES)
    val watchers = cursor.getInt(ShowColumns.WATCHERS)
    val plays = cursor.getInt(ShowColumns.PLAYS)
    val scrobbles = cursor.getInt(ShowColumns.SCROBBLES)
    val checkins = cursor.getInt(ShowColumns.CHECKINS)
    val inWatchlist = cursor.getBoolean(ShowColumns.IN_WATCHLIST)
    val watchlistedAt = cursor.getLong(ShowColumns.LISTED_AT)
    val lastWatchedAt = cursor.getLong(ShowColumns.LAST_WATCHED_AT)
    val lastCollectedAt = cursor.getLong(ShowColumns.LAST_COLLECTED_AT)
    val hiddenCalendar = cursor.getBoolean(ShowColumns.HIDDEN_CALENDAR)
    val hiddenWatched = cursor.getBoolean(ShowColumns.HIDDEN_WATCHED)
    val hiddenCollected = cursor.getBoolean(ShowColumns.HIDDEN_COLLECTED)
    val hiddenRecommendations = cursor.getBoolean(ShowColumns.HIDDEN_RECOMMENDATIONS)
    val watchedCount = cursor.getInt(ShowColumns.WATCHED_COUNT)
    val airdateCount = cursor.getInt(ShowColumns.AIRDATE_COUNT)
    val inCollectionCount = cursor.getInt(ShowColumns.IN_COLLECTION_COUNT)
    val inWatchlistCount = cursor.getInt(ShowColumns.IN_WATCHLIST_COUNT)
    val needsSync = cursor.getBoolean(ShowColumns.NEEDS_SYNC)
    val lastSync = cursor.getLong(ShowColumns.LAST_SYNC)
    val lastCommentSync = cursor.getLong(ShowColumns.LAST_COMMENT_SYNC)
    val lastCreditsSync = cursor.getLong(ShowColumns.LAST_CREDITS_SYNC)
    val lastRelatedSync = cursor.getLong(ShowColumns.LAST_RELATED_SYNC)
    val watching = cursor.getBoolean(ShowColumns.WATCHING)
    val airedCount = cursor.getInt(ShowColumns.AIRED_COUNT)
    val unairedCount = cursor.getInt(ShowColumns.UNAIRED_COUNT)
    val episodeCount = cursor.getInt(ShowColumns.EPISODE_COUNT)
    val watchingEpisodeId = cursor.getLong(ShowColumns.WATCHING_EPISODE_ID)

    return Show(
      id,
      title,
      titleNoArticle,
      year,
      firstAired,
      country,
      overview,
      runtime,
      network,
      airDay,
      airTime,
      airTimezone,
      certification,
      slug,
      traktId,
      imdbId,
      tvdbId,
      tmdbId,
      tvrageId,
      lastUpdated,
      trailer,
      homepage,
      status,
      userRating,
      ratedAt,
      rating,
      votes,
      watchers,
      plays,
      scrobbles,
      checkins,
      inWatchlist,
      watchlistedAt,
      lastWatchedAt,
      lastCollectedAt,
      hiddenCalendar,
      hiddenWatched,
      hiddenCollected,
      hiddenRecommendations,
      watchedCount,
      airdateCount,
      inCollectionCount,
      inWatchlistCount,
      needsSync,
      lastSync,
      lastCommentSync,
      lastCreditsSync,
      lastRelatedSync,
      watching,
      airedCount,
      unairedCount,
      episodeCount,
      watchingEpisodeId
    )
  }

  val projection = arrayOf(
    Tables.SHOWS + "." + ShowColumns.ID,
    ShowColumns.TITLE,
    ShowColumns.TITLE_NO_ARTICLE,
    ShowColumns.YEAR,
    ShowColumns.FIRST_AIRED,
    ShowColumns.COUNTRY,
    Tables.SHOWS + "." + ShowColumns.OVERVIEW,
    ShowColumns.RUNTIME,
    ShowColumns.NETWORK,
    ShowColumns.AIR_DAY,
    ShowColumns.AIR_TIME,
    ShowColumns.AIR_TIMEZONE,
    ShowColumns.CERTIFICATION,
    ShowColumns.SLUG,
    Tables.SHOWS + "." + ShowColumns.TRAKT_ID,
    Tables.SHOWS + "." + ShowColumns.IMDB_ID,
    Tables.SHOWS + "." + ShowColumns.TVDB_ID,
    Tables.SHOWS + "." + ShowColumns.TMDB_ID,
    Tables.SHOWS + "." + ShowColumns.TVRAGE_ID,
    Tables.SHOWS + "." + ShowColumns.LAST_UPDATED,
    ShowColumns.TRAILER,
    ShowColumns.HOMEPAGE,
    ShowColumns.STATUS,
    Tables.SHOWS + "." + ShowColumns.USER_RATING,
    Tables.SHOWS + "." + ShowColumns.RATED_AT,
    Tables.SHOWS + "." + ShowColumns.RATING,
    Tables.SHOWS + "." + ShowColumns.VOTES,
    ShowColumns.WATCHERS,
    Tables.SHOWS + "." + ShowColumns.PLAYS,
    ShowColumns.SCROBBLES,
    ShowColumns.CHECKINS,
    ShowColumns.IN_WATCHLIST,
    ShowColumns.LISTED_AT,
    Tables.SHOWS + "." + ShowColumns.LAST_WATCHED_AT,
    ShowColumns.LAST_COLLECTED_AT,
    ShowColumns.HIDDEN_CALENDAR,
    ShowColumns.HIDDEN_WATCHED,
    ShowColumns.HIDDEN_COLLECTED,
    ShowColumns.HIDDEN_RECOMMENDATIONS,
    ShowColumns.WATCHED_COUNT,
    ShowColumns.AIRDATE_COUNT,
    ShowColumns.IN_COLLECTION_COUNT,
    ShowColumns.IN_WATCHLIST_COUNT,
    ShowColumns.NEEDS_SYNC,
    ShowColumns.LAST_SYNC,
    Tables.SHOWS + "." + ShowColumns.LAST_COMMENT_SYNC,
    ShowColumns.LAST_CREDITS_SYNC,
    ShowColumns.LAST_RELATED_SYNC,
    ShowColumns.WATCHING,
    ShowColumns.AIRED_COUNT,
    ShowColumns.UNAIRED_COUNT,
    ShowColumns.EPISODE_COUNT,
    ShowColumns.WATCHING_EPISODE_ID
  )
}
