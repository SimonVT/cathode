package net.simonvt.trakt.provider;

import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.util.DateUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Calendar;
import java.util.TimeZone;

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
            }, TraktContract.Seasons.SHOW_ID + "=? AND " + TraktContract.Seasons.SEASON + "=?", new String[] {
                    String.valueOf(showId),
                    String.valueOf(season),
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
        Cursor c = resolver.query(TraktContract.Seasons.buildFromId(seasonId), new String[] {
                TraktContract.Seasons.SHOW_ID,
        }, null, null, null);

        if (c.moveToFirst()) {
            return c.getLong(0);
        }

        return -1L;
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
            cv.put(TraktContract.Shows.POSTER, season.getImages().getPoster());
            cv.put(TraktContract.Shows.FANART, season.getImages().getFanart());
            cv.put(TraktContract.Shows.HEADSHOT, season.getImages().getHeadshot());
            cv.put(TraktContract.Shows.SCREEN, season.getImages().getScreen());
        }

        return cv;
    }

    private static final String[] EPISODES_PROJECTION = new String[] {
            TraktContract.EpisodeColumns.FIRST_AIRED,
            TraktContract.EpisodeColumns.WATCHED,
            TraktContract.EpisodeColumns.IN_COLLECTION,
            TraktContract.EpisodeColumns.IN_WATCHLIST,
    };

    public static void updateSeasonCounts(ContentResolver resolver, int showTvdbId, int season) {
        updateSeasonCounts(resolver, getSeasonId(resolver, showTvdbId, season));
    }

    public static void updateSeasonCounts(ContentResolver resolver, long seasonId) {
        Cursor c = resolver.query(TraktContract.Episodes.buildFromSeasonId(seasonId), EPISODES_PROJECTION,
                null, null, null);

        int airdateCount = 0;
        int airedCount = 0;
        int watchedCount = 0;
        int inCollectionCount = 0;

        final int firstAiredIndex = c.getColumnIndex(TraktContract.EpisodeColumns.FIRST_AIRED);
        final int watchedIndex = c.getColumnIndex(TraktContract.EpisodeColumns.WATCHED);
        final int inCollectionIndex = c.getColumnIndex(TraktContract.EpisodeColumns.IN_COLLECTION);

        // TODO: Possibly get server timezone from trakt api
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        final long currentTimeSeconds = cal.getTimeInMillis() / 1000L;

        while (c.moveToNext()) {
            final long firstAired = c.getLong(firstAiredIndex);
            if (c.getInt(watchedIndex) > 0) watchedCount++;
            // Unaired
            if (firstAired > 1 * DateUtils.YEAR_IN_SECONDS) {
                airdateCount++;
                if (firstAired <= currentTimeSeconds) {
                    airedCount++;
                }
            }
            if (c.getInt(inCollectionIndex) > 0) inCollectionCount++;
        }

        ContentValues cv = new ContentValues();
        cv.put(TraktContract.SeasonColumns.AIRDATE_COUNT, airdateCount);
        cv.put(TraktContract.SeasonColumns.AIRED_COUNT, airedCount);
        cv.put(TraktContract.SeasonColumns.UNAIRED_COUNT, airdateCount - airedCount);
        cv.put(TraktContract.SeasonColumns.WATCHED_COUNT, watchedCount);
        cv.put(TraktContract.SeasonColumns.IN_COLLECTION_COUNT, inCollectionCount);

        resolver.update(TraktContract.Seasons.buildFromId(seasonId), cv, null, null);
        resolver.notifyChange(TraktContract.Seasons.CONTENT_URI, null);

        c.close();
    }
}
