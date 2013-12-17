package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.util.ApiUtils;

public final class SeasonWrapper {

  private SeasonWrapper() {
  }

  public static long getSeasonId(ContentResolver resolver, int showTvdbId, int season) {
    return getSeasonId(resolver, ShowWrapper.getShowId(resolver, showTvdbId), season);
  }

  public static long getSeasonId(ContentResolver resolver, long showId, int season) {
    Cursor c = null;
    try {
      c = resolver.query(CathodeContract.Seasons.CONTENT_URI, new String[] {
          BaseColumns._ID,
      }, CathodeContract.Seasons.SHOW_ID + "=? AND " + CathodeContract.Seasons.SEASON + "=?",
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
      c = resolver.query(CathodeContract.Seasons.CONTENT_URI, new String[] {
          BaseColumns._ID,
      }, CathodeContract.Seasons.URL + "=?", new String[] {
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
      c = resolver.query(CathodeContract.Seasons.buildFromId(seasonId), new String[] {
          CathodeContract.Seasons.SHOW_ID,
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
    resolver.update(CathodeContract.Seasons.buildFromId(seasonId), cv, null, null);
  }

  public static long insertSeason(ContentResolver resolver, long showId, Season season) {
    ContentValues cv = getSeasonCVs(season);
    cv.put(CathodeContract.SeasonColumns.SHOW_ID, showId);

    Uri uri = resolver.insert(CathodeContract.Seasons.CONTENT_URI, cv);
    return Long.valueOf(CathodeContract.Seasons.getSeasonId(uri));
  }

  private static ContentValues getSeasonCVs(Season season) {
    ContentValues cv = new ContentValues();

    cv.put(CathodeContract.SeasonColumns.SEASON, season.getSeason());
    cv.put(CathodeContract.SeasonColumns.EPISODES, season.getEpisodes().getCount());
    cv.put(CathodeContract.SeasonColumns.URL, season.getUrl());
    if (season.getImages() != null) {
      Images images = season.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) {
        cv.put(CathodeContract.SeasonColumns.POSTER, images.getPoster());
      }
    }

    return cv;
  }
}
