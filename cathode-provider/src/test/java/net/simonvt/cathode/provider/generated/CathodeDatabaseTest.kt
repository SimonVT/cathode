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

package net.simonvt.cathode.provider.generated

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.DatabaseHelper
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CathodeDatabaseTest {

  @Test
  @Throws(Exception::class)
  fun testSeasonDeleteTrigger() {
    val helper = DatabaseHelper.getInstance(ApplicationProvider.getApplicationContext<Context>())

    val db = helper.writableDatabase

    val seasonCV = ContentValues()
    seasonCV.put(SeasonColumns.SEASON, 1)

    val seasonId = db.insertOrThrow(Tables.SEASONS, null, seasonCV)

    var seasons = db.query(Tables.SEASONS, null, null, null, null, null, null)
    assertThat(seasons.count).isEqualTo(1)
    seasons.close()

    val episodeCV = ContentValues()
    episodeCV.put(EpisodeColumns.SEASON_ID, seasonId)

    db.insert(Tables.EPISODES, null, episodeCV)

    var episodes = db.query(Tables.EPISODES, null, null, null, null, null, null)
    assertThat(episodes.count).isEqualTo(1)
    episodes.close()

    db.delete(Tables.SEASONS, SeasonColumns.ID + "=" + seasonId, null)

    seasons = db.query(Tables.SEASONS, null, null, null, null, null, null)
    assertThat(seasons.count).isEqualTo(0)
    seasons.close()

    episodes = db.query(Tables.EPISODES, null, null, null, null, null, null)
    assertThat(episodes.count).isEqualTo(0)
    episodes.close()

    db.close()
  }

  @Test
  @Throws(Exception::class)
  fun testShowDeleteTrigger() {
    val helper = DatabaseHelper.getInstance(ApplicationProvider.getApplicationContext<Context>())

    val db = helper.writableDatabase

    // Create show
    val showCV = ContentValues()
    showCV.put(ShowColumns.TRAKT_ID, 1)
    val showId = db.insert(Tables.SHOWS, null, showCV)

    // Create season
    val seasonCV = ContentValues()
    seasonCV.put(SeasonColumns.SEASON, 1)
    seasonCV.put(SeasonColumns.SHOW_ID, showId)
    val seasonId = db.insert(Tables.SEASONS, null, seasonCV)

    var seasons = db.query(Tables.SEASONS, null, null, null, null, null, null)
    assertThat(seasons.count).isEqualTo(1)
    seasons.close()

    // Create episode
    val episodeCV = ContentValues()
    episodeCV.put(EpisodeColumns.SEASON_ID, seasonId)
    episodeCV.put(EpisodeColumns.SHOW_ID, showId)
    db.insert(Tables.EPISODES, null, episodeCV)

    var episodes = db.query(Tables.EPISODES, null, null, null, null, null, null)
    assertThat(episodes.count).isEqualTo(1)
    episodes.close()

    // Create comment
    val commentCV = ContentValues()
    commentCV.put(CommentColumns.ITEM_TYPE, ItemType.SHOW.toString())
    commentCV.put(CommentColumns.ITEM_ID, showId)
    commentCV.put(CommentColumns.COMMENT, "Not null")
    db.insert(Tables.COMMENTS, null, commentCV)

    var comments = db.query(Tables.COMMENTS, null, null, null, null, null, null)
    assertThat(comments.count).isEqualTo(1)
    comments.close()

    // Delete show and check that other tables are empty
    db.delete(Tables.SHOWS, ShowColumns.ID + "=" + showId, null)

    seasons = db.query(Tables.SEASONS, null, null, null, null, null, null)
    assertThat(seasons.count).isEqualTo(0)
    seasons.close()

    episodes = db.query(Tables.EPISODES, null, null, null, null, null, null)
    assertThat(episodes.count).isEqualTo(0)
    episodes.close()

    comments = db.query(Tables.COMMENTS, null, null, null, null, null, null)
    assertThat(comments.count).isEqualTo(0)
    comments.close()

    db.close()
  }
}
