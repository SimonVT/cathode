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

package net.simonvt.cathode.remote.sync;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.HiddenItem;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.HiddenSection;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.PagedCallJob;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import timber.log.Timber;

public class SyncHiddenWatched extends PagedCallJob<HiddenItem> {

  @Inject transient UsersService usersService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;

  public SyncHiddenWatched() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncHiddenWatched";
  }

  @Override public int getPriority() {
    return 0;
  }

  @Override public Call<List<HiddenItem>> getCall(int page) {
    return usersService.getHiddenItems(HiddenSection.PROGRESS_WATCHED, page, 25);
  }

  @Override public void handleResponse(List<HiddenItem> items) {
    List<Long> unhandledShows = new ArrayList<>();
    Cursor hiddenShows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, ShowColumns.HIDDEN_WATCHED + "=1", null, null);
    while (hiddenShows.moveToNext()) {
      final long id = Cursors.getLong(hiddenShows, ShowColumns.ID);
      unhandledShows.add(id);
    }
    hiddenShows.close();

    List<Long> unhandledSeasons = new ArrayList<>();
    Cursor hiddenSeasons = getContentResolver().query(Seasons.SEASONS, new String[] {
        SeasonColumns.ID,
    }, SeasonColumns.HIDDEN_WATCHED + "=1", null, null);
    while (hiddenSeasons.moveToNext()) {
      final long id = Cursors.getLong(hiddenSeasons, SeasonColumns.ID);
      unhandledSeasons.add(id);
    }
    hiddenSeasons.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (HiddenItem item : items) {
      switch (item.getType()) {
        case SHOW: {
          Show show = item.getShow();
          final long traktId = show.getIds().getTrakt();
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
          final long showId = showResult.showId;
          if (showResult.didCreate) {
            queue(new SyncShow(traktId));
          }

          if (!unhandledShows.remove(showId)) {
            ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
                .withValue(ShowColumns.HIDDEN_WATCHED, 1)
                .build();
            ops.add(op);
          }
          break;
        }

        case SEASON: {
          Show show = item.getShow();
          final long traktId = show.getIds().getTrakt();
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(traktId);
          final long showId = showResult.showId;
          if (showResult.didCreate) {
            queue(new SyncShow(traktId));
          }

          Season season = item.getSeason();
          final int seasonNumber = season.getNumber();
          SeasonDatabaseHelper.IdResult result = seasonHelper.getIdOrCreate(showId, seasonNumber);
          final long seasonId = result.id;
          if (result.didCreate) {
            if (!showResult.didCreate) {
              queue(new SyncShow(show.getIds().getTrakt()));
            }
          }

          if (!unhandledSeasons.remove(seasonId)) {
            ContentProviderOperation op =
                ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
                    .withValue(SeasonColumns.HIDDEN_WATCHED, 1)
                    .build();
            ops.add(op);
          }
          break;
        }
      }
    }

    for (long showId : unhandledShows) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
          .withValue(ShowColumns.HIDDEN_WATCHED, 0)
          .build();
      ops.add(op);
    }

    for (long seasonId : unhandledSeasons) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
          .withValue(SeasonColumns.HIDDEN_WATCHED, 0)
          .build();
      ops.add(op);
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Unable to update hidden state");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Unable to update hidden state");
    }
  }
}
