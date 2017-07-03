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
import com.squareup.picasso.Downloader;
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import java.io.IOException;
import java.io.InputStream;

public class ImageRequestHandler extends BaseUrlRequestHandler {

  private final Downloader downloader;

  public ImageRequestHandler(Context context, Downloader downloader) {
    super(context);
    this.downloader = downloader;
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_IMAGE.equals(data.uri.getScheme());
  }

  @Override public RequestHandler.Result load(Request request, int networkPolicy)
      throws IOException {
    final String baseUrl = getBaseUrl();
    if (TextUtils.isEmpty(baseUrl)) {
      return null;
    }

    String path = transform(request, request.uri);

    if (TextUtils.isEmpty(path)) {
      return null;
    }

    Downloader.Response response = downloader.load(Uri.parse(path), networkPolicy);
    if (response == null) {
      return null;
    }

    InputStream is = response.getInputStream();
    if (is == null) {
      return null;
    }

    return new RequestHandler.Result(is, Picasso.LoadedFrom.NETWORK);
  }
}
