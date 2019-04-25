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

package net.simonvt.cathode.link;

import java.io.IOException;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.CustomList;
import net.simonvt.cathode.api.enumeration.Privacy;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.remote.action.lists.AddEpisode;
import net.simonvt.cathode.remote.action.lists.AddMovie;
import net.simonvt.cathode.remote.action.lists.AddPerson;
import net.simonvt.cathode.remote.action.lists.AddSeason;
import net.simonvt.cathode.remote.action.lists.AddShow;
import net.simonvt.cathode.sync.trakt.UserList;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public class SyncListJob extends Job {

  public interface ListItemType {

    int SHOW = 1;
    int SEASON = 2;
    int EPISODE = 3;
    int MOVIE = 4;
    int PERSON = 5;
  }

  public static class ListItem {

    int itemType;

    long traktId;

    int season;

    int episode;

    public ListItem(int itemType, long traktId) {
      this.itemType = itemType;
      this.traktId = traktId;
    }

    public ListItem(int itemType, long traktId, int season) {
      this.itemType = itemType;
      this.traktId = traktId;
      this.season = season;
    }

    public ListItem(int itemType, long traktId, int season, int episode) {
      this.itemType = itemType;
      this.traktId = traktId;
      this.season = season;
      this.episode = episode;
    }
  }

  @Inject transient UsersService usersService;
  @Inject transient UserList userList;

  String name;

  String description;

  List<ListItem> items;

  public SyncListJob(String name, String description, List<ListItem> items) {
    super();
    this.name = name;
    this.description = description;
    this.items = items;
  }

  @Override public String key() {
    return "SyncListJob&name=" + name;
  }

  @Override public boolean perform() {
    try {
      long listId = getListTraktId(name);

      if (listId == -1L) {
        if (!userList.create(name, description, Privacy.PUBLIC, true, true)) {
          return false;
        }

        listId = getListTraktId(name);
        if (listId == -1L) {
          return false;
        }
      }

      for (ListItem item : items) {
        switch (item.itemType) {
          case ListItemType.SHOW: {
            queue(new AddShow(listId, item.traktId));
            break;
          }

          case ListItemType.SEASON: {
            queue(new AddSeason(listId, item.traktId, item.season));
            break;
          }

          case ListItemType.EPISODE: {
            queue(new AddEpisode(listId, item.traktId, item.season, item.episode));
            break;
          }

          case ListItemType.MOVIE: {
            queue(new AddMovie(listId, item.traktId));
            break;
          }

          case ListItemType.PERSON: {
            queue(new AddPerson(listId, item.traktId));
            break;
          }
        }
      }

      return true;
    } catch (IOException e) {
      Timber.d(e, "Job failed: %s", key());
      return false;
    }
  }

  private long getListTraktId(String name) throws IOException {
    Call<List<CustomList>> listsCall = usersService.lists();
    Response<List<CustomList>> listsResponse = listsCall.execute();
    if (!listsResponse.isSuccessful()) {
      return -1L;
    }

    List<CustomList> lists = listsResponse.body();
    long listTraktId = -1L;

    for (CustomList list : lists) {
      if (name.equals(list.getName())) {
        listTraktId = list.getIds().getTrakt();
      }
    }

    return listTraktId;
  }
}
