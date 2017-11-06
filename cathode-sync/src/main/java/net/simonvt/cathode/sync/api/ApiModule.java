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

package net.simonvt.cathode.sync.api;

import android.content.Context;
import dagger.Module;
import dagger.Provides;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Named;
import net.simonvt.cathode.api.TraktSettings;
import net.simonvt.cathode.sync.BuildConfig;
import okhttp3.Interceptor;

import static net.simonvt.cathode.api.TraktModule.NAMED_TRAKT;
import static net.simonvt.cathode.sync.tmdb.TmdbModule.NAMED_TMDB_API_KEY;

@Module public class ApiModule {

  @Provides TraktSettings bindTraktSettings(Context context) {
    return new ApiSettings(context);
  }

  @Provides @Named(NAMED_TRAKT) List<Interceptor> provideInterceptors() {
    List<Interceptor> interceptors = new ArrayList<>();
    interceptors.add(new LoggingInterceptor());
    return interceptors;
  }

  @Provides @Named(NAMED_TMDB_API_KEY) String tmdbApiKey() {
    return BuildConfig.TMDB_API_KEY;
  }
}
