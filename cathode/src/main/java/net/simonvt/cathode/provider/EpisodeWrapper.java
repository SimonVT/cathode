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
package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Images;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public final class EpisodeWrapper {

  private EpisodeWrapper() {
  }

  public static Cursor query(ContentResolver resolver, long id, String... columns) {
    return resolver.query(Episodes.withId(id), columns, null, null, null);
  }

  public static long getEpisodeId(ContentResolver resolver, long showId, int seasonNumber,
      int episodeNumber) {
    Cursor c = resolver.query(Episodes.EPISODES, new String[] {
        EpisodeColumns.ID,
    }, EpisodeColumns.SHOW_ID
        + "=? AND "
        + EpisodeColumns.SEASON
        + "=? AND "
        + EpisodeColumns.EPISODE
        + "=?", new String[] {
        String.valueOf(showId), String.valueOf(seasonNumber), String.valueOf(episodeNumber),
    }, null);

    long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(EpisodeColumns.ID));

    c.close();

    return id;
  }

  public static long getShowTraktId(ContentResolver resolver, long episodeId) {
    Cursor c = resolver.query(Episodes.EPISODES, new String[] {
        EpisodeColumns.SHOW_ID,
    }, EpisodeColumns.ID + "=?", new String[] {
        String.valueOf(episodeId),
    }, null);

    if (c.moveToFirst()) {
      long showId = c.getLong(c.getColumnIndex(EpisodeColumns.SHOW_ID));
      return ShowWrapper.getTraktId(resolver, showId);
    }

    c.close();

    return -1L;
  }

  public static long createEpisode(ContentResolver resolver, long showId, long seasonId,
      int number) {
    final int seasonNumber = SeasonWrapper.getSeasonNumber(resolver, seasonId);

    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.SHOW_ID, showId);
    cv.put(EpisodeColumns.SEASON_ID, seasonId);
    cv.put(EpisodeColumns.SEASON, seasonNumber);
    cv.put(EpisodeColumns.EPISODE, number);
    cv.put(EpisodeColumns.NEEDS_SYNC, 1);

    return Episodes.getId(resolver.insert(Episodes.EPISODES, cv));
  }

  public static long updateOrInsertEpisode(ContentResolver resolver, Episode episode, long showId,
      long seasonId) {
    if (showId < 0L) {
      throw new IllegalArgumentException("Invalid show id: " + showId);
    }
    if (seasonId < 0L) {
      throw new IllegalArgumentException("Invalid season id: " + seasonId);
    }

    long episodeId = getEpisodeId(resolver, showId, episode.getSeason(), episode.getNumber());

    if (episodeId == -1L) {
      episodeId = insertEpisodes(resolver, showId, seasonId, episode);
    } else {
      updateEpisode(resolver, episodeId, episode);
    }

    return episodeId;
  }

  public static void updateEpisode(ContentResolver resolver, long episodeId, Episode episode) {
    ContentValues cv = getEpisodeCVs(episode);
    resolver.update(Episodes.withId(episodeId), cv, null, null);
  }

  public static long insertEpisodes(ContentResolver resolver, long showId, long seasonId,
      Episode episode) {
    ContentValues cv = getEpisodeCVs(episode);

    cv.put(EpisodeColumns.SHOW_ID, showId);
    cv.put(EpisodeColumns.SEASON_ID, seasonId);

    Uri uri = resolver.insert(Episodes.EPISODES, cv);

    return Episodes.getId(uri);
  }

  public static void episodeUpdateWatched(ContentResolver resolver, long episodeId,
      boolean watched) {
    ContentValues cv = new ContentValues();

    cv.put(EpisodeColumns.WATCHED, watched);

    resolver.update(Episodes.withId(episodeId), cv, null, null);
  }

  public static long getShowId(ContentResolver resolver, long episodeId) {
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
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
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
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
    Cursor c = resolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.SEASON,
    }, null, null, null);

    int season = -1;
    if (c.moveToFirst()) {
      season = c.getInt(c.getColumnIndex(EpisodeColumns.SEASON));
    }

    c.close();

    return season;
  }

  public static void setWatched(ContentResolver resolver, long traktId, int season, int episode,
      boolean watched, long watchedAt) {
    final long showId = ShowWrapper.getShowId(resolver, traktId);
    final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, season, episode);
    setWatched(resolver, showId, episodeId, watched, watchedAt);
  }

  public static void setWatched(ContentResolver resolver, long showId, long episodeId,
      boolean watched, long watchedAt) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.WATCHED, watched);
    resolver.update(Episodes.withId(episodeId), cv, null, null);

    if (watched) {
      Cursor c = resolver.query(Shows.withId(showId), new String[] {
          ShowColumns.LAST_WATCHED_AT,
      }, null, null, null);
      c.moveToFirst();
      final long lastWatched = c.getLong(c.getColumnIndex(ShowColumns.LAST_WATCHED_AT));
      c.close();
      if (watchedAt > lastWatched) {
        cv = new ContentValues();
        cv.put(ShowColumns.LAST_WATCHED_AT, watchedAt);
        resolver.update(Shows.withId(showId), cv, null, null);
      }
    }
  }

  public static void setInCollection(ContentResolver resolver, long traktId, int season,
      int episode, boolean inCollection, long collectedAt) {
    final long showId = ShowWrapper.getShowId(resolver, traktId);
    final long episodeId = getEpisodeId(resolver, showId, season, episode);
    setInCollection(resolver, episodeId, inCollection, collectedAt);
  }

  public static void setInCollection(ContentResolver resolver, long episodeId, boolean inCollection,
      long collectedAt) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_COLLECTION, inCollection);
    cv.put(EpisodeColumns.COLLECTED_AT, collectedAt);

    resolver.update(Episodes.withId(episodeId), cv, null, null);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long traktId, int season,
      int episode, boolean inWatchlist, long listedAt) {
    final long showId = ShowWrapper.getShowId(resolver, traktId);
    final long episodeId = EpisodeWrapper.getEpisodeId(resolver, showId, season, episode);
    setIsInWatchlist(resolver, episodeId, inWatchlist, listedAt);
  }

  public static void setIsInWatchlist(ContentResolver resolver, long episodeId, boolean inWatchlist,
      long listedAt) {
    ContentValues cv = new ContentValues();
    cv.put(EpisodeColumns.IN_WATCHLIST, inWatchlist);
    cv.put(EpisodeColumns.LISTED_AT, listedAt);

    resolver.update(Episodes.withId(episodeId), cv, null, null);
  }

  public static ContentValues getEpisodeCVs(Episode episode) {
    ContentValues cv = new ContentValues();

    cv.put(EpisodeColumns.NEEDS_SYNC, 0);

    cv.put(EpisodeColumns.SEASON, episode.getSeason());
    cv.put(EpisodeColumns.EPISODE, episode.getNumber());
    cv.put(EpisodeColumns.NUMBER_ABS, episode.getNumberAbs());

    if (episode.getTitle() != null) cv.put(EpisodeColumns.TITLE, episode.getTitle());
    if (episode.getOverview() != null) cv.put(EpisodeColumns.OVERVIEW, episode.getOverview());

    if (episode.getFirstAired() != null) {
      cv.put(EpisodeColumns.FIRST_AIRED, episode.getFirstAired().getTimeInMillis());
    }
    if (episode.getUpdatedAt() != null) {
      cv.put(EpisodeColumns.UPDATED_AT, episode.getUpdatedAt().getTimeInMillis());
    }

    cv.put(EpisodeColumns.TRAKT_ID, episode.getIds().getTrakt());
    cv.put(EpisodeColumns.IMDB_ID, episode.getIds().getImdb());
    cv.put(EpisodeColumns.TVDB_ID, episode.getIds().getTvdb());
    cv.put(EpisodeColumns.TMDB_ID, episode.getIds().getTmdb());
    cv.put(EpisodeColumns.TVRAGE_ID, episode.getIds().getTvrage());

    if (episode.getImages() != null) {
      // TODO: What images do I actually have?
      Images images = episode.getImages();
      if (images.getFanart() != null) {
        cv.put(EpisodeColumns.FANART, images.getFanart().getFull());
      }
    }

    if (episode.getRating() != null) {
      cv.put(EpisodeColumns.RATING, episode.getRating());
    }
    if (episode.getVotes() != null) {
      cv.put(EpisodeColumns.VOTES, episode.getVotes());
    }

    return cv;
  }
}
