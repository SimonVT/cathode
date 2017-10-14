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

package net.simonvt.cathode.provider;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.net.Uri;
import net.simonvt.cathode.TestApp;
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns;
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns;
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Episodes;
import net.simonvt.cathode.provider.ProviderSchematic.Seasons;
import net.simonvt.cathode.provider.ProviderSchematic.Shows;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import net.simonvt.schematic.Cursors;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.util.ContentProviderController;

import static com.google.common.truth.Truth.assertThat;

@RunWith(RobolectricTestRunner.class)
@Config(application = TestApp.class, constants = BuildConfig.class, sdk = 21,
    packageName = "net.simonvt.cathode") public class ShowDatabaseHelperTest {

  ContentProviderController<CathodeProvider> provider;
  ContentResolver contentResolver = RuntimeEnvironment.application.getContentResolver();

  @Before public void setUp() {
    ProviderInfo info = new ProviderInfo();
    info.authority = BuildConfig.PROVIDER_AUTHORITY;
    provider = Robolectric.buildContentProvider(CathodeProvider.class).create(info);
  }

  @Test public void getNextEpisodeId() throws Exception {
    final long showId = insertShow("Test show");

    ShowDatabaseHelper showHelper = ShowDatabaseHelper.getInstance(RuntimeEnvironment.application);

    long nextEpisode = showHelper.getNextEpisodeId(showId);
    assertNextEpisode(nextEpisode, 1, 1);

    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.WATCHED, true);

    contentResolver.update(Episodes.EPISODES, values,
        EpisodeColumns.SEASON + "=0 AND " + EpisodeColumns.EPISODE + "=2", null);

    nextEpisode = showHelper.getNextEpisodeId(showId);
    assertNextEpisode(nextEpisode, 1, 1);

    contentResolver.update(Episodes.EPISODES, values,
        EpisodeColumns.SEASON + "=2 AND " + EpisodeColumns.EPISODE + "=2", null);

    nextEpisode = showHelper.getNextEpisodeId(showId);
    assertNextEpisode(nextEpisode, 2, 3);

    contentResolver.update(Episodes.EPISODES, values,
        EpisodeColumns.SEASON + "=2 AND " + EpisodeColumns.EPISODE + "=10", null);

    nextEpisode = showHelper.getNextEpisodeId(showId);
    assertNextEpisode(nextEpisode, 3, 1);
  }

  private void assertNextEpisode(long episodeId, int assertSeason, int assertEpisode) {
    assertThat(episodeId).isGreaterThan(-1L);

    Cursor episodeCursor = contentResolver.query(Episodes.withId(episodeId), new String[] {
        EpisodeColumns.SEASON, EpisodeColumns.EPISODE,
    }, null, null, null);

    assertThat(episodeCursor.getCount()).isEqualTo(1);

    if (episodeCursor.moveToNext()) {
      final int season = Cursors.getInt(episodeCursor, EpisodeColumns.SEASON);
      final int episode = Cursors.getInt(episodeCursor, EpisodeColumns.EPISODE);
      assertThat(season).isEqualTo(assertSeason);
      assertThat(episode).isEqualTo(assertEpisode);
    }
  }

  private long insertShow(String title) {
    ContentValues values = new ContentValues();
    values.put(ShowColumns.TITLE, title);
    Uri showUri = contentResolver.insert(Shows.SHOWS, values);
    long showId = Shows.getShowId(showUri);

    for (int i = 10; i >= 0; i--) {
      insertSeason(showId, i);
    }

    return showId;
  }

  private void insertSeason(long showId, int season) {
    ContentValues values = new ContentValues();
    values.put(SeasonColumns.SHOW_ID, showId);
    values.put(SeasonColumns.SEASON, season);
    Uri seasonUri = contentResolver.insert(Seasons.SEASONS, values);
    long seasonId = Seasons.getId(seasonUri);

    // Other way around to ensure order doesn't matter
    for (int i = 10; i > 0; i--) {
      insertEpisode(showId, seasonId, season, i);
    }
  }

  private void insertEpisode(long showId, long seasonId, int season, int episode) {
    ContentValues values = new ContentValues();
    values.put(EpisodeColumns.SHOW_ID, showId);
    values.put(EpisodeColumns.SEASON_ID, seasonId);
    values.put(EpisodeColumns.SEASON, season);
    values.put(EpisodeColumns.EPISODE, episode);
    values.put(EpisodeColumns.FIRST_AIRED, System.currentTimeMillis());
    contentResolver.insert(Episodes.EPISODES, values);
  }
}
