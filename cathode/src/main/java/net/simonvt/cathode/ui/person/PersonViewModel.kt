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

package net.simonvt.cathode.ui.person

import android.content.Context
import android.text.format.DateUtils
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.launch
import net.simonvt.cathode.actions.ActionManager
import net.simonvt.cathode.actions.people.SyncPerson
import net.simonvt.cathode.actions.people.SyncPersonBackdrop
import net.simonvt.cathode.actions.people.SyncPersonHeadshot
import net.simonvt.cathode.actions.people.SyncPersonMovieCredits
import net.simonvt.cathode.actions.people.SyncPersonShowCredits
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper
import net.simonvt.cathode.ui.RefreshableViewModel
import javax.inject.Inject

class PersonViewModel @Inject constructor(
  private val context: Context,
  private val personHelper: PersonDatabaseHelper,
  private val syncPerson: SyncPerson,
  private val syncPersonShowCredits: SyncPersonShowCredits,
  private val syncPersonMovieCredits: SyncPersonMovieCredits,
  private val syncPersonHeadshot: SyncPersonHeadshot,
  private val syncPersonBackdrop: SyncPersonBackdrop
) : RefreshableViewModel() {

  private var personId = -1L

  lateinit var person: LiveData<Person>
    private set

  fun setPersonId(personId: Long) {
    if (this.personId == -1L) {
      this.personId = personId
      person = PersonLiveData(context, personId)

      person.observeForever(observer)
    }
  }

  override fun onCleared() {
    person.removeObserver(observer)
    super.onCleared()
  }

  private val observer = Observer<Person> { person ->
    viewModelScope.launch {
      val currentTime = System.currentTimeMillis()
      if (currentTime > person.lastSync + DateUtils.DAY_IN_MILLIS) {
        val traktId = person.traktId
        val tmdbId = person.tmdbId

        ActionManager.invokeAsync(
          SyncPerson.key(traktId),
          syncPerson,
          SyncPerson.Params(traktId)
        )
        ActionManager.invokeAsync(
          SyncPersonShowCredits.key(traktId),
          syncPersonShowCredits,
          SyncPersonShowCredits.Params(traktId)
        )
        ActionManager.invokeAsync(
          SyncPersonMovieCredits.key(traktId),
          syncPersonMovieCredits,
          SyncPersonMovieCredits.Params(traktId)
        )
        ActionManager.invokeAsync(
          SyncPersonHeadshot.key(tmdbId),
          syncPersonHeadshot,
          SyncPersonHeadshot.Params(tmdbId)
        )
        ActionManager.invokeAsync(
          SyncPersonBackdrop.key(tmdbId),
          syncPersonBackdrop,
          SyncPersonBackdrop.Params(tmdbId)
        )
      }
    }
  }

  override suspend fun onRefresh() {
    val traktId = personHelper.getTraktId(personId)
    val tmdbId = personHelper.getTmdbId(personId)

    val personDeferred = ActionManager.invokeAsync(
      SyncPerson.key(traktId),
      syncPerson,
      SyncPerson.Params(traktId)
    )
    val showCreditsDeferred = ActionManager.invokeAsync(
      SyncPersonShowCredits.key(traktId),
      syncPersonShowCredits,
      SyncPersonShowCredits.Params(traktId)
    )
    val movieCreditsDeferred = ActionManager.invokeAsync(
      SyncPersonMovieCredits.key(traktId),
      syncPersonMovieCredits,
      SyncPersonMovieCredits.Params(traktId)
    )
    val headshotDeferred = ActionManager.invokeAsync(
      SyncPersonHeadshot.key(tmdbId),
      syncPersonHeadshot,
      SyncPersonHeadshot.Params(tmdbId)
    )
    val backdropDeferred = ActionManager.invokeAsync(
      SyncPersonBackdrop.key(tmdbId),
      syncPersonBackdrop,
      SyncPersonBackdrop.Params(tmdbId)
    )

    personDeferred.await()
    showCreditsDeferred.await()
    movieCreditsDeferred.await()
    headshotDeferred.await()
    backdropDeferred.await()
  }
}
