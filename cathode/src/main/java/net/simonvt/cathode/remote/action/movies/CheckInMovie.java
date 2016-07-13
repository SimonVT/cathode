/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.remote.action.movies;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.SystemClock;
import android.text.format.DateUtils;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.body.CheckinItem;
import net.simonvt.cathode.api.entity.CheckinResponse;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.event.CheckInFailedEvent;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.service.SyncWatchingReceiver;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import retrofit2.Response;

public class CheckInMovie extends CallJob<CheckinResponse> {

  @Inject transient CheckinService checkinService;

  @Inject transient MovieDatabaseHelper movieHelper;

  long traktId;

  String message;

  boolean facebook;

  boolean twitter;

  boolean tumblr;

  public CheckInMovie(long traktId, String message, boolean facebook, boolean twitter,
      boolean tumblr) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.message = message;
    this.facebook = facebook;
    this.twitter = twitter;
    this.tumblr = tumblr;
  }

  @Override public String key() {
    return "CheckInMovie" + "&traktId=" + traktId + "&message=" + message;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<CheckinResponse> getCall() {
    CheckinItem item = new CheckinItem() //
        .movie(traktId)
        .message(message)
        .facebook(facebook)
        .twitter(twitter)
        .tumblr(tumblr)
        .appVersion(BuildConfig.VERSION_NAME)
        .appDate(BuildConfig.BUILD_TIME);
    return checkinService.checkin(item);
  }

  @Override protected boolean handleError(Response<CheckinResponse> response) {
    if (response.code() == 409) {
      queue(new SyncWatching());

      final long movieId = movieHelper.getId(traktId);
      Cursor c = getContentResolver().query(Movies.withId(movieId), new String[] {
          MovieColumns.TITLE,
      }, null, null, null);
      if (c.moveToFirst()) {
        final String title = Cursors.getString(c, MovieColumns.TITLE);
        CheckInFailedEvent.post(title);
      }
      c.close();

      return true;
    }

    return super.handleError(response);
  }

  @Override public void handleResponse(CheckinResponse response) {
    final Movie movie = response.getMovie();
    final long movieId = movieHelper.getId(movie.getIds().getTrakt());
    Cursor c = getContentResolver().query(Movies.withId(movieId), new String[] {
        MovieColumns.RUNTIME,
    }, null, null, null);

    if (c.moveToFirst()) {
      final int runtime = Cursors.getInt(c, MovieColumns.RUNTIME);

      Intent i = new Intent(getContext(), SyncWatchingReceiver.class);
      PendingIntent pi = PendingIntent.getBroadcast(getContext(), 0, i, 0);

      AlarmManager am = (AlarmManager) getContext().getSystemService(Context.ALARM_SERVICE);
      am.set(AlarmManager.ELAPSED_REALTIME,
          SystemClock.elapsedRealtime() + (runtime + 1) * DateUtils.MINUTE_IN_MILLIS, pi);
    }

    c.close();
  }
}
