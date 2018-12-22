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
import net.simonvt.cathode.common.data.StringMapper;
import net.simonvt.cathode.common.entity.CastMember;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.common.entity.Episode;
import net.simonvt.cathode.common.entity.Season;
import net.simonvt.cathode.common.entity.Show;
import net.simonvt.cathode.entitymapper.CommentListMapper;
import net.simonvt.cathode.entitymapper.EpisodeMapper;
import net.simonvt.cathode.entitymapper.SeasonListMapper;
import net.simonvt.cathode.entitymapper.ShowCastMapper;
import net.simonvt.cathode.entitymapper.ShowListMapper;
import net.simonvt.cathode.entitymapper.ShowMapper;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.PersonColumns;
import net.simonvt.cathode.provider.DatabaseContract.RelatedShowsColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowCastColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowGenreColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.RelatedShows;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.ShowCast;
import net.simonvt.cathode.provider.ProviderSchematic.ShowGenres;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.util.SqlColumn;

public class ShowViewModel extends AndroidViewModel {

  static final String[] SHOW_PROJECTION = new String[] {
      ShowColumns.ID, ShowColumns.TRAKT_ID, ShowColumns.TITLE, ShowColumns.YEAR,
      ShowColumns.AIR_TIME, ShowColumns.AIR_DAY, ShowColumns.NETWORK, ShowColumns.CERTIFICATION,
      ShowColumns.STATUS, ShowColumns.USER_RATING, ShowColumns.RATING, ShowColumns.OVERVIEW,
      ShowColumns.IN_WATCHLIST, ShowColumns.IN_COLLECTION_COUNT, ShowColumns.WATCHED_COUNT,
      ShowColumns.LAST_SYNC, ShowColumns.LAST_COMMENT_SYNC, ShowColumns.LAST_CREDITS_SYNC,
      ShowColumns.LAST_RELATED_SYNC, ShowColumns.HOMEPAGE, ShowColumns.TRAILER, ShowColumns.IMDB_ID,
      ShowColumns.TVDB_ID, ShowColumns.TMDB_ID, ShowColumns.NEEDS_SYNC, ShowColumns.HIDDEN_CALENDAR,
  };

  public static final String[] SEASONS_PROJECTION = new String[] {
      SeasonColumns.ID, SeasonColumns.SHOW_ID, SeasonColumns.SEASON, SeasonColumns.UNAIRED_COUNT,
      SeasonColumns.WATCHED_COUNT, SeasonColumns.IN_COLLECTION_COUNT, SeasonColumns.AIRED_COUNT,
      SeasonColumns.WATCHED_AIRED_COUNT, SeasonColumns.COLLECTED_AIRED_COUNT,
      SeasonColumns.LAST_MODIFIED, SeasonColumns.SHOW_TITLE,
  };

  static final String[] EPISODE_PROJECTION = new String[] {
      EpisodeColumns.ID, EpisodeColumns.TITLE, EpisodeColumns.FIRST_AIRED, EpisodeColumns.SEASON,
      EpisodeColumns.EPISODE, EpisodeColumns.WATCHED, EpisodeColumns.WATCHING,
      EpisodeColumns.CHECKED_IN,
  };

