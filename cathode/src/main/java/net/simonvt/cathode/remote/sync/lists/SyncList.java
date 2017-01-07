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

package net.simonvt.cathode.remote.sync.lists;

import android.content.ContentProviderOperation;
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.ListItem;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Person;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.Show;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ListWrapper;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.PersonDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.ListItems;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.sync.SyncPerson;
import net.simonvt.cathode.remote.sync.movies.SyncMovie;
import net.simonvt.cathode.remote.sync.shows.SyncSeason;
import net.simonvt.cathode.remote.sync.shows.SyncShow;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;
import timber.log.Timber;

public class SyncList extends CallJob<List<ListItem>> {

  private static class Item {

    int itemType;

    long itemId;

    public Item(int itemType, long itemId) {
      this.itemType = itemType;
      this.itemId = itemId;
    }
  }

  @Inject transient UsersService usersService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;
  @Inject transient MovieDatabaseHelper movieHelper;
  @Inject transient PersonDatabaseHelper personHelper;

  private long traktId;

  public SyncList(long traktId) {
    super(Flags.REQUIRES_AUTH);
    this.traktId = traktId;
  }

  @Override public String key() {
    return "SyncList&id=" + traktId;
  }

  @Override public int getPriority() {
    return PRIORITY_USER_DATA;
  }

  private int getItemPosition(List<Item> items, int itemType, long itemId) {
    for (int i = 0, count = items.size(); i < count; i++) {
      Item item = items.get(i);
      if (item.itemType == itemType && item.itemId == itemId) {
        return i;
      }
    }

    return -1;
  }

  @Override public Call<List<ListItem>> getCall() {
    return usersService.listItems(traktId);
  }

  @Override public void handleResponse(List<ListItem> items) {
    final long listId = ListWrapper.getId(getContentResolver(), traktId);
    if (listId == -1L) {
      // List has been removed
      return;
    }

    Cursor c = getContentResolver().query(ListItems.inList(listId), new String[] {
        ListItemColumns.ITEM_TYPE, ListItemColumns.ITEM_ID,
    }, null, null, null);

    List<Item> oldItems = new ArrayList<>(c.getCount());

    while (c.moveToNext()) {
      oldItems.add(new Item(Cursors.getInt(c, ListItemColumns.ITEM_TYPE),
          Cursors.getLong(c, ListItemColumns.ITEM_ID)));
    }
    c.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (ListItem item : items) {

      switch (item.getType()) {
        case SHOW: {
          Show show = item.getShow();
          final long showTraktId = show.getIds().getTrakt();
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
          final long showId = showResult.showId;
          if (showResult.didCreate) {
            queue(new SyncShow(showTraktId));
          }

          final int itemPosition =
              getItemPosition(oldItems, DatabaseContract.ItemType.SHOW, showId);
          if (itemPosition >= 0) {
            oldItems.remove(itemPosition);
            continue;
          }

          ContentProviderOperation.Builder opBuilder =
              ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
                  .withValue(ListItemColumns.LISTED_AT, item.getListedAt().getTimeInMillis())
                  .withValue(ListItemColumns.LIST_ID, listId)
                  .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.SHOW)
                  .withValue(ListItemColumns.ITEM_ID, showId);
          ops.add(opBuilder.build());
          break;
        }

        case SEASON: {
          Show show = item.getShow();
          final long showTraktId = show.getIds().getTrakt();
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
          final long showId = showResult.showId;
          final boolean didShowExist = !showResult.didCreate;
          if (showResult.didCreate) {
            queue(new SyncShow(showTraktId));
          }

          Season season = item.getSeason();
          final int seasonNumber = season.getNumber();
          SeasonDatabaseHelper.IdResult seasonResult =
              seasonHelper.getIdOrCreate(showId, seasonNumber);
          final long seasonId = seasonResult.id;
          if (seasonResult.didCreate) {
            if (didShowExist) {
              queue(new SyncShow(showTraktId));
            }
          }

          final int itemPosition =
              getItemPosition(oldItems, DatabaseContract.ItemType.SEASON, seasonId);
          if (itemPosition >= 0) {
            oldItems.remove(itemPosition);
            continue;
          }

          ContentProviderOperation.Builder opBuilder =
              ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
                  .withValue(ListItemColumns.LISTED_AT, item.getListedAt().getTimeInMillis())
                  .withValue(ListItemColumns.LIST_ID, listId)
                  .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.SEASON)
                  .withValue(ListItemColumns.ITEM_ID, seasonId);
          ops.add(opBuilder.build());
          break;
        }

        case EPISODE: {
          Show show = item.getShow();
          Episode episode = item.getEpisode();

          final long showTraktId = show.getIds().getTrakt();
          ShowDatabaseHelper.IdResult showResult = showHelper.getIdOrCreate(showTraktId);
          final long showId = showResult.showId;
          final boolean didShowExist = !showResult.didCreate;
          if (showResult.didCreate) {
            queue(new SyncShow(showTraktId));
          }

          final int seasonNumber = episode.getSeason();
          SeasonDatabaseHelper.IdResult seasonResult =
              seasonHelper.getIdOrCreate(showId, seasonNumber);
          final long seasonId = seasonResult.id;
          final boolean didSeasonExist = !seasonResult.didCreate;
          if (seasonResult.didCreate) {
            if (didShowExist) {
              queue(new SyncShow(showTraktId));
            }
          }

          EpisodeDatabaseHelper.IdResult episodeResult =
              episodeHelper.getIdOrCreate(showId, seasonId, episode.getNumber());
          final long episodeId = episodeResult.id;
          if (episodeResult.didCreate) {
            if (didShowExist && didSeasonExist) {
              queue(new SyncSeason(showTraktId, seasonNumber));
            }
          }

          final int itemPosition =
              getItemPosition(oldItems, DatabaseContract.ItemType.EPISODE, episodeId);
          if (itemPosition >= 0) {
            oldItems.remove(itemPosition);
            continue;
          }

          ContentProviderOperation.Builder opBuilder =
              ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
                  .withValue(ListItemColumns.LISTED_AT, item.getListedAt().getTimeInMillis())
                  .withValue(ListItemColumns.LIST_ID, listId)
                  .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.EPISODE)
                  .withValue(ListItemColumns.ITEM_ID, episodeId);
          ops.add(opBuilder.build());
          break;
        }

