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
package net.simonvt.cathode.ui.show;

import android.content.Context;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.api.entity.HistoryItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.database.BaseAsyncLoader;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class EpisodeHistoryLoader extends BaseAsyncLoader<EpisodeHistoryLoader.Result> {

  public static class Result {

    boolean isSuccessful;

    List<net.simonvt.cathode.ui.show.HistoryItem> items;

    public Result(boolean isSuccessful) {
      this.isSuccessful = isSuccessful;
    }

    public Result(boolean isSuccessful, List<net.simonvt.cathode.ui.show.HistoryItem> items) {
      this.isSuccessful = isSuccessful;
      this.items = items;
    }

    public boolean isSuccessful() {
      return isSuccessful;
    }

    public List<net.simonvt.cathode.ui.show.HistoryItem> getItems() {
      return items;
    }
  }

  @Inject SyncService syncService;

  @Inject EpisodeDatabaseHelper episodeHelper;

  private long episodeId;

  private DateFormat df = DateFormat.getDateTimeInstance();

  public EpisodeHistoryLoader(Context context, long episodeId) {
    super(context);
    this.episodeId = episodeId;
    Injector.obtain().inject(this);
  }

  @Override public Result loadInBackground() {
    try {
      final long traktId = episodeHelper.getTraktId(episodeId);
      Call<List<HistoryItem>> call = syncService.getEpisodeHistory(traktId);
      Response<List<HistoryItem>> response = call.execute();

      if (response.isSuccessful()) {
        List<HistoryItem> items = response.body();
        List<net.simonvt.cathode.ui.show.HistoryItem> historyItems = new ArrayList<>();
        for (HistoryItem item : items) {
          final long id = item.getId();
          final String date = df.format(new Date(item.getWatchedAt().getTimeInMillis()));
          net.simonvt.cathode.ui.show.HistoryItem historyItem =
              new net.simonvt.cathode.ui.show.HistoryItem(id, date);
          historyItems.add(historyItem);
        }

        return new Result(true, historyItems);
      }
    } catch (IOException e) {
      Timber.d(e, "Loading episode history failed");
    }

    return new Result(false);
  }
}