  static final String[] RELATED_PROJECTION = new String[] {
      SqlColumn.table(Tables.SHOW_RELATED).column(RelatedShowsColumns.RELATED_SHOW_ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.ID),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.TITLE),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.OVERVIEW),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.RATING),
      SqlColumn.table(Tables.SHOWS).column(ShowColumns.VOTES),
  };

  static final String[] GENRES_PROJECTION = new String[] {
      ShowGenreColumns.GENRE,
  };

  private long showId = -1L;

  private LiveData<Show> show;
  private LiveData<List<String>> genres;
  private LiveData<List<Season>> seasons;
  private LiveData<List<CastMember>> cast;
  private LiveData<List<Comment>> userComments;
  private LiveData<List<Comment>> comments;
  private LiveData<List<Show>> related;
  private LiveData<Episode> toWatch;
  private LiveData<Episode> lastWatched;
  private LiveData<Episode> toCollect;
  private LiveData<Episode> lastCollected;

  public ShowViewModel(@NonNull Application application) {
    super(application);
  }

  public void setShowId(long showId) {
    if (this.showId == -1L) {
      this.showId = showId;

      show =
          new MappedCursorLiveData<>(getApplication(), Shows.withId(showId), SHOW_PROJECTION, null,
              null, null, new ShowMapper());
      genres = new MappedCursorLiveData<>(getApplication(), ShowGenres.fromShow(showId),
          GENRES_PROJECTION, null, null, null, new StringMapper(ShowGenreColumns.GENRE));
      seasons =
          new MappedCursorLiveData<>(getApplication(), Seasons.fromShow(showId), SEASONS_PROJECTION,
              null, null, Seasons.DEFAULT_SORT, new SeasonListMapper());
      cast = new MappedCursorLiveData<>(getApplication(), ShowCast.fromShow(showId),
          ShowCastMapper.PROJECTION, Tables.PEOPLE + "." + PersonColumns.NEEDS_SYNC + "=0", null,
          Tables.SHOW_CAST + "." + ShowCastColumns.ID + " ASC LIMIT 3", new ShowCastMapper());
      userComments = new MappedCursorLiveData<>(getApplication(), Comments.fromShow(showId),
          CommentListMapper.PROJECTION, CommentColumns.IS_USER_COMMENT + "=1", null, null,
          new CommentListMapper());
      comments = new MappedCursorLiveData<>(getApplication(), Comments.fromShow(showId),
          CommentListMapper.PROJECTION,
          CommentColumns.IS_USER_COMMENT + "=0 AND " + CommentColumns.SPOILER + "=0", null,
          CommentColumns.LIKES + " DESC LIMIT 3", new CommentListMapper());
      related = new MappedCursorLiveData<>(getApplication(), RelatedShows.fromShow(showId),
          RELATED_PROJECTION, null, null, RelatedShowsColumns.RELATED_INDEX + " ASC LIMIT 3",
          new ShowListMapper());
      toWatch = new WatchedLiveData(getApplication(), showId, EPISODE_PROJECTION);
      lastWatched = new MappedCursorLiveData<>(getApplication(), Episodes.fromShow(showId),
          EPISODE_PROJECTION, EpisodeColumns.WATCHED + "=1", null,
          EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1",
          new EpisodeMapper());
      toCollect = new MappedCursorLiveData<>(getApplication(), Episodes.fromShow(showId),
          EPISODE_PROJECTION, EpisodeColumns.IN_COLLECTION
          + "=0 AND "
          + EpisodeColumns.FIRST_AIRED
          + " IS NOT NULL AND "
          + EpisodeColumns.SEASON
          + ">0", null, EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1",
          new EpisodeMapper());
      lastCollected = new MappedCursorLiveData<>(getApplication(), Episodes.fromShow(showId),
          EPISODE_PROJECTION, EpisodeColumns.IN_COLLECTION + "=1", null,
          EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1",
          new EpisodeMapper());
    }
  }

  public LiveData<Show> getShow() {
    return show;
  }

  public LiveData<List<String>> getGenres() {
    return genres;
  }

  public LiveData<List<Season>> getSeasons() {
    return seasons;
  }

  public LiveData<List<CastMember>> getCast() {
    return cast;
  }

  public LiveData<List<Comment>> getUserComments() {
    return userComments;
  }

  public LiveData<List<Comment>> getComments() {
    return comments;
  }

  public LiveData<List<Show>> getRelated() {
    return related;
  }

  public LiveData<Episode> getToWatch() {
    return toWatch;
  }

  public LiveData<Episode> getLastWatched() {
    return lastWatched;
  }

  public LiveData<Episode> getToCollect() {
    return toCollect;
  }

  public LiveData<Episode> getLastCollected() {
    return lastCollected;
  }
}
