package net.simonvt.trakt.provider;

import net.simonvt.trakt.api.entity.Images;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.provider.TraktContract.SeasonColumns;
import net.simonvt.trakt.provider.TraktContract.ShowColumns;
import net.simonvt.trakt.provider.TraktContract.Shows;
import net.simonvt.trakt.util.ApiUtils;
import net.simonvt.trakt.util.DateUtils;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;

import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;

public final class ShowWrapper {

    private static final String TAG = "ShowWrapper";

    private static final String[] SEASON_PROJECTION = new String[] {
            SeasonColumns.AIRDATE_COUNT,
            SeasonColumns.UNAIRED_COUNT,
            SeasonColumns.WATCHED_COUNT,
            SeasonColumns.AIRED_COUNT,
            SeasonColumns.IN_COLLECTION_COUNT,
    };

    private ShowWrapper() {
    }

    public static int getTvdbId(ContentResolver resolver, long showId) {
        Cursor c = resolver.query(Shows.buildShowUri(showId), new String[] {
                ShowColumns.TVDB_ID,
        }, null, null, null);

        int tvdbId = -1;
        if (c.moveToFirst()) {
            tvdbId = c.getInt(c.getColumnIndex(ShowColumns.TVDB_ID));
        }

        c.close();

        return tvdbId;
    }

    public static void insertShowGenres(ContentResolver resolver, long showId, List<String> genres) {
        for (String genre : genres) {
            ContentValues cv = new ContentValues();

            cv.put(TraktContract.ShowGenres.SHOW_ID, showId);
            cv.put(TraktContract.ShowGenres.GENRE, genre);

            resolver.insert(TraktContract.ShowGenres.buildFromShowId(showId), cv);
        }
    }

