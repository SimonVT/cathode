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
import net.simonvt.cathode.settings.link.TraktLinkActivity;
import net.simonvt.cathode.settings.link.TraktLinkSyncActivity;
import net.simonvt.cathode.settings.login.LoginActivity;
import net.simonvt.cathode.settings.login.OauthWebViewActivity;
import net.simonvt.cathode.settings.login.TokenActivity;
import net.simonvt.cathode.ui.CalendarEntryActivity;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.ui.HomeActivity;
import net.simonvt.cathode.ui.SeasonDetailsActivity;

@Module public abstract class ActivityBindingModule {

  @ContributesAndroidInjector abstract HomeActivity homeActivity();

  @ContributesAndroidInjector abstract LoginActivity loginActivity();

  @ContributesAndroidInjector abstract TraktLinkActivity traktLinkActivity();

  @ContributesAndroidInjector abstract TraktLinkSyncActivity traktLinkSyncActivity();

  @ContributesAndroidInjector abstract CalendarEntryActivity calendarEntryActivity();

  @ContributesAndroidInjector abstract EpisodeDetailsActivity episodeDetailsActivity();

  @ContributesAndroidInjector abstract SeasonDetailsActivity seasonDetailsActivity();

  @ContributesAndroidInjector abstract OauthWebViewActivity oauthWebViewActivity();

  @ContributesAndroidInjector abstract TokenActivity tokenActivity();

  @ContributesAndroidInjector abstract SettingsActivity settingsActivity();

  @ContributesAndroidInjector abstract NotificationSettingsActivity notificationSettingsActivity();

  @ContributesAndroidInjector abstract HiddenItems hiddenItems();
}
