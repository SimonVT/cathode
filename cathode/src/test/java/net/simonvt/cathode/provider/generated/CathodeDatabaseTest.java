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

package net.simonvt.cathode.provider.generated;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class) public class CathodeDatabaseTest {

  @Test public void testSeasonDeleteTrigger() throws Exception {
    CathodeDatabase helper = CathodeDatabase.getInstance(RuntimeEnvironment.application);

    SQLiteDatabase db = helper.getWritableDatabase();

    ContentValues seasonCV = new ContentValues();
    seasonCV.put(SeasonColumns.SEASON, 1);

    final long seasonId = db.insertOrThrow(Tables.SEASONS, null, seasonCV);

    Cursor seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(1);

    ContentValues episodeCV = new ContentValues();
    episodeCV.put(EpisodeColumns.SEASON_ID, seasonId);

    db.insert(Tables.EPISODES, null, episodeCV);

    Cursor episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(1);

    db.delete(Tables.SEASONS, SeasonColumns.ID + "=" + seasonId, null);

    seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(0);

    episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(0);

    db.close();
  }

  @Test public void testShowDeleteTrigger() throws Exception {
    CathodeDatabase helper = CathodeDatabase.getInstance(RuntimeEnvironment.application);

    SQLiteDatabase db = helper.getWritableDatabase();

    // Create show
    ContentValues showCV = new ContentValues();
    showCV.put(ShowColumns.TRAKT_ID, 1);
    final long showId = db.insert(Tables.SHOWS, null, showCV);

    // Create season
    ContentValues seasonCV = new ContentValues();
    seasonCV.put(SeasonColumns.SEASON, 1);
    seasonCV.put(SeasonColumns.SHOW_ID, showId);
    final long seasonId = db.insert(Tables.SEASONS, null, seasonCV);

    Cursor seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(1);

    // Create episode
    ContentValues episodeCV = new ContentValues();
    episodeCV.put(EpisodeColumns.SEASON_ID, seasonId);
    episodeCV.put(EpisodeColumns.SHOW_ID, showId);
    db.insert(Tables.EPISODES, null, episodeCV);

    Cursor episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(1);

    // Create comment
    ContentValues commentCV = new ContentValues();
    commentCV.put(CommentColumns.ITEM_TYPE, DatabaseContract.ItemType.SHOW);
    commentCV.put(CommentColumns.ITEM_ID, showId);
    commentCV.put(CommentColumns.COMMENT, "Not null");
    db.insert(Tables.COMMENTS, null, commentCV);

    Cursor comments = db.query(Tables.COMMENTS, null, null, null, null, null, null);
    assertThat(comments.getCount()).isEqualTo(1);

    // Delete show and check that other tables are empty
    db.delete(Tables.SHOWS, ShowColumns.ID + "=" + showId, null);

    seasons = db.query(Tables.SEASONS, null, null, null, null, null, null);
    assertThat(seasons.getCount()).isEqualTo(0);

    episodes = db.query(Tables.EPISODES, null, null, null, null, null, null);
    assertThat(episodes.getCount()).isEqualTo(0);

    comments = db.query(Tables.COMMENTS, null, null, null, null, null, null);
    assertThat(comments.getCount()).isEqualTo(0);

    db.close();
  }
}
