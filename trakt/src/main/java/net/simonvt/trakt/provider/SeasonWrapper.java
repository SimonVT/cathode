package net.simonvt.trakt.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import net.simonvt.trakt.api.entity.Images;
import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.util.ApiUtils;

public final class SeasonWrapper {

  private SeasonWrapper() {
  }

  public static long getSeasonId(ContentResolver resolver, int showTvdbId, int season) {
    return getSeasonId(resolver, ShowWrapper.getShowId(resolver, showTvdbId), season);
  }

  public static long getSeasonId(ContentResolver resolver, long showId, int season) {
    Cursor c = null;
    try {
      c = resolver.query(TraktContract.Seasons.CONTENT_URI, new String[] {
          BaseColumns._ID,
      }, TraktContract.Seasons.SHOW_ID + "=? AND " + TraktContract.Seasons.SEASON + "=?",
          new String[] {
              String.valueOf(showId), String.valueOf(season),
          }, null);

      return !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));
    } finally {
      if (c != null) c.close();
    }
  }

  public static long getSeasonId(ContentResolver resolver, Season season) {
    Cursor c = null;
    try {
      c = resolver.query(TraktContract.Seasons.CONTENT_URI, new String[] {
          BaseColumns._ID,
      }, TraktContract.Seasons.URL + "=?", new String[] {
          season.getUrl(),
      }, null);

      return !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));
    } finally {
      if (c != null) c.close();
    }
  }

  public static long getShowId(ContentResolver resolver, long seasonId) {
    Cursor c = null;
    try {
      c = resolver.query(TraktContract.Seasons.buildFromId(seasonId), new String[] {
          TraktContract.Seasons.SHOW_ID,
      }, null, null, null);

      if (c.moveToFirst()) {
        return c.getLong(0);
      }

      return -1L;
    } finally {
      if (c != null) c.close();
    }
  }

  public static long updateOrInsertSeason(ContentResolver resolver, Season season, long showId) {
    long seasonId = getSeasonId(resolver, season);

    if (seasonId == -1) {
      seasonId = insertSeason(resolver, showId, season);
    } else {
      updateSeason(resolver, seasonId, season);
    }

    return seasonId;
  }

  public static void updateSeason(ContentResolver resolver, long seasonId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    resolver.update(TraktContract.Seasons.buildFromId(seasonId), cv, null, null);
  }

  public static long insertSeason(ContentResolver resolver, long showId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    cv.put(TraktContract.SeasonColumns.SHOW_ID, showId);

    Uri uri = resolver.insert(TraktContract.Seasons.CONTENT_URI, cv);
    return Long.valueOf(TraktContract.Seasons.getSeasonId(uri));
  }

  private static ContentValues getSeasonCVs(Season season) {
    ContentValues cv = new ContentValues();

    cv.put(TraktContract.SeasonColumns.SEASON, season.getSeason());
    cv.put(TraktContract.SeasonColumns.EPISODES, season.getEpisodes().getCount());
    cv.put(TraktContract.SeasonColumns.URL, season.getUrl());
    if (season.getImages() != null) {
      Images images = season.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) {
        cv.put(TraktContract.Shows.POSTER, images.getPoster());
      }
      if (!ApiUtils.isPlaceholder(images.getFanart())) {
        cv.put(TraktContract.Shows.FANART, images.getFanart());
      }
      if (!ApiUtils.isPlaceholder(images.getScreen())) {
        cv.put(TraktContract.Shows.SCREEN, images.getScreen());
      }
    }

    return cv;
  }
}
