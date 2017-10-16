/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.sync.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.SyncItems;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.common.event.ErrorEvent;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.remote.action.RemoveHistoryItem;
import net.simonvt.cathode.remote.action.shows.AddEpisodeToHistory;
import net.simonvt.cathode.remote.action.shows.CollectEpisode;
import net.simonvt.cathode.remote.action.shows.RateEpisode;
import net.simonvt.cathode.remote.action.shows.RemoveEpisodeFromHistory;
import net.simonvt.cathode.remote.action.shows.WatchlistEpisode;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.remote.sync.comments.SyncComments;
import net.simonvt.cathode.remote.sync.shows.SyncSeason;
import net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus;
import net.simonvt.cathode.sync.R;
import net.simonvt.cathode.sync.tmdb.api.show.SyncEpisodeImages;
import net.simonvt.cathode.sync.trakt.CheckIn;
import net.simonvt.schematic.Cursors;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class EpisodeTaskScheduler extends BaseTaskScheduler {

  @Inject ShowDatabaseHelper showHelper;
  @Inject EpisodeDatabaseHelper episodeHelper;
  @Inject CheckinService checkinService;
  @Inject CheckIn checkIn;

  public EpisodeTaskScheduler(Context context) {
    super(context);
  }

  public void sync(final long episodeId, final Job.OnDoneListener onDoneListener) {
    execute(new Runnable() {
      @Override public void run() {
        final long showId = episodeHelper.getShowId(episodeId);
        final int showTmdbId = showHelper.getTmdbId(showId);
        final long traktId = showHelper.getTraktId(showId);
        final int season = episodeHelper.getSeason(episodeId);
        final int episode = episodeHelper.getNumber(episodeId);

        queue(new SyncSeason(traktId, season));
        queue(new SyncEpisodeImages(showTmdbId, season, episode));
        Job syncComments = new SyncComments(ItemType.EPISODE, traktId, season, episode);
        syncComments.registerOnDoneListener(onDoneListener);
        queue(syncComments);

        ContentValues values = new ContentValues();
        values.put(EpisodeColumns.LAST_COMMENT_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Episodes.withId(episodeId), values, null, null);
      }
    });
  }

  public void addToHistoryNow(final long episodeId) {
    addToHistory(episodeId, System.currentTimeMillis());
  }

  public void addToHistoryOnRelease(final long episodeId) {
    addToHistory(episodeId, SyncItems.TIME_RELEASED);
  }

  public void addToHistory(final long episodeId, final long watchedAt) {
    final String isoWhen = TimeUtils.getIsoTime(watchedAt);
    addToHistory(episodeId, isoWhen);
  }

  public void addToHistory(final long episodeId, final int year, final int month, final int day,
      final int hour, final int minute) {
    addToHistory(episodeId, TimeUtils.getMillis(year, month, day, hour, minute));
  }

  public void addToHistory(final long episodeId, final String watchedAt) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = episodeHelper.query(episodeId, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON,
            EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
        final long traktId = showHelper.getTraktId(showId);
        final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
        final int number = Cursors.getInt(c, EpisodeColumns.EPISODE);
        c.close();

        if (SyncItems.TIME_RELEASED.equals(watchedAt)) {
          episodeHelper.addToHistory(episodeId, EpisodeDatabaseHelper.WATCHED_RELEASE);
        } else {
          episodeHelper.addToHistory(episodeId, TimeUtils.getMillis(watchedAt));
        }

        queue(new AddEpisodeToHistory(traktId, season, number, watchedAt));
      }
    });
  }

  public void removeFromHistory(final long episodeId) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor c = episodeHelper.query(episodeId, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON,
            EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
        final long traktId = showHelper.getTraktId(showId);
        final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
        final int number = Cursors.getInt(c, EpisodeColumns.EPISODE);
        c.close();

        episodeHelper.removeFromHistory(episodeId);

        queue(new RemoveEpisodeFromHistory(traktId, season, number));
      }
    });
  }

  public void removeHistoryItem(final long episodeId, final long historyId,
      final boolean lastItem) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new RemoveHistoryItem(historyId));

        if (lastItem) {
          episodeHelper.removeFromHistory(episodeId);
        }

        final long showId = episodeHelper.getShowId(episodeId);
        final long traktId = showHelper.getTraktId(showId);
        queue(new SyncShowWatchedStatus(traktId));
      }
    });
  }

  public void checkin(final long episodeId, final String message, final boolean facebook,
      final boolean twitter, final boolean tumblr) {
    execute(new Runnable() {
      @Override public void run() {
        Cursor watching =
            context.getContentResolver().query(Episodes.EPISODE_WATCHING, new String[] {
                EpisodeColumns.ID, EpisodeColumns.EXPIRES_AT,
            }, null, null, null);

        final long currentTime = System.currentTimeMillis();
        long expires = 0;
        if (watching.moveToFirst()) {
          expires = Cursors.getLong(watching, EpisodeColumns.EXPIRES_AT);
        }

        Cursor episode =
            episodeHelper.query(episodeId, EpisodeColumns.SHOW_ID, EpisodeColumns.TITLE,
                EpisodeColumns.SEASON, EpisodeColumns.EPISODE, EpisodeColumns.WATCHED);
        episode.moveToFirst();
        final long showId = Cursors.getLong(episode, EpisodeColumns.SHOW_ID);
        final int season = Cursors.getInt(episode, EpisodeColumns.SEASON);
        final int number = Cursors.getInt(episode, EpisodeColumns.EPISODE);
        final boolean watched = Cursors.getBoolean(episode, EpisodeColumns.WATCHED);
        final String title = DataHelper.getEpisodeTitle(context, episode, season, number, watched);
        episode.close();

        Cursor show = context.getContentResolver().query(Shows.withId(showId), new String[] {
            ShowColumns.RUNTIME,
        }, null, null, null);
        show.moveToFirst();
        final int runtime = Cursors.getInt(show, ShowColumns.RUNTIME);
        final long watchSlop = (long) (runtime * DateUtils.MINUTE_IN_MILLIS * 0.8f);
        show.close();

        if (watching.getCount() == 0 || ((expires - watchSlop) < currentTime && expires > 0)) {
          if (checkIn.episode(episodeId, message, facebook, twitter, tumblr)) {
            episodeHelper.checkIn(episodeId);
          }
        } else {
          ErrorEvent.post(context.getString(R.string.checkin_error_watching, title));
        }

        watching.close();
        queue(new SyncWatching());
      }
    });
  }

  public void cancelCheckin() {
    execute(new Runnable() {
      @Override public void run() {
        Cursor episode = null;
        try {
          episode = context.getContentResolver().query(Episodes.EPISODE_WATCHING, new String[] {
              EpisodeColumns.ID, EpisodeColumns.STARTED_AT, EpisodeColumns.EXPIRES_AT,
          }, null, null, null);

          if (episode.moveToFirst()) {
            final long id = Cursors.getLong(episode, EpisodeColumns.ID);
            final long startedAt = Cursors.getLong(episode, EpisodeColumns.STARTED_AT);
            final long expiresAt = Cursors.getLong(episode, EpisodeColumns.EXPIRES_AT);

            ContentValues values = new ContentValues();
            values.put(EpisodeColumns.CHECKED_IN, false);
            context.getContentResolver().update(Episodes.EPISODE_WATCHING, values, null, null);

            try {
              Call<ResponseBody> call = checkinService.deleteCheckin();
              Response<ResponseBody> response = call.execute();
              if (response.isSuccessful()) {
                return;
              }
            } catch (IOException e) {
              Timber.d(e);
            }

            ErrorEvent.post(context.getString(R.string.checkin_cancel_error));

            values.clear();
            values.put(EpisodeColumns.CHECKED_IN, true);
            values.put(EpisodeColumns.STARTED_AT, startedAt);
            values.put(EpisodeColumns.EXPIRES_AT, expiresAt);
            context.getContentResolver().update(Episodes.withId(id), values, null, null);

            queue(new SyncWatching());
          }
        } finally {
          if (episode != null) {
            episode.close();
          }
        }
      }
    });
  }

  /**
   * Add episodes to user library collection.
   *
   * @param episodeId The database id of the episode.
   */
  public void setIsInCollection(final long episodeId, final boolean inCollection) {
    execute(new Runnable() {
      @Override public void run() {
        String collectedAt = null;
        long collectedAtMillis = 0L;
        if (inCollection) {
          collectedAt = TimeUtils.getIsoTime();
          collectedAtMillis = TimeUtils.getMillis(collectedAt);
        }

        Cursor c = episodeHelper.query(episodeId, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON,
            EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
        final long traktId = showHelper.getTraktId(showId);
        final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
        final int number = Cursors.getInt(c, EpisodeColumns.EPISODE);
        c.close();

        episodeHelper.setInCollection(episodeId, inCollection, collectedAtMillis);

        queue(new CollectEpisode(traktId, season, number, inCollection, collectedAt));
      }
    });
  }

  public void setIsInWatchlist(final long episodeId, final boolean inWatchlist) {
    execute(new Runnable() {
      @Override public void run() {
        String listedAt = null;
        long listeddAtMillis = 0L;
        if (inWatchlist) {
          listedAt = TimeUtils.getIsoTime();
          listeddAtMillis = TimeUtils.getMillis(listedAt);
        }

        Cursor c = episodeHelper.query(episodeId, EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON,
            EpisodeColumns.EPISODE);
        c.moveToFirst();
        final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
        final long traktId = showHelper.getTraktId(showId);
        final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
        final int number = Cursors.getInt(c, EpisodeColumns.EPISODE);
        c.close();

        episodeHelper.setIsInWatchlist(episodeId, inWatchlist, listeddAtMillis);

        queue(new WatchlistEpisode(traktId, season, number, inWatchlist, listedAt));
      }
    });
  }

  /**
   * Rate an episode on trakt. Depending on the user settings, this will also send out social
   * updates to facebook,
   * twitter, and tumblr.
   *
   * @param episodeId The database id of the episode.
   * @param rating A rating betweeo 1 and 10. Use 0 to undo rating.
   */
  public void rate(final long episodeId, final int rating) {
    execute(new Runnable() {
      @Override public void run() {
        String ratedAt = TimeUtils.getIsoTime();
        long ratedAtMillis = TimeUtils.getMillis(ratedAt);

        final long showId = episodeHelper.getShowId(episodeId);
        final long showTraktId = showHelper.getTraktId(showId);
        Cursor c = episodeHelper.query(episodeId, EpisodeColumns.EPISODE, EpisodeColumns.SEASON);

        if (c.moveToFirst()) {
          final int episode = Cursors.getInt(c, EpisodeColumns.EPISODE);
          final int season = Cursors.getInt(c, EpisodeColumns.SEASON);

          ContentValues values = new ContentValues();
          values.put(EpisodeColumns.USER_RATING, rating);
          values.put(EpisodeColumns.RATED_AT, ratedAtMillis);
          context.getContentResolver().update(Episodes.withId(episodeId), values, null, null);

          queue(new RateEpisode(showTraktId, season, episode, rating, ratedAt));
        }
        c.close();
      }
    });
  }

  public void syncComments(final long episodeId) {
    execute(new Runnable() {
      @Override public void run() {
        final long showId = episodeHelper.getShowId(episodeId);
        final long traktId = showHelper.getTraktId(showId);
        final int season = episodeHelper.getSeason(episodeId);
        final int episode = episodeHelper.getNumber(episodeId);
        queue(new SyncComments(ItemType.EPISODE, traktId, season, episode));

        ContentValues values = new ContentValues();
        values.put(EpisodeColumns.LAST_COMMENT_SYNC, System.currentTimeMillis());
        context.getContentResolver().update(Episodes.withId(episodeId), values, null, null);
      }
    });
  }
}
