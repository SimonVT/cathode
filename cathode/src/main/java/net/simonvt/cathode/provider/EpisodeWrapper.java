package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.provider.CathodeContract.EpisodeColumns;
import net.simonvt.cathode.provider.CathodeContract.Episodes;
import net.simonvt.cathode.util.ApiUtils;
import net.simonvt.cathode.util.DateUtils;

public final class EpisodeWrapper {

  private EpisodeWrapper() {
  }

  public static Cursor query(ContentResolver resolver, long id, String... columns) {
    return resolver.query(Episodes.buildFromId(id), columns, null, null, null);
  }

  public static long getEpisodeId(ContentResolver resolver, Episode episode) {
    Cursor c = resolver.query(Episodes.CONTENT_URI, new String[] {
        BaseColumns._ID,
    }, Episodes.URL + "=?", new String[] {
        episode.getUrl(),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));

    c.close();

    return id;
  }

  public static long getEpisodeId(ContentResolver resolver, long showId, int seasonNumber,
      int episodeNumber) {
    Cursor c = resolver.query(Episodes.CONTENT_URI, new String[] {
        BaseColumns._ID,
    }, Episodes.SHOW_ID + "=? AND " + Episodes.SEASON + "=? AND " + Episodes.EPISODE + "=?",
        new String[] {
            String.valueOf(showId), String.valueOf(seasonNumber), String.valueOf(episodeNumber),
        }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));

    c.close();

