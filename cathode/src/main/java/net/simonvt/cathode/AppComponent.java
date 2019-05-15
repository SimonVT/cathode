/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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

package net.simonvt.cathode;

import dagger.BindsInstance;
import dagger.Component;
import dagger.android.AndroidInjectionModule;
import javax.inject.Singleton;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.common.dagger.CathodeAndroidInjectionModule;
import net.simonvt.cathode.dagger.AppAssistedModule;
import net.simonvt.cathode.dagger.ServiceBindingModule;
import net.simonvt.cathode.dagger.views.ViewModule;
import net.simonvt.cathode.images.ImageModule;
import net.simonvt.cathode.jobqueue.JobInjectionModule;
import net.simonvt.cathode.remote.JobModule;
import net.simonvt.cathode.sync.api.ApiModule;
import net.simonvt.cathode.sync.tmdb.TmdbModule;
import net.simonvt.cathode.ui.di.FragmentModule;
import net.simonvt.cathode.ui.di.ViewModelModule;
import net.simonvt.cathode.work.di.WorkerModule;

@Singleton @Component(modules = {
    ServiceBindingModule.class, AndroidInjectionModule.class, CathodeAndroidInjectionModule.class,
    AppModule.class, ApiModule.class, TraktModule.class, TmdbModule.class, ImageModule.class,
    ProviderModule.class, ViewModule.class, JobModule.class, JobInjectionModule.class,
    WorkerModule.class, AppAssistedModule.class, ViewModelModule.class, FragmentModule.class,
}) //
public interface AppComponent {

  CathodeComponent plusCathodeComponent();

  interface Builder {

    AppComponent build();

    @BindsInstance Builder application(CathodeApp app);
  }
}
