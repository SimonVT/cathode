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

package net.simonvt.cathode.images;

import android.content.Context;
import android.net.Uri;
import android.os.StatFs;
import com.jakewharton.picasso.OkHttp3Downloader;
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import com.uwetrottmann.tmdb2.services.MoviesService;
import com.uwetrottmann.tmdb2.services.PeopleService;
import com.uwetrottmann.tmdb2.services.TvEpisodesService;
import com.uwetrottmann.tmdb2.services.TvSeasonsService;
import com.uwetrottmann.tmdb2.services.TvShowService;
import dagger.Module;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import net.simonvt.cathode.provider.helper.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.helper.MovieDatabaseHelper;
import net.simonvt.cathode.provider.helper.PersonDatabaseHelper;
import net.simonvt.cathode.provider.helper.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.helper.ShowDatabaseHelper;
import okhttp3.OkHttpClient;
import timber.log.Timber;

@Module(complete = false, library = true) public class ImageModule {

  private static final String PICASSO_CACHE = "picasso-cache";
  private static final int MIN_DISK_CACHE_SIZE = 5 * 1024 * 1024; // 5MB
  private static final int MAX_DISK_CACHE_SIZE = 50 * 1024 * 1024; // 50MB

  private static File createCacheDir(Context context) {
    File cache = new File(context.getApplicationContext().getCacheDir(), PICASSO_CACHE);
    if (!cache.exists()) {
      //noinspection ResultOfMethodCallIgnored
      cache.mkdirs();
    }
    return cache;
  }

  private static long calculateDiskCacheSize(File dir) {
    long size = MIN_DISK_CACHE_SIZE;

    try {
      StatFs statFs = new StatFs(dir.getAbsolutePath());
      long available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
      // Target 2% of the total space.
      size = available / 50;
    } catch (IllegalArgumentException ignored) {
    }

    // Bound inside min/max size for disk cache.
    return Math.max(Math.min(size, MAX_DISK_CACHE_SIZE), MIN_DISK_CACHE_SIZE);
  }

  private static OkHttpClient defaultOkHttpClient(File cacheDir, long maxSize) {
    return new OkHttpClient.Builder().cache(new okhttp3.Cache(cacheDir, maxSize)).build();
  }

  @Provides @Singleton Picasso providePicasso(Context context, Downloader downloader,
      ImageRequestHandler imageRequestHandler, ShowRequestHandler showRequestHandler,
      SeasonRequestHandler seasonRequestHandler, EpisodeRequestHandler episodeRequestHandler,
      MovieRequestHandler movieRequestHandler, PersonRequestHandler personRequestHandler) {
    Picasso.Builder builder =
        new Picasso.Builder(context).requestTransformer(new ImageRequestTransformer(context))
            .addRequestHandler(imageRequestHandler)
            .addRequestHandler(showRequestHandler)
            .addRequestHandler(seasonRequestHandler)
            .addRequestHandler(episodeRequestHandler)
            .addRequestHandler(movieRequestHandler)
            .addRequestHandler(personRequestHandler)
            .downloader(downloader);

    if (BuildConfig.DEBUG) {
      builder.listener(new Picasso.Listener() {
        @Override public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
          Timber.d(exception);
        }
      });
    }

    return builder.build();
  }

  @Provides @Singleton @Images OkHttpClient provideOkHttpClient(Context context) {
    final File cacheDir = createCacheDir(context);
    final long cacheSize = calculateDiskCacheSize(cacheDir);
    return defaultOkHttpClient(cacheDir, cacheSize);
  }

  @Provides @Singleton Downloader provideDownloader(@Images OkHttpClient okClient) {
    return new OkHttp3Downloader(okClient);
  }

  @Provides @Singleton ImageDownloader provideImageDownloader(@Images OkHttpClient okClient) {
    return new ImageDownloader(okClient);
  }

  @Provides @Singleton ImageRequestHandler provideImageRequestHandler(Context context,
      ConfigurationService configurationService, Downloader downloader) {
    return new ImageRequestHandler(context, configurationService, downloader);
  }

  @Provides @Singleton ShowRequestHandler provideShowRequestHandler(Context context,
      ConfigurationService configurationService, ImageDownloader downloader,
      TvShowService tvShowService, ShowDatabaseHelper showHelper) {
    return new ShowRequestHandler(context, configurationService, downloader, tvShowService,
        showHelper);
  }

  @Provides @Singleton SeasonRequestHandler provideSeasonRequestHandler(Context context,
      ConfigurationService configurationService, ImageDownloader downloader,
      TvSeasonsService tvSeasonService, ShowDatabaseHelper showHelper,
      SeasonDatabaseHelper seasonHelper) {
    return new SeasonRequestHandler(context, configurationService, downloader, tvSeasonService,
        showHelper, seasonHelper);
  }

  @Provides @Singleton EpisodeRequestHandler provideEpisodeRequestHandler(Context context,
      ConfigurationService configurationService, ImageDownloader downloader,
      TvEpisodesService tvEpisodeService, ShowDatabaseHelper showHelper,
      EpisodeDatabaseHelper episodeHelper) {
    return new EpisodeRequestHandler(context, configurationService, downloader, tvEpisodeService,
        showHelper, episodeHelper);
  }

  @Provides @Singleton MovieRequestHandler provideMovieRequestHandler(Context context,
      ConfigurationService configurationService, ImageDownloader downloader,
      MoviesService moviesService, MovieDatabaseHelper movieHelper) {
    return new MovieRequestHandler(context, configurationService, downloader, moviesService,
        movieHelper);
  }

  @Provides @Singleton PersonRequestHandler providePersonRequestHandler(Context context,
      ConfigurationService configurationService, ImageDownloader downloader,
      PeopleService peopleService, PersonDatabaseHelper personHelper) {
    return new PersonRequestHandler(context, configurationService, downloader, peopleService,
        personHelper);
  }
}
