package net.simonvt.cathode.entitymapper;

import android.database.Cursor;
import android.text.TextUtils;
import net.simonvt.cathode.api.enumeration.ShowStatus;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;

public class ShowMapper implements MappedCursorLiveData.CursorMapper<Show> {

  @Override public Show map(Cursor cursor) {
    if (cursor.moveToFirst()) {
      return mapShow(cursor);
    }

    return null;
  }

  public static Show mapShow(Cursor cursor) {
    long id = Cursors.getLong(cursor, ShowColumns.ID);
    String title = Cursors.getStringOrNull(cursor, ShowColumns.TITLE);
    String titleNoArticle = Cursors.getStringOrNull(cursor, ShowColumns.TITLE_NO_ARTICLE);
    Integer year = Cursors.getIntOrNull(cursor, ShowColumns.YEAR);
    Long firstAired = Cursors.getLongOrNull(cursor, ShowColumns.FIRST_AIRED);
    String country = Cursors.getStringOrNull(cursor, ShowColumns.COUNTRY);
    String overview = Cursors.getStringOrNull(cursor, ShowColumns.OVERVIEW);
    Integer runtime = Cursors.getIntOrNull(cursor, ShowColumns.RUNTIME);
    String network = Cursors.getStringOrNull(cursor, ShowColumns.NETWORK);
    String airDay = Cursors.getStringOrNull(cursor, ShowColumns.AIR_DAY);
    String airTime = Cursors.getStringOrNull(cursor, ShowColumns.AIR_TIME);
    String airTimezone = Cursors.getStringOrNull(cursor, ShowColumns.AIR_TIMEZONE);
    String certification = Cursors.getStringOrNull(cursor, ShowColumns.CERTIFICATION);
    String slug = Cursors.getStringOrNull(cursor, ShowColumns.SLUG);
    Long traktId = Cursors.getLongOrNull(cursor, ShowColumns.TRAKT_ID);
    String imdbId = Cursors.getStringOrNull(cursor, ShowColumns.IMDB_ID);
    Integer tvdbId = Cursors.getIntOrNull(cursor, ShowColumns.TVDB_ID);
    if (tvdbId == null) {
      tvdbId = 0;
    }
    Integer tmdbId = Cursors.getIntOrNull(cursor, ShowColumns.TMDB_ID);
    if (tmdbId == null) {
      tmdbId = 0;
    }
    Long tvrageId = Cursors.getLongOrNull(cursor, ShowColumns.TVRAGE_ID);
    if (tvrageId == null) {
      tvrageId = 0L;
    }
    Long lastUpdated = Cursors.getLongOrNull(cursor, ShowColumns.LAST_UPDATED);
    String trailer = Cursors.getStringOrNull(cursor, ShowColumns.TRAILER);
    String homepage = Cursors.getStringOrNull(cursor, ShowColumns.HOMEPAGE);
    String statusString = Cursors.getStringOrNull(cursor, ShowColumns.STATUS);
    ShowStatus status = null;
    if (!TextUtils.isEmpty(statusString)) {
      status = ShowStatus.fromValue(statusString);
    }
    Integer userRating = Cursors.getIntOrNull(cursor, ShowColumns.USER_RATING);
    Long ratedAt = Cursors.getLongOrNull(cursor, ShowColumns.RATED_AT);
    Float rating = Cursors.getFloatOrNull(cursor, ShowColumns.RATING);
    Integer votes = Cursors.getIntOrNull(cursor, ShowColumns.VOTES);
    Integer watchers = Cursors.getIntOrNull(cursor, ShowColumns.WATCHERS);
    Integer plays = Cursors.getIntOrNull(cursor, ShowColumns.PLAYS);
    Integer scrobbles = Cursors.getIntOrNull(cursor, ShowColumns.SCROBBLES);
    Integer checkins = Cursors.getIntOrNull(cursor, ShowColumns.CHECKINS);
    Boolean inWatchlist = Cursors.getBooleanOrNull(cursor, ShowColumns.IN_WATCHLIST);
    Long watchlistedAt = Cursors.getLongOrNull(cursor, ShowColumns.LISTED_AT);
    Long lastWatchedAt = Cursors.getLongOrNull(cursor, ShowColumns.LAST_WATCHED_AT);
    Long lastCollectedAt = Cursors.getLongOrNull(cursor, ShowColumns.LAST_COLLECTED_AT);
    Boolean hiddenCalendar = Cursors.getBooleanOrNull(cursor, ShowColumns.HIDDEN_CALENDAR);
    Boolean hiddenWatched = Cursors.getBooleanOrNull(cursor, ShowColumns.HIDDEN_WATCHED);
    Boolean hiddenCollected = Cursors.getBooleanOrNull(cursor, ShowColumns.HIDDEN_COLLECTED);
    Boolean hiddenRecommendations =
        Cursors.getBooleanOrNull(cursor, ShowColumns.HIDDEN_RECOMMENDATIONS);
    Integer watchedCount = Cursors.getIntOrNull(cursor, ShowColumns.WATCHED_COUNT);
    Integer airdateCount = Cursors.getIntOrNull(cursor, ShowColumns.AIRDATE_COUNT);
    Integer inCollectionCount = Cursors.getIntOrNull(cursor, ShowColumns.IN_COLLECTION_COUNT);
    Integer inWatchlistCount = Cursors.getIntOrNull(cursor, ShowColumns.IN_WATCHLIST_COUNT);
    Boolean needsSync = Cursors.getBooleanOrNull(cursor, ShowColumns.NEEDS_SYNC);
    Long lastSync = Cursors.getLongOrNull(cursor, ShowColumns.LAST_SYNC);
    Long lastCommentSync = Cursors.getLongOrNull(cursor, ShowColumns.LAST_COMMENT_SYNC);
    Long lastCreditsSync = Cursors.getLongOrNull(cursor, ShowColumns.LAST_CREDITS_SYNC);
    Long lastRelatedSync = Cursors.getLongOrNull(cursor, ShowColumns.LAST_RELATED_SYNC);
    Boolean watching = Cursors.getBooleanOrNull(cursor, ShowColumns.WATCHING);
    Integer airedCount = Cursors.getIntOrNull(cursor, ShowColumns.AIRED_COUNT);
    Integer unairedCount = Cursors.getIntOrNull(cursor, ShowColumns.UNAIRED_COUNT);
    Integer episodeCount = Cursors.getIntOrNull(cursor, ShowColumns.EPISODE_COUNT);
    Long watchingEpisodeId = Cursors.getLongOrNull(cursor, ShowColumns.WATCHING_EPISODE_ID);

    return new Show(id, title, titleNoArticle, year, firstAired, country, overview, runtime,
        network, airDay, airTime, airTimezone, certification, slug, traktId, imdbId, tvdbId, tmdbId,
        tvrageId, lastUpdated, trailer, homepage, status, userRating, ratedAt, rating, votes,
        watchers, plays, scrobbles, checkins, inWatchlist, watchlistedAt, lastWatchedAt,
        lastCollectedAt, hiddenCalendar, hiddenWatched, hiddenCollected, hiddenRecommendations,
        watchedCount, airdateCount, inCollectionCount, inWatchlistCount, needsSync, lastSync,
        lastCommentSync, lastCreditsSync, lastRelatedSync, watching, airedCount, unairedCount,
        episodeCount, watchingEpisodeId);
  }
}
