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
import android.text.TextUtils;
import android.text.format.DateUtils;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.entities.Image;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;
import static com.squareup.picasso.Picasso.LoadedFrom.NETWORK;

public abstract class ItemRequestHandler extends BaseUrlRequestHandler {

  private ImageDownloader downloader;

  public ItemRequestHandler(Context context, ConfigurationService configurationService,
      ImageDownloader downloader) {
    super(context, configurationService);
    this.downloader = downloader;
  }

  protected abstract int getTmdbId(long id);

  public abstract long getLastCacheUpdate(long id);

  protected abstract String getCachedPath(ImageType imageType, long id);

  protected abstract void clearCachedPaths(long id);

  protected abstract boolean updateCache(ImageType imageType, long id, int tmdbId)
      throws IOException;

  private boolean needsUpdate(long lastUpdated) {
    return lastUpdated + DateUtils.WEEK_IN_MILLIS < System.currentTimeMillis();
  }

  @Override public Result load(Request request, int networkPolicy) throws IOException {
    final String baseUrl = getBaseUrl();
    if (TextUtils.isEmpty(baseUrl)) {
      return null;
    }

    Uri uri = request.uri;

    final ImageType imageType = ImageType.fromValue(uri.getHost());
    final long id = Long.valueOf(uri.getPathSegments().get(0));
    final int tmdbId = getTmdbId(id);

    final long lastUpdated = getLastCacheUpdate(id);
    final boolean needsUpdate = needsUpdate(lastUpdated);

    String path;
    if (needsUpdate) {
      final boolean success = updateCache(imageType, id, tmdbId);
      if (!success) {
        return null;
      }
    }

    path = getCachedPath(imageType, id);
    if (TextUtils.isEmpty(path)) {
      return null;
    }

    path = transform(request, Uri.parse(path));
    okhttp3.Response response = downloader.load(Uri.parse(path), networkPolicy);
    if (response == null) {
      return null;
    }

    if (!needsUpdate && response.code() == 404) {
      final boolean success = updateCache(imageType, id, tmdbId);
      if (!success) {
        clearCachedPaths(id);
        return null;
      }
      path = getCachedPath(imageType, id);
      path = transform(request, Uri.parse(path));
      response = downloader.load(Uri.parse(path), networkPolicy);
    }

    if (response.isSuccessful()) {
      final boolean fromCache = response.cacheResponse() != null;
      final Picasso.LoadedFrom loadedFrom = fromCache ? DISK : NETWORK;
      return new Result(response.body().byteStream(), loadedFrom);
    }

    return null;
  }

  public static Image selectBest(List<Image> imageList) {
    if (imageList.size() == 0) {
      return null;
    }

    List<Image> images = new ArrayList<>(imageList);
    Collections.sort(images, new Comparator<Image>() {
      @Override public int compare(Image image1, Image image2) {
        return image1.vote_average.compareTo(image2.vote_average);
      }
    });

    for (Image image : images) {
      if (image.vote_count > 10) {
        return image;
      }
    }

    return images.get(0);
  }
}
