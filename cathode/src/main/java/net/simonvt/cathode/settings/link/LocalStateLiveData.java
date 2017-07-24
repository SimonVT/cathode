/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings.link;

import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.util.TimeUtils;
import net.simonvt.cathode.common.data.AsyncLiveData;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.link.SyncListJob;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.DatabaseContract.ListsColumns;
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.ListItems;
import net.simonvt.cathode.provider.ProviderSchematic.Lists;
import net.simonvt.cathode.provider.ProviderSchematic.Movies;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.cathode.remote.action.movies.AddMovieToHistory;
import net.simonvt.cathode.remote.action.movies.CollectMovie;
import net.simonvt.cathode.remote.action.movies.WatchlistMovie;
import net.simonvt.cathode.remote.action.shows.AddEpisodeToHistory;
import net.simonvt.cathode.remote.action.shows.CollectEpisode;
import net.simonvt.cathode.remote.action.shows.WatchlistEpisode;
import net.simonvt.cathode.remote.action.shows.WatchlistShow;
import net.simonvt.schematic.Cursors;
import timber.log.Timber;

public class LocalStateLiveData extends AsyncLiveData<List<Job>> {

  private static final String[] PROJECTION_EPISODES = new String[] {
      EpisodeColumns.SHOW_ID, EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
      EpisodeColumns.LAST_WATCHED_AT, EpisodeColumns.COLLECTED_AT, EpisodeColumns.LISTED_AT,
  };

  private static final String[] PROJECTION_SHOWS = new String[] {
      ShowColumns.TRAKT_ID, ShowColumns.LISTED_AT,
  };

  private static final String[] PROJECTION_MOVIES = new String[] {
      MovieColumns.TRAKT_ID, MovieColumns.LISTED_AT, MovieColumns.COLLECTED_AT,
  };

  private static final String[] PROJECTION_LISTS = new String[] {
      ListsColumns.ID, ListsColumns.NAME, ListsColumns.DESCRIPTION, ListsColumns.PRIVACY,
      ListsColumns.DISPLAY_NUMBERS, ListsColumns.ALLOW_COMMENTS,
  };

  private static final String[] PROJECTION_LIST_ITEMS = new String[] {
      ListItemColumns.LIST_ID, ListItemColumns.ITEM_TYPE, ListItemColumns.ITEM_ID,
      ListItemColumns.LIST_ID,
  };

  private Context context;
  private ShowDatabaseHelper showHelper;
  private SeasonDatabaseHelper seasonHelper;
  private EpisodeDatabaseHelper episodeHelper;
  private MovieDatabaseHelper movieHelper;
  private PersonDatabaseHelper personHelper;

  public LocalStateLiveData(Context context, ShowDatabaseHelper showHelper,
      SeasonDatabaseHelper seasonHelper, EpisodeDatabaseHelper episodeHelper,
      MovieDatabaseHelper movieHelper, PersonDatabaseHelper personHelper) {
    this.context = context;
    this.showHelper = showHelper;
    this.seasonHelper = seasonHelper;
    this.episodeHelper = episodeHelper;
    this.movieHelper = movieHelper;
    this.personHelper = personHelper;
  }

