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

package net.simonvt.cathode.scheduler;

import android.content.Context;
import javax.inject.Inject;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.ListItemColumns;
import net.simonvt.cathode.provider.EpisodeWrapper;
import net.simonvt.cathode.provider.ListWrapper;
import net.simonvt.cathode.provider.MovieWrapper;
import net.simonvt.cathode.provider.PersonWrapper;
import net.simonvt.cathode.provider.ProviderSchematic.ListItems;
import net.simonvt.cathode.provider.SeasonWrapper;
import net.simonvt.cathode.provider.ShowWrapper;
import net.simonvt.cathode.remote.action.lists.CreateList;
import net.simonvt.cathode.remote.action.lists.RemoveEpisode;
import net.simonvt.cathode.remote.action.lists.RemoveMovie;
import net.simonvt.cathode.remote.action.lists.RemovePerson;
import net.simonvt.cathode.remote.action.lists.RemoveSeason;
import net.simonvt.cathode.remote.action.lists.RemoveShow;

public class ListsTaskScheduler extends BaseTaskScheduler {

  @Inject SyncService syncService;

  public ListsTaskScheduler(Context context) {
    super(context);
  }

  public void createList(final String name, final String description, final Privacy privacy,
      final boolean displayNumbers, final boolean allowComments) {
    execute(new Runnable() {
      @Override public void run() {
        final long listId =
            ListWrapper.createList(context.getContentResolver(), name, description, privacy,
                displayNumbers, allowComments);

        queue(new CreateList(listId, name, description, privacy, displayNumbers, allowComments));
      }
    });
  }

  public void removeItem(final long listId, final int itemType, final long itemId) {
    execute(new Runnable() {
      @Override public void run() {
        final long listTraktId = ListWrapper.getTraktId(context.getContentResolver(), listId);
        context.getContentResolver().delete(ListItems.LIST_ITEMS,
            ListItemColumns.ITEM_TYPE + "=? AND " + ListItemColumns.ITEM_ID + "=?", new String[] {
                String.valueOf(itemType), String.valueOf(itemId),
            });

        switch (itemType) {
          case ListItemColumns.Type.SHOW: {
            final long showTraktId = ShowWrapper.getTraktId(context.getContentResolver(), itemId);

            queue(new RemoveShow(listTraktId, showTraktId));
            break;
          }

          case ListItemColumns.Type.SEASON: {
            final long showId = SeasonWrapper.getShowId(context.getContentResolver(), itemId);
            final long showTraktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
            final int seasonNumber =
                SeasonWrapper.getSeasonNumber(context.getContentResolver(), itemId);

            queue(new RemoveSeason(listTraktId, showTraktId, seasonNumber));
            break;
          }

          case ListItemColumns.Type.EPISODE: {
            final long showId = EpisodeWrapper.getShowId(context.getContentResolver(), itemId);
            final long showTraktId = ShowWrapper.getTraktId(context.getContentResolver(), showId);
            final long seasonId = EpisodeWrapper.getSeason(context.getContentResolver(), itemId);
            final int seasonNumber =
                SeasonWrapper.getSeasonNumber(context.getContentResolver(), seasonId);
            final int episodeNumber =
                EpisodeWrapper.getEpisodeNumber(context.getContentResolver(), itemId);

            queue(new RemoveEpisode(listTraktId, showTraktId, seasonNumber, episodeNumber));
            break;
          }

          case ListItemColumns.Type.MOVIE: {
            final long movieTraktId = MovieWrapper.getTraktId(context.getContentResolver(), itemId);

            queue(new RemoveMovie(listTraktId, movieTraktId));
            break;
          }

          case ListItemColumns.Type.PERSON: {
            final long personTraktId =
                PersonWrapper.getTraktId(context.getContentResolver(), itemId);

            queue(new RemovePerson(listTraktId, personTraktId));
            break;
          }
        }
      }
    });
  }
}
