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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import java.io.IOException;

import static com.squareup.picasso.Picasso.LoadedFrom.DISK;
import static com.squareup.picasso.Picasso.LoadedFrom.NETWORK;

public abstract class ItemRequestHandler extends BaseUrlRequestHandler {

  private final ImageDownloader downloader;

  public ItemRequestHandler(Context context, ImageDownloader downloader) {
    super(context);
    this.downloader = downloader;
  }

  protected abstract int getTmdbId(long id);

  protected abstract String getCachedPath(ImageType imageType, long id);

  protected abstract void clearCachedPaths(long id);

  protected abstract String queryPath(ImageType imageType, long id, int tmdbId) throws IOException;

  @Override public Result load(Request request, int networkPolicy) throws IOException {
    final String baseUrl = getBaseUrl();
    if (TextUtils.isEmpty(baseUrl)) {
      return null;
    }

    Uri uri = request.uri;

    final ImageType imageType = ImageType.fromValue(uri.getHost());
    final long id = Long.valueOf(uri.getPathSegments().get(0));
    final int tmdbId = getTmdbId(id);

    boolean wasCached = false;
    String cachedPath = getCachedPath(imageType, id);

    String path;
    if (cachedPath != null) {
      path = cachedPath;
      wasCached = true;
    } else {
      path = queryPath(imageType, id, tmdbId);
    }

    if (TextUtils.isEmpty(path)) {
      return null;
    }

    path = transform(request, Uri.parse(path));

    okhttp3.Response response = downloader.load(Uri.parse(path), networkPolicy);

    if (response == null) {
      return null;
    }

    if (wasCached && response.code() == 404) {
      clearCachedPaths(id);
      path = queryPath(imageType, id, tmdbId);
      path = transform(request, Uri.parse(path));

      response = downloader.load(Uri.parse(path), networkPolicy);
    }

    if (response.isSuccessful()) {
      boolean fromCache = response.cacheResponse() != null;

      Picasso.LoadedFrom loadedFrom = fromCache ? DISK : NETWORK;

      return new Result(response.body().byteStream(), loadedFrom);
    }

    return null;
  }
}
