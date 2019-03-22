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
import net.simonvt.cathode.account.AuthenticatorService;
import net.simonvt.cathode.appwidget.UpcomingWidgetService;
import net.simonvt.cathode.calendar.CalendarService;
import net.simonvt.cathode.dashclock.DashClockService;
import net.simonvt.cathode.notification.NotificationActionService;

@Module public abstract class ServiceBindingModule {

  @ContributesAndroidInjector abstract NotificationActionService notificationActionService();

  @ContributesAndroidInjector abstract DashClockService dashClockService();

  @ContributesAndroidInjector abstract AuthenticatorService authenticatorService();

  @ContributesAndroidInjector abstract CalendarService calendarService();

  @ContributesAndroidInjector abstract UpcomingWidgetService upcomingWidgetService();
}
