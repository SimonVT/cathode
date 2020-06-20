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
package net.simonvt.cathode.ui

import android.annotation.SuppressLint
import android.content.ContentValues
import android.content.Intent
import android.os.Bundle
import android.text.format.DateUtils
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.AdapterView
import androidx.work.WorkManager
import dagger.android.AndroidInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.common.event.AuthFailedEvent
import net.simonvt.cathode.common.event.RequestFailedEvent
import net.simonvt.cathode.common.widget.PaletteTransformation
import net.simonvt.cathode.databinding.DebugDrawerBinding
import net.simonvt.cathode.databinding.DebugHomeBinding
import net.simonvt.cathode.notification.NotificationService
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseContract.SeasonColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.provider.ProviderSchematic.Seasons
import net.simonvt.cathode.provider.ProviderSchematic.Shows
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.settings.StartPage
import net.simonvt.cathode.settings.TraktLinkSettings
import net.simonvt.cathode.work.enqueueUniqueNow
import net.simonvt.cathode.work.movies.MarkSyncUserMoviesWorker
import net.simonvt.cathode.work.movies.SyncUpdatedMoviesWorker
import net.simonvt.cathode.work.shows.MarkSyncUserShowsWorker
import net.simonvt.cathode.work.shows.SyncUpdatedShowsWorker
import okhttp3.logging.HttpLoggingInterceptor
import javax.inject.Inject

@SuppressLint("SetTextI18n")
abstract class BaseActivity : CathodeActivity() {

  // TODO:
  //  @Inject
  //  @Named(NAMED_STATUS_CODE)
  //  lateinit var httpStatusCodePreference: IntPreference
  @Inject
  lateinit var debugWorkManager: WorkManager

  @Inject
  lateinit var debugLoggingInterceptor: HttpLoggingInterceptor

  @Inject
  lateinit var debugShowHelper: ShowDatabaseHelper

  private lateinit var binding: DebugHomeBinding
  private lateinit var drawerBinding: DebugDrawerBinding

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidInjection.inject(this)

    binding = DebugHomeBinding.inflate(layoutInflater)
    super.setContentView(binding.root)

    val drawerContext = ContextThemeWrapper(this, R.style.Theme_AppCompat)
    drawerBinding = DebugDrawerBinding.inflate(LayoutInflater.from(drawerContext))
    binding.debugDrawer.addView(drawerBinding.root)

