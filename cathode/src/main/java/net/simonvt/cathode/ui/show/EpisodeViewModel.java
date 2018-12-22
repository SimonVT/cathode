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
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.entitymapper.CommentListMapper;
import net.simonvt.cathode.entitymapper.EpisodeMapper;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;

public class EpisodeViewModel extends AndroidViewModel {

  static final String[] PROJECTION = new String[] {
      EpisodeColumns.ID, EpisodeColumns.TRAKT_ID, EpisodeColumns.TITLE, EpisodeColumns.OVERVIEW,
      EpisodeColumns.FIRST_AIRED, EpisodeColumns.WATCHED, EpisodeColumns.IN_COLLECTION,
      EpisodeColumns.IN_WATCHLIST, EpisodeColumns.WATCHING, EpisodeColumns.CHECKED_IN,
      EpisodeColumns.USER_RATING, EpisodeColumns.RATING, EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE, EpisodeColumns.LAST_COMMENT_SYNC, EpisodeColumns.SHOW_ID,
      EpisodeColumns.SEASON_ID, EpisodeColumns.SHOW_TITLE,
  };

  private long episodeId = -1L;

  private LiveData<Episode> episode;
  private LiveData<List<Comment>> userComments;
  private LiveData<List<Comment>> comments;

  public EpisodeViewModel(@NonNull Application application) {
    super(application);
  }

  public void setEpisodeId(long episodeId) {
    if (this.episodeId == -1L) {
      this.episodeId = episodeId;

      episode = new MappedCursorLiveData<>(getApplication(), Episodes.withId(episodeId),
          PROJECTION, null, null, null, new EpisodeMapper());
      userComments = new MappedCursorLiveData<>(getApplication(), Comments.fromEpisode(episodeId),
          CommentListMapper.PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null,
          new CommentListMapper());
      comments = new MappedCursorLiveData<>(getApplication(), Comments.fromEpisode(episodeId),
          CommentListMapper.PROJECTION,
          CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
          CommentColumns.LIKES + " DESC LIMIT 3", new CommentListMapper());
    }
  }

  public LiveData<Episode> getEpisode() {
    return episode;
  }

  public LiveData<List<Comment>> getUserComments() {
    return userComments;
  }

  public LiveData<List<Comment>> getComments() {
    return comments;
  }
}
