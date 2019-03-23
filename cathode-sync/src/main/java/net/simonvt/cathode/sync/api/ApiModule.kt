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

package net.simonvt.cathode.sync.api

import android.content.Context
import dagger.Module
import dagger.Provides
import net.simonvt.cathode.api.TraktModule.Companion.NAMED_TRAKT
import net.simonvt.cathode.api.TraktSettings
import net.simonvt.cathode.sync.BuildConfig
import net.simonvt.cathode.sync.tmdb.TmdbModule.NAMED_TMDB_API_KEY
import okhttp3.Interceptor
import javax.inject.Named

@Module
class ApiModule {

  @Provides
  fun bindTraktSettings(context: Context): TraktSettings {
    return ApiSettings(context)
  }

  @Provides
  @Named(NAMED_TRAKT)
  fun provideInterceptors(): List<Interceptor> {
    val interceptors = mutableListOf<Interceptor>()
    interceptors.add(LoggingInterceptor())
    return interceptors
  }

  @Provides
  @Named(NAMED_TMDB_API_KEY)
  fun tmdbApiKey(): String {
    return BuildConfig.TMDB_API_KEY
  }
}
