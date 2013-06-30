package net.simonvt.trakt.provider;

import net.simonvt.trakt.api.entity.LastActivity;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.database.Cursor;

public class ActivityWrapper {

    private static final String TAG = "ActivityWrapper";

    public static boolean episodeWatchedNeedsUpdate(ContentResolver resolver, long lastUpdated) {
        Cursor c = resolver.query(TraktContract.UserActivity.CONTENT_URI, new String[] {
                TraktContract.UserActivity.EPISODE_WATCHED,
        }, null, null, null);

        if (c.moveToFirst()) {
            long updated = c.getLong(0);
            return updated == 0 || lastUpdated > updated;
        }

        return true;
    }

    public static boolean episodeCollectedNeedsUpdate(ContentResolver resolver, long lastUpdated) {
        Cursor c = resolver.query(TraktContract.UserActivity.CONTENT_URI, new String[] {
                TraktContract.UserActivity.EPISODE_COLLECTION,
        }, null, null, null);

        if (c.moveToFirst()) {
            long updated = c.getLong(0);
            return updated == 0 || lastUpdated > updated;
        }

        return true;
    }

    public static boolean movieWatchedNeedsUpdate(ContentResolver resolver, long lastUpdated) {
        Cursor c = resolver.query(TraktContract.UserActivity.CONTENT_URI, new String[] {
                TraktContract.UserActivity.MOVIE_WATCHED,
        }, null, null, null);

        if (c.moveToFirst()) {
            long updated = c.getLong(0);
            return updated == 0 || lastUpdated > updated;
        }

        return true;
    }

    public static boolean movieCollectedNeedsUpdate(ContentResolver resolver, long lastUpdated) {
        Cursor c = resolver.query(TraktContract.UserActivity.CONTENT_URI, new String[] {
                TraktContract.UserActivity.MOVIE_COLLECTION,
        }, null, null, null);

        if (c.moveToFirst()) {
            long updated = c.getLong(0);
            return updated == 0 || lastUpdated > updated;
        }

        return true;
    }

    public static void update(ContentResolver resolver, LastActivity lastActivity) {
        ContentValues cv = new ContentValues();

        cv.put(TraktContract.UserActivity.ALL, lastActivity.getAll());
        cv.put(TraktContract.UserActivity.EPISODE_CHECKIN, lastActivity.getEpisode().getCheckin());
        cv.put(TraktContract.UserActivity.EPISODE_COLLECTION, lastActivity.getEpisode().getCollection());
        cv.put(TraktContract.UserActivity.EPISODE_SCROBBLE, lastActivity.getEpisode().getScrobble());
        cv.put(TraktContract.UserActivity.EPISODE_SEEN, lastActivity.getEpisode().getSeen());
        cv.put(TraktContract.UserActivity.EPISODE_WATCHED, lastActivity.getEpisode().getWatched());
        cv.put(TraktContract.UserActivity.MOVIE_CHECKIN, lastActivity.getMovie().getCheckin());
        cv.put(TraktContract.UserActivity.MOVIE_COLLECTION, lastActivity.getMovie().getCollection());
        cv.put(TraktContract.UserActivity.MOVIE_SCROBBLE, lastActivity.getMovie().getScrobble());
        cv.put(TraktContract.UserActivity.MOVIE_SEEN, lastActivity.getMovie().getSeen());
        cv.put(TraktContract.UserActivity.MOVIE_WATCHED, lastActivity.getMovie().getWatched());

        final int updatedRows =
                resolver.update(TraktContract.UserActivity.CONTENT_URI, cv, TraktContract.UserActivity._ID + "=0",
                        null);
        if (updatedRows == 0) {
            resolver.insert(TraktContract.UserActivity.CONTENT_URI, cv);
        }
    }
}