        case MOVIE: {
          Movie movie = item.getMovie();
          final long movieTraktId = movie.getIds().getTrakt();
          MovieDatabaseHelper.IdResult result = movieHelper.getIdOrCreate(movieTraktId);
          final long movieId = result.movieId;
          if (result.didCreate) {
            queue(new SyncMovie(movieTraktId));
          }

          final int itemPosition =
              getItemPosition(oldItems, DatabaseContract.ItemType.MOVIE, movieId);
          if (itemPosition >= 0) {
            oldItems.remove(itemPosition);
            continue;
          }

          ContentProviderOperation.Builder opBuilder =
              ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
                  .withValue(ListItemColumns.LISTED_AT, item.getListedAt().getTimeInMillis())
                  .withValue(ListItemColumns.LIST_ID, listId)
                  .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.MOVIE)
                  .withValue(ListItemColumns.ITEM_ID, movieId);
          ops.add(opBuilder.build());
          break;
        }

        case PERSON: {
          Person person = item.getPerson();
          long personId = personHelper.getId(person.getIds().getTrakt());
          if (personId == -1L) {
            personId = personHelper.updateOrInsert(person);
            queue(new SyncPerson(person.getIds().getTrakt()));
          }

          final int itemPosition =
              getItemPosition(oldItems, DatabaseContract.ItemType.PERSON, personId);
          if (itemPosition >= 0) {
            oldItems.remove(itemPosition);
            continue;
          }

          ContentProviderOperation.Builder opBuilder =
              ContentProviderOperation.newInsert(ListItems.LIST_ITEMS)
                  .withValue(ListItemColumns.LISTED_AT, item.getListedAt().getTimeInMillis())
                  .withValue(ListItemColumns.LIST_ID, listId)
                  .withValue(ListItemColumns.ITEM_TYPE, DatabaseContract.ItemType.PERSON)
                  .withValue(ListItemColumns.ITEM_ID, personId);
          ops.add(opBuilder.build());
          break;
        }
      }
    }

    for (Item item : oldItems) {
      ContentProviderOperation.Builder opBuilder =
          ContentProviderOperation.newDelete(ListItems.LIST_ITEMS)
              .withSelection(ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?",
                  new String[] {
                      String.valueOf(item.itemType), String.valueOf(item.itemId),
                  });
      ops.add(opBuilder.build());
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Updating list failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Updating list failed");
      throw new JobFailedException(e);
    }
  }
}
