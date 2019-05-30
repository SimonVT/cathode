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

package net.simonvt.cathode.entitymapper

import android.database.Cursor
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.common.database.getBoolean
import net.simonvt.cathode.common.database.getInt
import net.simonvt.cathode.common.database.getLong
import net.simonvt.cathode.common.database.getStringOrNull
import net.simonvt.cathode.entity.NextEpisode
import net.simonvt.cathode.entity.ShowWithEpisode
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.util.DataHelper

object ShowWithEpisodeMapper : MappedCursorLiveData.CursorMapper<ShowWithEpisode> {

  override fun map(cursor: Cursor): ShowWithEpisode? {
    return if (cursor.moveToFirst()) mapShowAndEpisode(cursor) else null
  }

  fun mapShowAndEpisode(cursor: Cursor): ShowWithEpisode {
    val showId = cursor.getLong(ShowColumns.ID)
    val show = ShowMapper.mapShow(cursor)

    val episodeId = cursor.getLong(COLUMN_EPISODE_ID)
    val episodeTitle = cursor.getStringOrNull(EpisodeColumns.TITLE)
    val watched = cursor.getBoolean(EpisodeColumns.WATCHED)
    var firstAired = cursor.getLong(EpisodeColumns.FIRST_AIRED)
    if (firstAired != 0L) {
      firstAired = DataHelper.getFirstAired(firstAired)
    }
    val season = cursor.getInt(EpisodeColumns.SEASON)
    val number = cursor.getInt(EpisodeColumns.EPISODE)
    val checkinStartedAt = cursor.getLong(EpisodeColumns.STARTED_AT)
    val checkinExpiresAt = cursor.getLong(EpisodeColumns.EXPIRES_AT)

    val episode = NextEpisode(
      episodeId,
      showId,
      season,
      number,
      episodeTitle,
      firstAired,
      watched,
      checkinStartedAt,
      checkinExpiresAt
    )
    return ShowWithEpisode(show, episode)
  }

  private const val COLUMN_EPISODE_ID = "episodeId"

  val projection = ShowMapper.projection.plus(
    arrayOf(
      Tables.EPISODES + "." + EpisodeColumns.ID + " AS " + COLUMN_EPISODE_ID,
      Tables.EPISODES + "." + EpisodeColumns.TITLE,
      Tables.EPISODES + "." + EpisodeColumns.WATCHED,
      Tables.EPISODES + "." + EpisodeColumns.FIRST_AIRED,
      Tables.EPISODES + "." + EpisodeColumns.SEASON,
      Tables.EPISODES + "." + EpisodeColumns.EPISODE,
      Tables.EPISODES + "." + EpisodeColumns.STARTED_AT,
      Tables.EPISODES + "." + EpisodeColumns.EXPIRES_AT
    )
  )
}