    public static long getShowId(ContentResolver resolver, TvShow show) {
        Cursor c = resolver.query(Shows.CONTENT_URI, new String[] {
                BaseColumns._ID,
        }, Shows.URL + "=?", new String[] {
                show.getUrl(),
        }, null);

        long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));

        c.close();

        return id;
    }

    public static long getShowId(ContentResolver resolver, int tvdbId) {
        Cursor c = resolver.query(Shows.CONTENT_URI, new String[] {
                BaseColumns._ID,
        }, Shows.TVDB_ID + "=?", new String[] {
                String.valueOf(tvdbId),
        }, null);

        long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));

        c.close();

        return id;
    }

    public static String getShowName(ContentResolver resolver, long showId) {
        Cursor c = resolver.query(Shows.CONTENT_URI, new String[] {
                ShowColumns.TITLE,
        }, BaseColumns._ID + "=?", new String[] {
                String.valueOf(showId),
        }, null);

        String title = null;
        if (c.moveToFirst()) {
            title = c.getString(c.getColumnIndex(ShowColumns.TITLE));
        }

        c.close();

        return title;
    }

    public static long getSeasonId(ContentResolver resolver, long showId, int seasonNumber) {
        Cursor c = resolver.query(TraktContract.Seasons.CONTENT_URI, new String[] {
                BaseColumns._ID,
        }, TraktContract.Seasons.SHOW_ID + "=? AND " + TraktContract.Seasons.SEASON + "=?", new String[] {
                String.valueOf(showId),
                String.valueOf(seasonNumber),
        }, null);

        long id = !c.moveToFirst() ? -1L : c.getLong(c.getColumnIndex(BaseColumns._ID));

        c.close();

        return id;
    }

    public static boolean exists(ContentResolver resolver, int tvdbId) {
        Cursor c = null;
        try {
            c = resolver.query(Shows.CONTENT_URI, new String[] {
                    Shows.LAST_UPDATED,
            }, Shows.TVDB_ID + "=?", new String[] {
                    String.valueOf(tvdbId),
            }, null);

            return c.moveToFirst();
        } finally {
            if (c != null) c.close();
        }
    }

    public static boolean needsUpdate(ContentResolver resolver, int tvdbId, long lastUpdated) {
        if (lastUpdated == 0) return true;

        Cursor c = null;
        try {
            c = resolver.query(Shows.CONTENT_URI, new String[] {
                    Shows.LAST_UPDATED,
            }, Shows.TVDB_ID + "=?", new String[] {
                    String.valueOf(tvdbId),
            }, null);

            boolean exists = c.moveToFirst();
            if (exists) {
                return lastUpdated > c.getLong(c.getColumnIndex(Shows.LAST_UPDATED));
            }

            return true;
        } finally {
            if (c != null) c.close();
        }
    }

    public static long updateOrInsertShow(ContentResolver resolver, TvShow show) {
        long showId = getShowId(resolver, show);

        if (showId == -1) {
            showId = insertShow(resolver, show);
        } else {
            updateShow(resolver, show);
        }

        return showId;
    }

    public static void updateShow(ContentResolver resolver, TvShow show) {
        final long id = getShowId(resolver, show);
        ContentValues cv = getShowCVs(show);
        resolver.update(Shows.buildShowUri(id), cv, null, null);
    }

    public static long insertShow(ContentResolver resolver, TvShow show) {
        ContentValues cv = getShowCVs(show);

        Uri uri = resolver.insert(Shows.CONTENT_URI, cv);
        final long showId = Long.valueOf(Shows.getShowId(uri));

        insertShowGenres(resolver, showId, show.getGenres());

        return showId;
    }

    public static void setWatched(ContentResolver resolver, int tvdbId, boolean watched) {
        setWatched(resolver, getShowId(resolver, tvdbId), watched);
    }

    public static void setWatched(ContentResolver resolver, long showId, boolean watched) {
        ContentValues cv = new ContentValues();
        cv.put(TraktContract.Episodes.WATCHED, watched);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        final long millis = cal.getTimeInMillis();

        resolver.update(TraktContract.Episodes.buildFromShowId(showId), cv,
                TraktContract.EpisodeColumns.FIRST_AIRED + "<?", new String[] {
                String.valueOf(millis),
        });
    }

    public static void setIsInWatchlist(ContentResolver resolver, int tvdbId, boolean inWatchlist) {
        setIsInWatchlist(resolver, getShowId(resolver, tvdbId), inWatchlist);
    }

    public static void setIsInWatchlist(ContentResolver resolver, long showId, boolean inWatchlist) {
        ContentValues cv = new ContentValues();
        cv.put(Shows.IN_WATCHLIST, inWatchlist);

        resolver.update(Shows.buildShowUri(showId), cv, null, null);
    }

    public static void setIsInCollection(ContentResolver resolver, int tvdbId, boolean inCollection) {
        setIsInCollection(resolver, getShowId(resolver, tvdbId), inCollection);
    }

    public static void setIsInCollection(ContentResolver resolver, long showId, boolean inCollection) {
        ContentValues cv = new ContentValues();
        cv.put(TraktContract.Episodes.IN_COLLECTION, inCollection);

        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("GMT-8:00"));
        final long millis = cal.getTimeInMillis();

        resolver.update(TraktContract.Episodes.buildFromShowId(showId), cv,
                TraktContract.EpisodeColumns.FIRST_AIRED + "<?", new String[] {
                String.valueOf(millis),
        });
    }

    public static void updateShowCounts(ContentResolver resolver, int tvdbId) {
        updateShowCounts(resolver, getShowId(resolver, tvdbId));
    }

    public static void updateShowCounts(ContentResolver resolver, long showId) {
        Cursor c = resolver.query(TraktContract.Seasons.CONTENT_URI, SEASON_PROJECTION,
                SeasonColumns.SHOW_ID + "=? AND " + SeasonColumns.SEASON + ">0", new String[] {
                String.valueOf(showId),
        }, null);

        int airdateCount = 0;
        int unairedCount = 0;
        int watchedCount = 0;
        int airedCount = 0;
        int inCollectionCount = 0;

        final int airdateIndex = c.getColumnIndex(ShowColumns.AIRDATE_COUNT);
        final int airedIndex = c.getColumnIndex(ShowColumns.AIRED_COUNT);
        final int unairedIndex = c.getColumnIndex(ShowColumns.UNAIRED_COUNT);
        final int watchedIndex = c.getColumnIndex(ShowColumns.WATCHED_COUNT);
        final int inCollectionIndex = c.getColumnIndex(ShowColumns.IN_COLLECTION_COUNT);

        while (c.moveToNext()) {
            airdateCount += c.getInt(airdateIndex);
            airedCount += c.getInt(airedIndex);
            unairedCount += c.getInt(unairedIndex);
            watchedCount += c.getInt(watchedIndex);
            inCollectionCount += c.getInt(inCollectionIndex);
        }

        ContentValues cv = new ContentValues();
        cv.put(SeasonColumns.AIRDATE_COUNT, airdateCount);
        cv.put(SeasonColumns.AIRED_COUNT, airedCount);
        cv.put(SeasonColumns.UNAIRED_COUNT, unairedCount);
        cv.put(SeasonColumns.WATCHED_COUNT, watchedCount);
        cv.put(SeasonColumns.IN_COLLECTION_COUNT, inCollectionCount);

        resolver.update(Shows.buildShowUri(showId), cv, null, null);

        c.close();
    }

    private static ContentValues getShowCVs(TvShow show) {
        ContentValues cv = new ContentValues();

        cv.put(Shows.TITLE, show.getTitle());
        cv.put(Shows.YEAR, show.getYear());
        cv.put(Shows.URL, show.getUrl());
        if (show.getFirstAiredIso() != null) {
            cv.put(Shows.FIRST_AIRED, DateUtils.getMillis(show.getFirstAiredIso()));
        }
        cv.put(Shows.COUNTRY, show.getCountry());
        cv.put(Shows.OVERVIEW, show.getOverview());
        cv.put(Shows.RUNTIME, show.getRuntime());
        if (show.getNetwork() != null) cv.put(Shows.NETWORK, show.getNetwork());
        if (show.getAirDay() != null) cv.put(Shows.AIR_DAY, show.getAirDay().toString());
        if (show.getAirTime() != null) cv.put(Shows.AIR_TIME, show.getAirTime());
        if (show.getCertification() != null) cv.put(Shows.CERTIFICATION, show.getCertification());
        cv.put(Shows.IMDB_ID, show.getImdbId());
        cv.put(Shows.TVDB_ID, show.getTvdbId());
        cv.put(Shows.TVRAGE_ID, show.getTvrageId());
        if (show.getLastUpdated() != null) cv.put(Shows.LAST_UPDATED, show.getLastUpdated());
        if (show.getImages() != null) {
            Images images = show.getImages();
            if (!ApiUtils.isPlaceholder(images.getPoster())) cv.put(Shows.POSTER, images.getPoster());
            if (!ApiUtils.isPlaceholder(images.getPoster())) cv.put(Shows.FANART, images.getFanart());
            if (!ApiUtils.isPlaceholder(images.getPoster())) cv.put(Shows.SCREEN, images.getScreen());
            if (!ApiUtils.isPlaceholder(images.getPoster())) cv.put(Shows.BANNER, images.getBanner());
        }
        if (show.getRatings() != null) {
            cv.put(Shows.RATING_PERCENTAGE, show.getRatings().getPercentage());
            cv.put(Shows.RATING_VOTES, show.getRatings().getVotes());
            cv.put(Shows.RATING_LOVED, show.getRatings().getLoved());
            cv.put(Shows.RATING_HATED, show.getRatings().getHated());
        }
        if (show.getStats() != null) {
            cv.put(Shows.WATCHERS, show.getStats().getWatchers());
            cv.put(Shows.PLAYS, show.getStats().getPlays());
            cv.put(Shows.SCROBBLES, show.getStats().getScrobbles());
            cv.put(Shows.CHECKINS, show.getStats().getCheckins());
        }
        if (show.getStatus() != null) cv.put(Shows.STATUS, show.getStatus().toString());
        if (show.getRatingAdvanced() != null) {
            cv.put(Shows.RATING, show.getRatingAdvanced());
        }

        return cv;
    }

    public static long getLastUpdated(ContentResolver resolver) {
        Cursor c = resolver.query(Shows.CONTENT_URI, new String[] {
                ShowColumns.LAST_UPDATED,
        }, null, null, ShowColumns.LAST_UPDATED + " DESC LIMIT 1");

        long lastUpdated = -1L;
        if (c.moveToFirst()) {
            lastUpdated = c.getLong(c.getColumnIndex(ShowColumns.LAST_UPDATED));
        }

        c.close();

        return lastUpdated;
    }
}
