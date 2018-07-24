/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

import android.app.Application;
import android.database.Cursor;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import net.simonvt.cathode.common.data.CursorLiveData;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;

public class ShowViewModel extends AndroidViewModel {

  private long showId = -1L;

  private LiveData<Cursor> show;
  private LiveData<Cursor> genres;
  private LiveData<Cursor> seasons;
  private LiveData<Cursor> cast;
  private LiveData<Cursor> userComments;
  private LiveData<Cursor> comments;
  private LiveData<Cursor> related;
  private WatchedLiveData toWatch;
  private CollectLiveData toCollect;

  public ShowViewModel(@NonNull Application application) {
    super(application);
  }

  public void setShowId(long showId) {
    if (this.showId == -1L) {
      this.showId = showId;

      show =
          new CursorLiveData(getApplication(), Shows.withId(showId), ShowFragment.SHOW_PROJECTION,
              null, null, null);
      genres = new CursorLiveData(getApplication(), ProviderSchematic.ShowGenres.fromShow(showId),
          ShowFragment.GENRES_PROJECTION, null, null, null);
      seasons =
          new CursorLiveData(getApplication(), Seasons.fromShow(showId), SeasonsAdapter.PROJECTION,
              null, null, Seasons.DEFAULT_SORT);
      cast = new CursorLiveData(getApplication(), ProviderSchematic.ShowCast.fromShow(showId),
          ShowFragment.CAST_PROJECTION, Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null,
          null);
      userComments = new CursorLiveData(getApplication(), Comments.fromShow(showId),
          ShowFragment.COMMENTS_PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null);
      comments = new CursorLiveData(getApplication(), Comments.fromShow(showId),
          ShowFragment.COMMENTS_PROJECTION,
          CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
          CommentColumns.LIKES + " DESC LIMIT 3");
      related = new CursorLiveData(getApplication(), RelatedShows.fromShow(showId),
          ShowFragment.RELATED_PROJECTION, null, null,
          RelatedShowsColumns.RELATED_INDEX + " ASC LIMIT 3");
      toWatch = new WatchedLiveData(getApplication(), showId, ShowFragment.EPISODE_PROJECTION);
      toCollect = new CollectLiveData(getApplication(), showId, ShowFragment.EPISODE_PROJECTION);
    }
  }

  public LiveData<Cursor> getShow() {
    return show;
  }

  public LiveData<Cursor> getGenres() {
    return genres;
  }

  public LiveData<Cursor> getSeasons() {
    return seasons;
  }

  public LiveData<Cursor> getCast() {
    return cast;
  }

  public LiveData<Cursor> getUserComments() {
    return userComments;
  }

  public LiveData<Cursor> getComments() {
    return comments;
  }

  public LiveData<Cursor> getRelated() {
    return related;
  }

  public LiveData<Cursor> getToWatch() {
    return toWatch;
  }

  public LiveData<Cursor> getToCollect() {
    return toCollect;
  }
}
