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

package net.simonvt.cathode.sync.trakt;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.text.format.DateUtils;
import androidx.work.WorkManager;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.api.body.CheckinItem;
import net.simonvt.cathode.api.entity.CheckinResponse;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.common.database.Cursors;
import net.simonvt.cathode.common.event.ErrorEvent;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.util.DataHelper;
import net.simonvt.cathode.sync.BuildConfig;
import net.simonvt.cathode.sync.R;
import net.simonvt.cathode.work.WorkManagerUtils;
import net.simonvt.cathode.work.user.SyncWatchingWorker;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class CheckIn {

  private Context context;
  private WorkManager workManager;
  private EpisodeDatabaseHelper episodeHelper;
  private CheckinService checkinService;

  @Inject
  public CheckIn(Context context, WorkManager workManager, EpisodeDatabaseHelper episodeHelper,
      CheckinService checkinService) {
    this.context = context;
    this.workManager = workManager;
    this.episodeHelper = episodeHelper;
    this.checkinService = checkinService;
  }

  public boolean episode(long episodeId, String message, boolean facebook, boolean twitter,
      boolean tumblr) {
    Cursor episode = episodeHelper.query(episodeId, EpisodeColumns.TRAKT_ID, EpisodeColumns.TITLE,
        EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
        EpisodeColumns.WATCHED);
    episode.moveToFirst();
    long traktId = Cursors.getLong(episode, EpisodeColumns.TRAKT_ID);
    long showId = Cursors.getLong(episode, EpisodeColumns.SHOW_ID);
    int season = Cursors.getInt(episode, EpisodeColumns.SEASON);
    int number = Cursors.getInt(episode, EpisodeColumns.EPISODE);
    boolean watched = Cursors.getBoolean(episode, EpisodeColumns.WATCHED);
    String title = DataHelper.getEpisodeTitle(context, episode, season, number, watched);
    episode.close();

    Cursor show = context.getContentResolver().query(Shows.withId(showId), new String[] {
        ShowColumns.RUNTIME,
    }, null, null, null);
    show.moveToFirst();
    int runtime = Cursors.getInt(show, ShowColumns.RUNTIME);
    show.close();

    try {
      CheckinItem item = new CheckinItem() //
          .episode(traktId)
          .message(message)
          .facebook(facebook)
          .twitter(twitter)
          .tumblr(tumblr)
          .appVersion(BuildConfig.VERSION_NAME)
          .appDate(BuildConfig.BUILD_TIME);
      Call<CheckinResponse> call = checkinService.checkin(item);
      Response<CheckinResponse> response = call.execute();

      if (response.isSuccessful()) {
        WorkManagerUtils.enqueueDelayed(workManager, SyncWatchingWorker.class,
            runtime * DateUtils.MINUTE_IN_MILLIS);
        return true;
      } else {
        if (response.code() == 409) {
          ErrorEvent.post(context.getString(R.string.checkin_error_watching, title));
        } else {
          ErrorEvent.post(context.getString(R.string.checkin_error, title));
        }
      }
    } catch (IOException e) {
      Timber.d(e, "Unable to check in %d", traktId);
      ErrorEvent.post(context.getString(R.string.checkin_error, title));
    }

    return false;
  }

  public boolean movie(long movieId, String message, boolean facebook, boolean twitter,
      boolean tumblr) {
    Cursor movie = context.getContentResolver().query(Movies.withId(movieId), new String[] {
        MovieColumns.ID, MovieColumns.TRAKT_ID, MovieColumns.TITLE, MovieColumns.RUNTIME,
    }, null, null, null);
    movie.moveToFirst();
    final long traktId = Cursors.getLong(movie, MovieColumns.TRAKT_ID);
    final String title = Cursors.getString(movie, MovieColumns.TITLE);
    final int runtime = Cursors.getInt(movie, MovieColumns.RUNTIME);
    movie.close();

    final long startedAt = System.currentTimeMillis();
    final long expiresAt = startedAt + runtime * DateUtils.MINUTE_IN_MILLIS;

    ContentValues values = new ContentValues();
    values.put(MovieColumns.CHECKED_IN, true);
    values.put(MovieColumns.STARTED_AT, startedAt);
    values.put(MovieColumns.EXPIRES_AT, expiresAt);
    context.getContentResolver().update(Movies.withId(movieId), values, null, null);

    try {
      CheckinItem item = new CheckinItem().movie(traktId)
          .message(message)
          .facebook(facebook)
          .twitter(twitter)
          .tumblr(tumblr)
          .appVersion(BuildConfig.VERSION_NAME)
          .appDate(BuildConfig.BUILD_TIME);

      Call<CheckinResponse> call = checkinService.checkin(item);
      Response<CheckinResponse> response = call.execute();

      if (response.isSuccessful()) {
        WorkManagerUtils.enqueueDelayed(workManager, SyncWatchingWorker.class,
            runtime * DateUtils.MINUTE_IN_MILLIS);
        return true;
      } else {
        if (response.code() == 409) {
          ErrorEvent.post(context.getString(R.string.checkin_error_watching, title));
        } else {
          ErrorEvent.post(context.getString(R.string.checkin_error, title));
        }
      }
    } catch (IOException e) {
      Timber.d(e, "Unable to check in %d", traktId);
      ErrorEvent.post(context.getString(R.string.checkin_error, title));
    }

    return false;
  }
}
