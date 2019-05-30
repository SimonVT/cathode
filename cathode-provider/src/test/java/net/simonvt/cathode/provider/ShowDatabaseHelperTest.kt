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

package net.simonvt.cathode.provider

import android.content.ContentValues
import android.content.Context
import android.content.pm.ProviderInfo
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.TestApp
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.generated.CathodeProvider
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.android.controller.ContentProviderController
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TestApp::class, sdk = [21], packageName = "net.simonvt.cathode")
class ShowDatabaseHelperTest {

  private lateinit var provider: ContentProviderController<CathodeProvider>
  private var contentResolver =
    ApplicationProvider.getApplicationContext<Context>().contentResolver

  @Before
  fun setUp() {
    val info = ProviderInfo()
    info.authority = BuildConfig.PROVIDER_AUTHORITY
    provider = Robolectric.buildContentProvider(CathodeProvider::class.java).create(info)
  }

  @Test
  fun getNextEpisodeId() {
    val showId = insertShow("Test show")

    val showHelper = ShowDatabaseHelper(ApplicationProvider.getApplicationContext<Context>())

    var nextEpisode = showHelper.getNextEpisodeId(showId)
    assertNextEpisode(nextEpisode, 1, 1)

    val values = ContentValues()
    values.put(EpisodeColumns.WATCHED, true)

    contentResolver.update(
      Episodes.EPISODES, values,
      EpisodeColumns.SEASON + "=0 AND " + EpisodeColumns.EPISODE + "=2", null
    )

    nextEpisode = showHelper.getNextEpisodeId(showId)
    assertNextEpisode(nextEpisode, 1, 1)

    contentResolver.update(
      Episodes.EPISODES, values,
      EpisodeColumns.SEASON + "=2 AND " + EpisodeColumns.EPISODE + "=2", null
    )

    nextEpisode = showHelper.getNextEpisodeId(showId)
    assertNextEpisode(nextEpisode, 2, 3)

    contentResolver.update(
      Episodes.EPISODES, values,
      EpisodeColumns.SEASON + "=2 AND " + EpisodeColumns.EPISODE + "=10", null
    )

    nextEpisode = showHelper.getNextEpisodeId(showId)
    assertNextEpisode(nextEpisode, 3, 1)
  }

  private fun assertNextEpisode(episodeId: Long, assertSeason: Int, assertEpisode: Int) {
    assertThat(episodeId).isGreaterThan(-1L)

    val episodeCursor = contentResolver.query(
      Episodes.withId(episodeId),
      arrayOf(EpisodeColumns.SEASON, EpisodeColumns.EPISODE),
      null,
      null,
      null
    )

    assertThat(episodeCursor!!.count).isEqualTo(1)

    if (episodeCursor.moveToNext()) {
      val season = episodeCursor.getInt(EpisodeColumns.SEASON)
      val episode = episodeCursor.getInt(EpisodeColumns.EPISODE)
      assertThat(season).isEqualTo(assertSeason)
      assertThat(episode).isEqualTo(assertEpisode)
    }
  }

  private fun insertShow(title: String): Long {
    val values = ContentValues()
    values.put(ShowColumns.TITLE, title)
    val showUri = contentResolver.insert(Shows.SHOWS, values)
    val showId = Shows.getShowId(showUri!!)

    for (i in 10 downTo 0) {
      insertSeason(showId, i)
    }

    return showId
  }

  private fun insertSeason(showId: Long, season: Int) {
    val values = ContentValues()
    values.put(SeasonColumns.SHOW_ID, showId)
    values.put(SeasonColumns.SEASON, season)
    val seasonUri = contentResolver.insert(Seasons.SEASONS, values)
    val seasonId = Seasons.getId(seasonUri!!)

    // Other way around to ensure order doesn't matter
    for (i in 10 downTo 1) {
      insertEpisode(showId, seasonId, season, i)
    }
  }

  private fun insertEpisode(showId: Long, seasonId: Long, season: Int, episode: Int) {
    val values = ContentValues()
    values.put(EpisodeColumns.SHOW_ID, showId)
    values.put(EpisodeColumns.SEASON_ID, seasonId)
    values.put(EpisodeColumns.SEASON, season)
    values.put(EpisodeColumns.EPISODE, episode)
    values.put(EpisodeColumns.FIRST_AIRED, System.currentTimeMillis())
    contentResolver.insert(Episodes.EPISODES, values)
  }
}