    val startPageAdapter = StartPageAdapter()
    drawerBinding.debugStartPage.adapter = startPageAdapter
    drawerBinding.debugStartPage.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
          val startPage = startPageAdapter.getItem(position)
          Settings.get(this@BaseActivity)
            .edit()
            .putString(Settings.START_PAGE, startPage.toString())
            .apply()
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
      }
    val startPage = StartPage.fromValue(
      Settings.get(this).getString(Settings.START_PAGE, null),
      StartPage.DASHBOARD
    )
    drawerBinding.debugStartPage.setSelection(startPageAdapter.getPositionForValue(startPage))

    drawerBinding.debugRecreateActivity.setOnClickListener { recreate() }

    drawerBinding.debugUpdateNotifications.setOnClickListener {
      val i = Intent(this@BaseActivity, NotificationService::class.java)
      startService(i)
    }

    drawerBinding.debugPlayImages.setOnCheckedChangeListener { buttonView, isChecked ->
      PaletteTransformation.shouldTransform = isChecked
    }

    drawerBinding.debugRequestFailedEvent.setOnClickListener {
      RequestFailedEvent.post(R.string.error_unknown_retrying)
      binding.debugDrawerLayout.closeDrawers()
    }

    drawerBinding.debugAuthFailedEvent.setOnClickListener {
      AuthFailedEvent.post()
      binding.debugDrawerLayout.closeDrawers()
    }

    drawerBinding.debugRemoveAccessToken.setOnClickListener {
      Settings.get(this@BaseActivity).edit().remove(TraktLinkSettings.TRAKT_ACCESS_TOKEN).apply()
    }

    drawerBinding.debugRemoveRefreshToken.setOnClickListener {
      Settings.get(this@BaseActivity)
        .edit()
        .remove(TraktLinkSettings.TRAKT_REFRESH_TOKEN)
        .apply()
    }

    drawerBinding.debugInvalidateAccessToken.setOnClickListener {
      Settings.get(this@BaseActivity)
        .edit()
        .putString(TraktLinkSettings.TRAKT_ACCESS_TOKEN, "invalid token")
        .putLong(TraktLinkSettings.TRAKT_TOKEN_EXPIRATION, 0L)
        .apply()
    }

    drawerBinding.debugInvalidateRefreshToken.setOnClickListener {
      Settings.get(this@BaseActivity)
        .edit()
        .putString(TraktLinkSettings.TRAKT_REFRESH_TOKEN, "invalid token")
        .apply()
    }

    drawerBinding.debugInsertFakeShow.setOnClickListener {
      Thread(Runnable {
        var showId = debugShowHelper.getId(FAKE_SHOW_ID)
        if (showId > -1L) {
          contentResolver.delete(Shows.withId(showId), null, null)
        }

        var values = ContentValues()
        values.put(ShowColumns.TITLE, "Fake show")
        values.put(ShowColumns.TRAKT_ID, FAKE_SHOW_ID)
        values.put(ShowColumns.RUNTIME, 1)
        val showUri = contentResolver.insert(Shows.SHOWS, values)
        showId = Shows.getShowId(showUri!!)

        values = ContentValues()
        values.put(SeasonColumns.SHOW_ID, showId)
        values.put(SeasonColumns.SEASON, 1)
        val seasonUri = contentResolver.insert(Seasons.SEASONS, values)
        val seasonId = Seasons.getId(seasonUri!!)

        var time = System.currentTimeMillis() - DateUtils.MINUTE_IN_MILLIS

        for (i in 1..10) {
          values = ContentValues()
          values.put(EpisodeColumns.SHOW_ID, showId)
          values.put(EpisodeColumns.SEASON_ID, seasonId)
          values.put(EpisodeColumns.SEASON, 1)
          values.put(EpisodeColumns.EPISODE, i)
          values.put(EpisodeColumns.FIRST_AIRED, time)
          values.put(EpisodeColumns.WATCHED, i == 1)
          contentResolver.insert(Episodes.EPISODES, values)

          time += DateUtils.MINUTE_IN_MILLIS
        }
      }).start()
    }

    drawerBinding.debugRemoveFakeShow.setOnClickListener {
      val showId = debugShowHelper.getId(FAKE_SHOW_ID)
      if (showId > -1L) {
        contentResolver.delete(Shows.withId(showId), null, null)
      }
    }

    val logLevelAdapter = EnumAdapter(HttpLoggingInterceptor.Level.values())
    drawerBinding.debugLogLevel.adapter = logLevelAdapter
    drawerBinding.debugLogLevel.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(
          adapterView: AdapterView<*>,
          view: View?,
          position: Int,
          id: Long
        ) {
          val logLevel = logLevelAdapter.getItem(position)
          debugLoggingInterceptor.level = logLevel
        }

        override fun onNothingSelected(adapterView: AdapterView<*>) {}
      }
    drawerBinding.debugLogLevel.setSelection(
      logLevelAdapter.getPositionForValue(debugLoggingInterceptor.level)
    )

    val statusCodes = intArrayOf(200, 401, 404, 409, 412, 502)
    val httpStatusCodeAdapter = IntAdapter(statusCodes)
    drawerBinding.debugNetworkStatusCode.adapter = httpStatusCodeAdapter
    drawerBinding.debugNetworkStatusCode.onItemSelectedListener =
      object : AdapterView.OnItemSelectedListener {
        override fun onItemSelected(parent: AdapterView<*>, view: View?, position: Int, id: Long) {
          // TODO: httpStatusCodePreference.set(httpStatusCodeAdapter.getItem(position)!!)
        }

        override fun onNothingSelected(parent: AdapterView<*>) {}
      }
    // TODO: debugViews.httpStatusCode.setSelection(httpStatusCodeAdapter.getPositionForValue(httpStatusCodePreference.get()))

    drawerBinding.debugUpdated.setOnClickListener {
      debugWorkManager.enqueueUniqueNow(
        SyncUpdatedShowsWorker.TAG,
        SyncUpdatedShowsWorker::class.java
      )
      debugWorkManager.enqueueUniqueNow(  
        MarkSyncUserShowsWorker.TAG,
        MarkSyncUserShowsWorker::class.java
      )
      debugWorkManager.enqueueUniqueNow(
        SyncUpdatedMoviesWorker.TAG,
        SyncUpdatedMoviesWorker::class.java
      )
      debugWorkManager.enqueueUniqueNow(
        MarkSyncUserMoviesWorker.TAG,
        MarkSyncUserMoviesWorker::class.java
      )
    }
  }

  override fun setContentView(layoutResID: Int) {
    binding.debugContent.removeAllViews()
    layoutInflater.inflate(layoutResID, binding.debugContent)
  }

  companion object {

    private const val FAKE_SHOW_ID = java.lang.Long.MAX_VALUE
  }
}