  @Override public List<Job> loadInBackground() {
    ContentResolver resolver = context.getContentResolver();
    List<Job> jobs = new ArrayList<>();

    Cursor c =
        resolver.query(Episodes.EPISODES, PROJECTION_EPISODES, EpisodeColumns.WATCHED + "=1", null,
            null);
    while (c.moveToNext()) {
      final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
      final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(c, EpisodeColumns.EPISODE);

      final long showTraktId = showHelper.getTraktId(showId);

      jobs.add(new AddEpisodeToHistory(showTraktId, season, episode, null));
    }
    c.close();

    c = resolver.query(Episodes.EPISODES, PROJECTION_EPISODES, EpisodeColumns.IN_COLLECTION + "=1",
        null, null);
    while (c.moveToNext()) {
      final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
      final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(c, EpisodeColumns.EPISODE);
      final long collectedAt = Cursors.getLong(c, EpisodeColumns.COLLECTED_AT);

      final long showTraktId = showHelper.getTraktId(showId);

      jobs.add(new CollectEpisode(showTraktId, season, episode, true,
          TimeUtils.getIsoTime(collectedAt)));
    }
    c.close();

    c = resolver.query(Shows.SHOWS_WATCHLIST, PROJECTION_SHOWS, null, null, null);
    while (c.moveToNext()) {
      final long traktId = Cursors.getLong(c, ShowColumns.TRAKT_ID);
      final long listedAt = Cursors.getLong(c, ShowColumns.LISTED_AT);

      jobs.add(new WatchlistShow(traktId, true, TimeUtils.getIsoTime(listedAt)));
    }
    c.close();

    c = resolver.query(Episodes.EPISODES, PROJECTION_EPISODES, EpisodeColumns.IN_WATCHLIST + "=1",
        null, null);
    while (c.moveToNext()) {
      final long showId = Cursors.getLong(c, EpisodeColumns.SHOW_ID);
      final int season = Cursors.getInt(c, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(c, EpisodeColumns.EPISODE);
      final long listedAt = Cursors.getLong(c, EpisodeColumns.LISTED_AT);

      final long showTraktId = showHelper.getTraktId(showId);

      jobs.add(
          new WatchlistEpisode(showTraktId, season, episode, true, TimeUtils.getIsoTime(listedAt)));
    }
    c.close();

    c = resolver.query(Movies.MOVIES_WATCHED, PROJECTION_MOVIES, null, null, null);
    while (c.moveToNext()) {
      final long traktId = Cursors.getLong(c, MovieColumns.TRAKT_ID);

      jobs.add(new AddMovieToHistory(traktId, null));
    }
    c.close();

    c = resolver.query(Movies.MOVIES_COLLECTED, PROJECTION_MOVIES, null, null, null);
    while (c.moveToNext()) {
      final long traktId = Cursors.getLong(c, MovieColumns.TRAKT_ID);
      final long collectedAt = Cursors.getLong(c, MovieColumns.COLLECTED_AT);

      jobs.add(new CollectMovie(traktId, true, TimeUtils.getIsoTime(collectedAt)));
    }
    c.close();

    c = resolver.query(Movies.MOVIES_WATCHLIST, PROJECTION_MOVIES, null, null, null);
    while (c.moveToNext()) {
      final long traktId = Cursors.getLong(c, MovieColumns.TRAKT_ID);
      final long listedAt = Cursors.getLong(c, MovieColumns.LISTED_AT);

      jobs.add(new WatchlistMovie(traktId, true, TimeUtils.getIsoTime(listedAt)));
    }
    c.close();

    c = resolver.query(Lists.LISTS, PROJECTION_LISTS, null, null, null);
    while (c.moveToNext()) {
      final long id = Cursors.getLong(c, ListsColumns.ID);
      final String name = Cursors.getString(c, ListsColumns.NAME);
      final String description = Cursors.getString(c, ListsColumns.DESCRIPTION);

      List<SyncListJob.ListItem> items = new ArrayList<>();

      Cursor listItemsCursor =
          resolver.query(ListItems.inList(id), PROJECTION_LIST_ITEMS, null, null, null);
      while (listItemsCursor.moveToNext()) {
        final int itemType = Cursors.getInt(listItemsCursor, ListItemColumns.ITEM_TYPE);
        final long itemId = Cursors.getInt(listItemsCursor, ListItemColumns.ITEM_ID);

        switch (itemType) {
          case DatabaseContract.ItemType.SHOW: {
            final long traktId = showHelper.getTraktId(itemId);
            items.add(new SyncListJob.ListItem(itemType, traktId));
            break;
          }

          case DatabaseContract.ItemType.SEASON: {
            final long showId = seasonHelper.getShowId(itemId);
            final long traktId = showHelper.getTraktId(showId);
            final int season = seasonHelper.getNumber(itemId);
            items.add(new SyncListJob.ListItem(itemType, traktId, season));
            break;
          }

          case DatabaseContract.ItemType.EPISODE: {
            final long showId = episodeHelper.getShowId(itemId);
            final long traktId = showHelper.getTraktId(showId);
            final int season = episodeHelper.getSeason(itemId);
            final int episode = episodeHelper.getNumber(itemId);
            items.add(new SyncListJob.ListItem(itemType, traktId, season, episode));
            break;
          }

          case DatabaseContract.ItemType.MOVIE: {
            final long traktId = movieHelper.getTraktId(itemId);
            items.add(new SyncListJob.ListItem(itemType, traktId));
            break;
          }

          case DatabaseContract.ItemType.PERSON: {
            final long traktId = personHelper.getTraktId(itemId);
            items.add(new SyncListJob.ListItem(itemType, traktId));
            break;
          }
        }
      }
      listItemsCursor.close();

      jobs.add(new SyncListJob(name, description, items));
    }
    c.close();

    if (BuildConfig.DEBUG) {
      Timber.d("Created jobs");
      for (Job job : jobs) {
        Timber.d("%s", job.key());
      }
    }

    return jobs;
  }
}
