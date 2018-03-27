/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.sync.tmdb;

import android.content.Context;
import com.uwetrottmann.tmdb2.services.CollectionsService;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import com.uwetrottmann.tmdb2.services.DiscoverService;
import com.uwetrottmann.tmdb2.services.FindService;
import com.uwetrottmann.tmdb2.services.GenresService;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.PeopleService;
import com.uwetrottmann.tmdb2.services.SearchService;
import com.uwetrottmann.tmdb2.services.TvEpisodesService;
import com.uwetrottmann.tmdb2.services.TvSeasonsService;
import com.uwetrottmann.tmdb2.services.TvService;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import java.util.concurrent.TimeUnit;
import javax.inject.Singleton;
import okhttp3.Cache;
import okhttp3.OkHttpClient;

@Module(library = true, complete = false) public class TmdbModule {

  @Provides @Singleton @Tmdb OkHttpClient.Builder okBuilder(Context context) {
    OkHttpClient.Builder builder = new OkHttpClient.Builder();
    builder.connectTimeout(15, TimeUnit.SECONDS);
    builder.readTimeout(20, TimeUnit.SECONDS);

    final File cacheDir = OkHttpUtils.getCacheDir(context);
    builder.cache(new Cache(cacheDir, OkHttpUtils.getCacheSize(cacheDir)));

    builder.networkInterceptors().add(new ImageLanguageInterceptor());

    return builder;
  }

  @Provides @Singleton TmdbSettings tmdbSettings(@TmdbApiKey String apiKey,
      @Tmdb OkHttpClient.Builder okBuilder) {
    return new TmdbSettings(apiKey, okBuilder);
  }

  @Provides @Singleton CollectionsService collectionService(TmdbSettings tmdb) {
    return tmdb.collectionService();
  }

  @Provides @Singleton ConfigurationService configurationService(TmdbSettings tmdb) {
    return tmdb.configurationService();
  }

  @Provides @Singleton DiscoverService discoverService(TmdbSettings tmdb) {
    return tmdb.discoverService();
  }

  @Provides @Singleton FindService findService(TmdbSettings tmdb) {
    return tmdb.findService();
  }

  @Provides @Singleton GenresService genreService(TmdbSettings tmdb) {
    return tmdb.genreService();
  }

  @Provides @Singleton MoviesService moviesService(TmdbSettings tmdb) {
    return tmdb.moviesService();
  }

  @Provides @Singleton PeopleService peopleService(TmdbSettings tmdb) {
    return tmdb.personService();
  }

  @Provides @Singleton SearchService searchService(TmdbSettings tmdb) {
    return tmdb.searchService();
  }

  @Provides @Singleton TvService tvService(TmdbSettings tmdb) {
    return tmdb.tvService();
  }

  @Provides @Singleton TvSeasonsService tvSeasonsService(TmdbSettings tmdb) {
    return tmdb.tvSeasonsService();
  }

  @Provides @Singleton TvEpisodesService tvEpisodesService(TmdbSettings tmdb) {
    return tmdb.tvEpisodesService();
  }
}
