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
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import javax.inject.Inject;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;

public class EpisodeHistoryViewModelFactory extends ViewModelProvider.NewInstanceFactory {

  private Application application;
  private SyncService syncService;
  private EpisodeDatabaseHelper episodeHelper;

  @Inject public EpisodeHistoryViewModelFactory(Application application, SyncService syncService,
      EpisodeDatabaseHelper episodeHelper) {
    this.application = application;
    this.syncService = syncService;
    this.episodeHelper = episodeHelper;
  }

  @NonNull @Override public <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
    if (modelClass.isAssignableFrom(EpisodeHistoryViewModel.class)) {
      return (T) new EpisodeHistoryViewModel(application, syncService, episodeHelper);
    }

    throw new RuntimeException("Unknown type " + modelClass.getCanonicalName());
  }
}
