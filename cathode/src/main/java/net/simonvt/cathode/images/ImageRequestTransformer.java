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
import com.squareup.picasso.Picasso;
import com.squareup.picasso.Request;
import java.util.ArrayList;
import java.util.List;

public class ImageRequestTransformer implements Picasso.RequestTransformer {

  public static final String QUERY_SIZE = "size";

  private static final List<String> SCHEMES = new ArrayList<>();

  static {
    SCHEMES.add(ImageUri.ITEM_SHOW);
    SCHEMES.add(ImageUri.ITEM_EPISODE);
    SCHEMES.add(ImageUri.ITEM_MOVIE);
    SCHEMES.add(ImageUri.ITEM_PERSON);
    SCHEMES.add(ImageUri.ITEM_IMAGE);
  }

  private Context context;

  public ImageRequestTransformer(Context context) {
    this.context = context;
  }

  @Override public Request transformRequest(Request request) {
    Uri uri = request.uri;

    if (uri == null) {
      return request;
    }

    final String scheme = uri.getScheme();

    if (!SCHEMES.contains(scheme)) {
      return request;
    }

    final ImageType imageType = ImageType.fromValue(uri.getHost());
    final String size = ImageSizeSelector.getInstance(context)
        .getSize(imageType, request.targetWidth, request.targetHeight);

    Uri newUri = uri.buildUpon().appendQueryParameter(QUERY_SIZE, size).build();

    return request.buildUpon().setUri(newUri).build();
  }
}
