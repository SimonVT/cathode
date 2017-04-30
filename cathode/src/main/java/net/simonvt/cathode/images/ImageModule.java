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
import dagger.Module;
import dagger.Provides;
import java.io.File;
import javax.inject.Singleton;
import net.simonvt.cathode.BuildConfig;
import okhttp3.OkHttpClient;

@Module(
    complete = false,
    library = true)
public class ImageModule {

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

  @Provides @Singleton Picasso providePicasso(Context context) {
    final File cacheDir = createCacheDir(context);
    final long cacheSize = calculateDiskCacheSize(cacheDir);
    OkHttpClient okClient = defaultOkHttpClient(cacheDir, cacheSize);

    Downloader downloader = new OkHttp3Downloader(okClient);
    ImageDownloader imageDownloader = new ImageDownloader(okClient);

    Picasso.Builder builder =
        new Picasso.Builder(context).requestTransformer(new ImageRequestTransformer(context))
            .addRequestHandler(new ImageRequestHandler(context, downloader))
            .addRequestHandler(new ShowRequestHandler(context, imageDownloader))
            .addRequestHandler(new SeasonRequestHandler(context, imageDownloader))
            .addRequestHandler(new EpisodeRequestHandler(context, imageDownloader))
            .addRequestHandler(new MovieRequestHandler(context, imageDownloader))
            .addRequestHandler(new PersonRequestHandler(context, imageDownloader))
            .downloader(downloader);

    if (BuildConfig.DEBUG) {
      builder.listener(new Picasso.Listener() {
        @Override public void onImageLoadFailed(Picasso picasso, Uri uri, Exception exception) {
          exception.printStackTrace();
        }
      });
    }

    return builder.build();
  }
}
