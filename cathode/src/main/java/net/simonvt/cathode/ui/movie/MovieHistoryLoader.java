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
package net.simonvt.cathode.ui.movie;

import android.content.Context;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import net.simonvt.cathode.api.entity.HistoryItem;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.database.BaseAsyncLoader;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class MovieHistoryLoader extends BaseAsyncLoader<MovieHistoryLoader.Result> {

  public static class Result {

    boolean isSuccessful;

    List<net.simonvt.cathode.ui.movie.HistoryItem> items;

    public Result(boolean isSuccessful) {
      this.isSuccessful = isSuccessful;
    }

    public Result(boolean isSuccessful, List<net.simonvt.cathode.ui.movie.HistoryItem> items) {
      this.isSuccessful = isSuccessful;
      this.items = items;
    }

    public boolean isSuccessful() {
      return isSuccessful;
    }

    public List<net.simonvt.cathode.ui.movie.HistoryItem> getItems() {
      return items;
    }
  }

  private SyncService syncService;

  private MovieDatabaseHelper movieHelper;

  private long movieId;

  private DateFormat df = DateFormat.getDateTimeInstance();

  public MovieHistoryLoader(Context context, long movieId, SyncService syncService, MovieDatabaseHelper movieHelper) {
    super(context);
    this.movieId = movieId;
    this.syncService = syncService;
    this.movieHelper = movieHelper;
  }

  @Override public Result loadInBackground() {
    try {
      final long traktId = movieHelper.getTraktId(movieId);
      Call<List<HistoryItem>> call = syncService.getMovieHistory(traktId);
      Response<List<HistoryItem>> response = call.execute();

      if (response.isSuccessful()) {
        List<HistoryItem> items = response.body();
        List<net.simonvt.cathode.ui.movie.HistoryItem> historyItems = new ArrayList<>();
        for (HistoryItem item : items) {
          final long id = item.getId();
          final String date = df.format(new Date(item.getWatchedAt().getTimeInMillis()));
          net.simonvt.cathode.ui.movie.HistoryItem historyItem =
              new net.simonvt.cathode.ui.movie.HistoryItem(id, date);
          historyItems.add(historyItem);
        }

        return new Result(true, historyItems);
      }
    } catch (IOException e) {
      Timber.d(e, "Loading movie history failed");
    }

    return new Result(false);
  }
}