    return id;
  }

  public static int getShowTvdbId(ContentResolver resolver, long episodeId) {
    Cursor c = resolver.query(Episodes.CONTENT_URI, new String[] {
        Episodes.SHOW_ID,
    }, BaseColumns._ID + "=?", new String[] {
        String.valueOf(episodeId),
    }, null);

    if (c.moveToFirst()) {
      long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
      return ShowWrapper.getTvdbId(resolver, showId);
    }

    c.close();

    return -1;
  }

  public static long updateOrInsertEpisode(ContentResolver resolver, Episode episode, long showId,
      long seasonId) {
    long episodeId = getEpisodeId(resolver, episode);

    if (episodeId == -1L) {
      episodeId = insertEpisodes(resolver, showId, seasonId, episode);
    } else {
      updateEpisode(resolver, episodeId, episode);
    }

    return episodeId;
  }

  public static void updateEpisode(ContentResolver resolver, long episodeId, Episode episode) {
    ContentValues cv = getEpisodeCVs(episode);
    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);

    Cursor c = resolver.query(Episodes.buildFromId(episodeId), new String[] {
        Episodes.SHOW_ID, Episodes.SEASON_ID,
    }, null, null, null);

    if (c.moveToFirst()) {
      final long showId = c.getLong(c.getColumnIndex(Episodes.SHOW_ID));
      final long seasonId = c.getLong(c.getColumnIndex(Episodes.SEASON_ID));
    }

    c.close();
  }

  public static long insertEpisodes(ContentResolver resolver, long showId, long seasonId,
      Episode episode) {
    ContentValues cv = getEpisodeCVs(episode);

    cv.put(EpisodeColumns.SHOW_ID, showId);
    cv.put(EpisodeColumns.SEASON_ID, seasonId);

    Uri uri = resolver.insert(Episodes.CONTENT_URI, cv);

    return Long.valueOf(Episodes.getEpisodeId(uri));
  }

  public static void episodeUpdateWatched(ContentResolver resolver, long episodeId,
      boolean watched) {
    ContentValues cv = new ContentValues();

    cv.put(EpisodeColumns.WATCHED, watched);

    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);
  }

  public static long getShowId(ContentResolver resolver, long episodeId) {
    Cursor c = resolver.query(Episodes.buildFromId(episodeId), new String[] {
        EpisodeColumns.SHOW_ID,
    }, null, null, null);

    long id = -1L;
    if (c.moveToFirst()) {
      id = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
    }

    c.close();

    return id;
  }

  public static long getSeasonId(ContentResolver resolver, long episodeId) {
    Cursor c = resolver.query(Episodes.buildFromId(episodeId), new String[] {
        EpisodeColumns.SEASON_ID,
    }, null, null, null);

    long id = -1L;
    if (c.moveToFirst()) {
      id = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON_ID));
    }

    c.close();

    return id;
  }

  public static int getSeason(ContentResolver resolver, long episodeId) {
    Cursor c = resolver.query(Episodes.buildFromId(episodeId), new String[] {
        EpisodeColumns.SEASON,
    }, null, null, null);

    int season = -1;
    if (c.moveToFirst()) {
      season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
    }

    c.close();

    return season;
  }

  public static void setWatched(ContentResolver resolver, int tvdbId, int season, int episode,
      boolean watched) {
    final long showId = ShowWrapper.getShowId(resolver, tvdbId);
    final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, season, episode);
    setWatched(resolver, episodeId, watched);
  }

  public static void setWatched(ContentResolver resolver, long episodeId, boolean watched) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);

    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);

    Cursor c = resolver.query(Episodes.buildFromId(episodeId), new String[] {
        Episodes.SHOW_ID, Episodes.SEASON_ID,
    }, null, null, null);

    if (c.moveToFirst()) {
      final long showId = c.getLong(c.getColumnIndex(Episodes.SHOW_ID));
      final long seasonId = c.getLong(c.getColumnIndex(Episodes.SEASON_ID));
    }

    c.close();
  }

  public static void unseen(ContentResolver resolver, long episodeId) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, false);

    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);
  }

  public static void setInCollection(ContentResolver resolver, int tvdbId, int season, int episode,
      boolean inCollection) {
    final long showId = ShowWrapper.getShowId(resolver, tvdbId);
    final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, season, episode);
    setInCollection(resolver, episodeId, inCollection);
  }

  public static void setInCollection(ContentResolver resolver, long episodeId,
      boolean inCollection) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, inCollection);

    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);

    Cursor c = resolver.query(Episodes.buildFromId(episodeId), new String[] {
        Episodes.SHOW_ID, Episodes.SEASON_ID,
    }, null, null, null);

    if (c.moveToFirst()) {
      final long showId = c.getLong(c.getColumnIndex(Episodes.SHOW_ID));
      final long seasonId = c.getLong(c.getColumnIndex(Episodes.SEASON_ID));
    }

    c.close();
  }

  public static void setIsInWatchlist(ContentResolver resolver, int tvdbId, int season, int episode,
      boolean inWatchlist) {
    final long showId = ShowWrapper.getShowId(resolver, tvdbId);
    final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, season, episode);
    setIsInWatchlist(resolver, episodeId, inWatchlist);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long episodeId,
      boolean inWatchlist) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_WATCHLIST, inWatchlist);

    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);
  }

  public static void setRating(ContentResolver resolver, Integer tvdbId, Integer season,
      Integer episode, Integer ratingAdvanced) {
    final long showId = ShowWrapper.getShowId(resolver, tvdbId);
    final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, season, episode);
    setRating(resolver, episodeId, ratingAdvanced);
  }

  public static void setRating(ContentResolver resolver, long episodeId, int ratingAdvanced) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.RATING, ratingAdvanced);

    resolver.update(Episodes.buildFromId(episodeId), cv, null, null);
  }

  public static ContentValues getEpisodeCVs(Episode episode) {
    ContentValues cv = new ContentValues();

    cv.put(EpisodeColumns.SEASON, episode.getSeason());
    cv.put(EpisodeColumns.EPISODE, episode.getNumber());
    cv.put(EpisodeColumns.TITLE, episode.getTitle());
    cv.put(EpisodeColumns.OVERVIEW, episode.getOverview());
    cv.put(EpisodeColumns.URL, episode.getUrl());
    cv.put(EpisodeColumns.TVDB_ID, episode.getTvdbId());
    cv.put(EpisodeColumns.IMDB_ID, episode.getImdbId());
    if (episode.getFirstAiredIso() != null) {
      cv.put(EpisodeColumns.FIRST_AIRED, DateUtils.getMillis(episode.getFirstAiredIso()));
    }
    if (episode.getImages() != null) {
      Images images = episode.getImages();
      if (!ApiUtils.isPlaceholder(images.getPoster())) cv.put(Episodes.POSTER, images.getPoster());
      if (!ApiUtils.isPlaceholder(images.getFanart())) cv.put(Episodes.FANART, images.getFanart());
      if (!ApiUtils.isPlaceholder(images.getScreen())) cv.put(Episodes.SCREEN, images.getScreen());
      if (!ApiUtils.isPlaceholder(images.getBanner())) cv.put(Episodes.BANNER, images.getBanner());
    }
    if (episode.getRatings() != null) {
      cv.put(EpisodeColumns.RATING_PERCENTAGE, episode.getRatings().getPercentage());
      cv.put(EpisodeColumns.RATING_VOTES, episode.getRatings().getVotes());
      cv.put(EpisodeColumns.RATING_LOVED, episode.getRatings().getLoved());
      cv.put(EpisodeColumns.RATING_HATED, episode.getRatings().getHated());
    }
    if (episode.isWatched() != null) cv.put(EpisodeColumns.WATCHED, episode.isWatched());
    if (episode.getPlays() != null) cv.put(EpisodeColumns.PLAYS, episode.getPlays());
    if (episode.getRating() != null) cv.put(EpisodeColumns.RATING, episode.getRating());
    if (episode.isInWatchlist() != null) {
      cv.put(EpisodeColumns.IN_WATCHLIST, episode.isInWatchlist());
    }
    if (episode.isInCollection() != null) {
      cv.put(EpisodeColumns.IN_COLLECTION, episode.isInCollection());
    }

    return cv;
  }
}
