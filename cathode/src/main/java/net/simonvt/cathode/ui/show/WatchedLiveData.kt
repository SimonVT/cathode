/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.show

import android.content.Context
import android.database.Cursor
import android.net.Uri
import net.simonvt.cathode.common.data.ListenableLiveData
import net.simonvt.cathode.common.database.Cursors
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.entitymapper.EpisodeMapper
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.query

class WatchedLiveData(context: Context, private val showId: Long) :
  ListenableLiveData<Episode>(context) {

  private var notificationUri: Uri? = null

  init {
    addNotificationUri(Episodes.fromShow(showId))
  }

  override fun loadInBackground(): Episode? {
    val show = context.contentResolver.query(
      Shows.withId(showId),
      arrayOf(ShowColumns.WATCHING)
    )
    show.moveToFirst()
    val watching = Cursors.getBoolean(show, ShowColumns.WATCHING)
    show.close()

    var toWatch: Cursor? = null
    try {
      if (watching) {
        toWatch = context.contentResolver.query(
          Episodes.fromShow(showId),
          EpisodeMapper.projection,
          EpisodeColumns.WATCHING + "=1 OR " + EpisodeColumns.CHECKED_IN + "=1"
        )
      } else {
        val lastWatched = context.contentResolver.query(
          Episodes.fromShow(showId),
          EpisodeMapper.projection,
          EpisodeColumns.WATCHED + "=1",
          null,
          EpisodeColumns.SEASON + " DESC, " + EpisodeColumns.EPISODE + " DESC LIMIT 1"
        )!!

        var lastWatchedSeason: Long = 0
        var lastWatchedEpisode: Long = -1
        if (lastWatched.moveToFirst()) {
          lastWatchedSeason = Cursors.getInt(lastWatched, EpisodeColumns.SEASON).toLong()
          lastWatchedEpisode = Cursors.getInt(lastWatched, EpisodeColumns.EPISODE).toLong()
        }
        lastWatched.close()

        toWatch = context.contentResolver.query(
          Episodes.fromShow(showId),
          EpisodeMapper.projection,
          EpisodeColumns.WATCHED + "=0 AND " +
              EpisodeColumns.FIRST_AIRED + " IS NOT NULL AND " +
              EpisodeColumns.SEASON + ">0 AND (" +
              EpisodeColumns.SEASON + ">" + lastWatchedSeason + " OR (" +
              EpisodeColumns.SEASON + "=" + lastWatchedSeason + " AND " +
              EpisodeColumns.EPISODE + ">" + lastWatchedEpisode + "))",
          null,
          EpisodeColumns.SEASON + " ASC, " + EpisodeColumns.EPISODE + " ASC LIMIT 1"
        )
      }

      if (toWatch!!.moveToFirst()) {
        if (notificationUri != null) {
          removeNotificationUri(notificationUri)
        }
        notificationUri = toWatch.notificationUri
        addNotificationUri(notificationUri)
        return EpisodeMapper.mapEpisode(toWatch)
      } else {
        removeNotificationUri(notificationUri)
        notificationUri = null
        return null
      }
    } finally {
      toWatch?.close()
    }
  }
}
