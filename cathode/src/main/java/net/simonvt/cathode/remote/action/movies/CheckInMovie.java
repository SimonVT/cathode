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

import android.database.Cursor;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.body.CheckinItem;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.event.CheckInFailedEvent;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.util.MainHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;

public class CheckInMovie extends Job {

  @Inject transient CheckinService checkinService;

  @Inject transient Bus bus;

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

  @Override public void perform() {
    try {
      CheckinItem item = new CheckinItem() //
          .movie(traktId) //
          .message(message) //
          .facebook(facebook) //
          .twitter(twitter) //
          .tumblr(tumblr) //
          .appVersion(BuildConfig.VERSION_NAME) //
          .appDate(BuildConfig.BUILD_TIME);
      checkinService.checkin(item);
    } catch (RetrofitError e) {
      Response response = e.getResponse();
      if (response != null) {
        if (response.getStatus() == 409) {
          queue(new SyncWatching());

          final long movieId = MovieWrapper.getMovieId(getContentResolver(), traktId);
          Cursor c = getContentResolver().query(Movies.withId(movieId), new String[] {
              MovieColumns.TITLE,
          }, null, null, null);
          if (c.moveToFirst()) {
            final String title = c.getString(c.getColumnIndex(MovieColumns.TITLE));

            MainHandler.post(new Runnable() {
              @Override public void run() {
                bus.post(new CheckInFailedEvent(title));
              }
            });
          }
          c.close();
          return;
        }
      }

      throw e;
    }
  }
}
