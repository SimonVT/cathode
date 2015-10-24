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
package net.simonvt.cathode.remote.action.shows;

import android.database.Cursor;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.body.CheckinItem;
import net.simonvt.cathode.api.entity.CheckinResponse;
import net.simonvt.cathode.api.service.CheckinService;
import net.simonvt.cathode.event.CheckInFailedEvent;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncWatching;
import net.simonvt.cathode.util.MainHandler;
import retrofit.Call;
import retrofit.Response;

public class CheckInEpisode extends CallJob<CheckinResponse> {

  @Inject transient CheckinService checkinService;

  @Inject transient Bus bus;

  private long traktId;

  String message;

  boolean facebook;

  boolean twitter;

  boolean tumblr;

  public CheckInEpisode(long traktId, String message, boolean facebook, boolean twitter,
      boolean tumblr) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
    this.message = message;
    this.facebook = facebook;
    this.twitter = twitter;
    this.tumblr = tumblr;
  }

  @Override public String key() {
    return "CheckInEpisode" + "&traktId=" + traktId + "&message=" + message;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<CheckinResponse> getCall() {
    CheckinItem item = new CheckinItem() //
        .episode(traktId)
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

      Cursor c = getContentResolver().query(Episodes.EPISODES, new String[] {
          EpisodeColumns.TITLE,
      }, EpisodeColumns.TRAKT_ID + "=?", new String[] {
          String.valueOf(traktId),
      }, null);

      if (c.moveToFirst()) {
        final String title = c.getString(c.getColumnIndex(EpisodeColumns.TITLE));

        MainHandler.post(new Runnable() {
          @Override public void run() {
            bus.post(new CheckInFailedEvent(title));
          }
        });
      }

      c.close();
      return true;
    }

    return super.handleError(response);
  }

  @Override public void handleResponse(CheckinResponse response) {
  }
}
