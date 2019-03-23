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

package net.simonvt.cathode.jobqueue;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import net.simonvt.schematic.annotation.Database;
import net.simonvt.schematic.annotation.OnUpgrade;
import net.simonvt.schematic.annotation.Table;

@Database(
    className = "JobDatabase",
    packageName = "net.simonvt.cathode.jobqueue.database",
    fileName = "jobDatabase.db",
    version = 11
)
public class JobDatabaseSchematic {

  private JobDatabaseSchematic() {
  }

  public static class Tables {

    @Table(JobColumns.class) public static final String JOBS = "jobs";
  }

  @OnUpgrade
  public static void onUpgrade(Context context, SQLiteDatabase db, int oldVersion, int newVersion) {
    if (oldVersion < 2) {
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncActivityStream");
    }

    if (oldVersion < 3) {
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncShowCast");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncMovieCrew");
    }

    if (oldVersion < 4) {
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncHiddenSection");
      deleteJob(db, "net.simonvt.cathode.remote.sync.PurgeDatabase");
    }

    if (oldVersion < 5) {
      deleteJob(db, "net.simonvt.cathode.remote.action.movies.WatchedMovie");
      deleteJob(db, "net.simonvt.cathode.remote.action.shows.WatchedShow");
      deleteJob(db, "net.simonvt.cathode.remote.action.shows.WatchedSeason");
      deleteJob(db, "net.simonvt.cathode.remote.action.shows.WatchedEpisode");
    }

    if (oldVersion < 6) {
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.StartSyncUpdatedShows");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.StartSyncUpdatedMovies");
    }

    if (oldVersion < 7) {
      deleteJob(db, "net.simonvt.cathode.remote.action.movies.CheckInMovie");
      deleteJob(db, "net.simonvt.cathode.remote.action.shows.CheckInEpisode");
      deleteJob(db, "net.simonvt.cathode.remote.action.CancelCheckin");
    }

    if (oldVersion < 8) {
      deleteJob(db, "net.simonvt.cathode.remote.action.lists.CreateList");
      deleteJob(db, "net.simonvt.cathode.remote.action.lists.UpdateList");
      deleteJob(db, "net.simonvt.cathode.remote.action.lists.DeleteList");
    }

    if (oldVersion < 9) {
      deleteJob(db, "net.simonvt.cathode.tmdb.api.SyncConfiguration");
      deleteJob(db, "net.simonvt.cathode.tmdb.api.movie.SyncMovieImages");
      deleteJob(db, "net.simonvt.cathode.tmdb.api.people.SyncPersonBackdrop");
      deleteJob(db, "net.simonvt.cathode.tmdb.api.people.SyncPersonHeadshot");
      deleteJob(db, "net.simonvt.cathode.tmdb.api.show.SyncEpisodeImages");
      deleteJob(db, "net.simonvt.cathode.tmdb.api.show.SyncShowImages");
    }

    if (oldVersion < 10) {
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncPendingSeasons");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncPendingShows");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncUpdatedShows");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncUserShows");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncMovie");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncPendingMovies");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncUpdatedMovies");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncUserMovies");
    }

    if (oldVersion < 11) {
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncHiddenCalendar");
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncHiddenCollected");
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncHiddenRecommendations");
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncHiddenWatched");
      deleteJob(db, "net.simonvt.cathode.remote.sync.SyncPerson");
      deleteJob(db, "net.simonvt.cathode.remote.sync.comments.SyncComments");
      deleteJob(db, "net.simonvt.cathode.remote.sync.lists.SyncList");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncAnticipatedMovies");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncMovieCredits");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncMovieRecommendations");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncRelatedMovies");
      deleteJob(db, "net.simonvt.cathode.remote.sync.movies.SyncTrendingMovies");
      deleteJob(db, "net.simonvt.cathode.remote.sync.people.SyncPersonMovieCredits");
      deleteJob(db, "net.simonvt.cathode.remote.sync.people.SyncPersonShowCredits");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncAnticipatedShows");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncRelatedShows");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncShowCollectedStatus");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncShowCredits");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncShowRecommendations");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncShowWatchedStatus");
      deleteJob(db, "net.simonvt.cathode.remote.sync.shows.SyncTrendingShows");
      
      deleteJob(db, "net.simonvt.cathode.sync.tmdb.api.people.SyncPersonBackdrop");
      deleteJob(db, "net.simonvt.cathode.sync.tmdb.api.people.SyncPersonHeadshot");
    }
  }

  private static void deleteJob(SQLiteDatabase db, String job) {
    db.delete(Tables.JOBS, JobColumns.JOB_NAME + "=?", new String[] {
        job,
    });
  }
}
