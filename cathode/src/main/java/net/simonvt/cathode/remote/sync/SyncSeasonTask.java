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
package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.OperationApplicationException;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.api.ResponseParser;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.ShowService;
import net.simonvt.cathode.provider.CathodeContract;
import net.simonvt.cathode.provider.CathodeProvider;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.TraktTask;
import retrofit.RetrofitError;
import timber.log.Timber;

public class SyncSeasonTask extends TraktTask {

  private static final String TAG = "SyncSeasonTask";

  @Inject transient ShowService showService;

  private int tvdbId;

  private int season;

  public SyncSeasonTask(int tvdbId, int season) {
    this.tvdbId = tvdbId;
    this.season = season;
  }

  private void addOp(ArrayList<ContentProviderOperation> ops, ContentProviderOperation op)
      throws RemoteException, OperationApplicationException {
    ops.add(op);
    if (ops.size() >= 50) {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
      ops.clear();
    }
  }

  @Override protected void doTask() {
    try {
      Timber.d("Syncing season %d of show %d", season, tvdbId);

      List<Episode> episodes = showService.season(tvdbId, season);

      ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();

      final ContentResolver resolver = getContentResolver();

      for (Episode episode : episodes) {
        if (!(episode.getSeason() == 0 && episode.getNumber() == 0)) {
          Episode summary;
          try {
            summary = showService.episodeSummary(tvdbId, season, episode.getNumber()).getEpisode();
          } catch (RetrofitError e) {
            final int statusCode = e.getResponse().getStatus();
            if (statusCode == 400) {
              Timber.tag(TAG).e(e, "URL: %s", e.getUrl());

              ResponseParser parser = new ResponseParser();
              CathodeApp.inject(getContext(), parser);
              Response response = parser.tryParse(e);
              if (response != null && "episode not found".equals(response.getError())) {
                postOnSuccess();
                return;
              }
            }
            continue;
          }

          final long showId = ShowWrapper.getShowId(resolver, tvdbId);
          final long seasonId = ShowWrapper.getSeasonId(resolver, showId, season);
          final long id =
              EpisodeWrapper.getEpisodeId(resolver, showId, season, episode.getNumber());
          final boolean exists = id != -1L;

          ContentProviderOperation.Builder builder;
          if (exists) {
            builder = ContentProviderOperation.newUpdate(CathodeContract.Episodes.buildFromId(id));
          } else {
            builder = ContentProviderOperation.newInsert(CathodeContract.Episodes.CONTENT_URI);
          }

          ContentValues cv = EpisodeWrapper.getEpisodeCVs(summary);
          cv.put(CathodeContract.Episodes.SHOW_ID, showId);
          cv.put(CathodeContract.Episodes.SEASON_ID, seasonId);
          builder.withValues(cv);

          addOp(ops, builder.build());
        }
      }

      resolver.applyBatch(CathodeProvider.AUTHORITY, ops);

      postOnSuccess();
    } catch (RemoteException e) {
      Timber.e(e, null);
      postOnFailure();
    } catch (OperationApplicationException e) {
      Timber.e(e, null);
      postOnFailure();
    }
  }
}
