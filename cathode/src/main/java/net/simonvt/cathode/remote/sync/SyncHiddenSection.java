/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.enumeration.HiddenSection;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.provider.DatabaseContract.HiddenColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.PagedCallJob;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import retrofit2.Call;
import timber.log.Timber;

public class SyncHiddenSection extends PagedCallJob<HiddenItem> {

  @Inject transient UsersService usersService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient MovieDatabaseHelper movieHelper;

  private HiddenSection section;

  public SyncHiddenSection(HiddenSection section) {
    super(Flags.REQUIRES_AUTH);
    this.section = section;
  }

  @Override public String key() {
    return "SyncHiddenItems?section" + section.toString();
  }

  @Override public int getPriority() {
    return 0;
  }

  @Override public Call<List<HiddenItem>> getCall(int page) {
    return usersService.getHiddenItems(section, page, 25);
  }

  @Override public void handleResponse(List<HiddenItem> items) {
    String hiddenColumn;
    switch (section) {
      case CALENDAR:
        hiddenColumn = HiddenColumns.HIDDEN_CALENDAR;
        break;

      case PROGRESS_WATCHED:
        hiddenColumn = HiddenColumns.HIDDEN_WATCHED;
        break;

      case PROGRESS_COLLECTED:
        hiddenColumn = HiddenColumns.HIDDEN_COLLECTED;
        break;

      case RECOMMENDATIONS:
        hiddenColumn = HiddenColumns.HIDDEN_RECOMMENDATIONS;
        break;

      default:
        throw new RuntimeException("Unknown section: " + section.toString());
    }

    List<Long> unhandledShows = new ArrayList<>();
    Cursor hiddenShows = getContentResolver().query(Shows.SHOWS, new String[] {
        ShowColumns.ID,
    }, hiddenColumn + "=1", null, null);
    while (hiddenShows.moveToNext()) {
      final long id = hiddenShows.getLong(hiddenShows.getColumnIndex(ShowColumns.ID));
      unhandledShows.add(id);
    }
    hiddenShows.close();

    List<Long> unhandledMovies = new ArrayList<>();
    Cursor hiddenMovies = getContentResolver().query(Movies.MOVIES, new String[] {
        MovieColumns.ID,
    }, hiddenColumn + "=1", null, null);
    while (hiddenMovies.moveToNext()) {
      final long id = hiddenMovies.getLong(hiddenMovies.getColumnIndex(MovieColumns.ID));
      unhandledMovies.add(id);
    }
    hiddenMovies.close();

    List<Long> unhandledSeasons = new ArrayList<>();
    Cursor hiddenSeasons = getContentResolver().query(Seasons.SEASONS, new String[] {
        SeasonColumns.ID,
    }, hiddenColumn + "=1", null, null);
    while (hiddenSeasons.moveToNext()) {
      final long id = hiddenSeasons.getLong(hiddenSeasons.getColumnIndex(SeasonColumns.ID));
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
                .withValue(hiddenColumn, 1)
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
                    .withValue(hiddenColumn, 1)
                    .build();
            ops.add(op);
          }
          break;
        }

        case MOVIE: {
          Movie movie = item.getMovie();
          final long traktId = movie.getIds().getTrakt();
          MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(traktId);
          final long movieId = result.movieId;
          if (result.didCreate) {
            queue(new SyncMovie(traktId));
          }

          if (!unhandledMovies.remove(movieId)) {
            ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
                .withValue(hiddenColumn, 1)
                .build();
            ops.add(op);
          }
          break;
        }
      }
    }

    /** TODO: Once trakt supports hiding via the API, remove this
    for (long showId : unhandledShows) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Shows.withId(showId))
          .withValue(hiddenColumn, 0)
          .build();
      ops.add(op);
    }

    for (long seasonId : unhandledSeasons) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Seasons.withId(seasonId))
          .withValue(hiddenColumn, 0)
          .build();
      ops.add(op);
    }

    for (long movieId : unhandledMovies) {
      ContentProviderOperation op = ContentProviderOperation.newUpdate(Movies.withId(movieId))
          .withValue(hiddenColumn, 0)
          .build();
      ops.add(op);
    }
     */

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Unable to update hidden state");
    } catch (OperationApplicationException e) {
      Timber.e(e, "Unable to update hidden state");
    }
  }
}
