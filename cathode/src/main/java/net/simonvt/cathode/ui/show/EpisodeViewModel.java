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
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;

public class EpisodeViewModel extends AndroidViewModel {

  private long episodeId = -1L;

  private LiveData<Cursor> episode;
  private LiveData<Cursor> userComments;
  private LiveData<Cursor> comments;

  public EpisodeViewModel(@NonNull Application application) {
    super(application);
  }

  public void setEpisodeId(long episodeId) {
    if (this.episodeId == -1L) {
      this.episodeId = episodeId;

      episode = new CursorLiveData(getApplication(), Episodes.withId(episodeId),
          EpisodeFragment.EPISODE_PROJECTION, null, null, null);
      userComments = new CursorLiveData(getApplication(), Comments.fromEpisode(episodeId),
          EpisodeFragment.COMMENTS_PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null);
      comments = new CursorLiveData(getApplication(), Comments.fromEpisode(episodeId),
          EpisodeFragment.COMMENTS_PROJECTION,
          CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
          CommentColumns.LIKES + " DESC LIMIT 3");
    }
  }

  public LiveData<Cursor> getEpisode() {
    return episode;
  }

  public LiveData<Cursor> getUserComments() {
    return userComments;
  }

  public LiveData<Cursor> getComments() {
    return comments;
  }
}
