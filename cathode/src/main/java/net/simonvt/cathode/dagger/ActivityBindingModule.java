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

package net.simonvt.cathode.dagger;

import dagger.Module;
import dagger.android.ContributesAndroidInjector;
import net.simonvt.cathode.settings.NotificationSettingsActivity;
import net.simonvt.cathode.settings.SettingsActivity;
import net.simonvt.cathode.settings.hidden.HiddenItems;
import net.simonvt.cathode.settings.login.LoginActivity;
import net.simonvt.cathode.settings.login.OauthWebViewActivity;
import net.simonvt.cathode.settings.login.TokenActivity;
import net.simonvt.cathode.settings.setup.CalendarSetupActivity;
import net.simonvt.cathode.settings.setup.NotificationSetupActivity;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.ui.HomeActivity;

@Module public abstract class ActivityBindingModule {

  @ContributesAndroidInjector abstract HomeActivity homeActivity();

  @ContributesAndroidInjector abstract LoginActivity loginActivity();

  @ContributesAndroidInjector abstract EpisodeDetailsActivity episodeDetailsActivity();

  @ContributesAndroidInjector abstract OauthWebViewActivity oauthWebViewActivity();

  @ContributesAndroidInjector abstract TokenActivity tokenActivity();

  @ContributesAndroidInjector abstract CalendarSetupActivity calendarSetupActivity();

  @ContributesAndroidInjector abstract NotificationSetupActivity notificationSetupActivity();

  @ContributesAndroidInjector abstract SettingsActivity settingsActivity();

  @ContributesAndroidInjector abstract NotificationSettingsActivity notificationSettingsActivity();

  @ContributesAndroidInjector abstract HiddenItems hiddenItems();
}
