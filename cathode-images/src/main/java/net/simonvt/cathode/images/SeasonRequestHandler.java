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

package net.simonvt.cathode.images;

import android.content.Context;
import com.squareup.picasso.Request;
import com.uwetrottmann.tmdb2.services.TvService;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;

public class SeasonRequestHandler extends ItemRequestHandler {

  @Inject TvService tvService;

  @Inject ShowDatabaseHelper showHelper;
  @Inject SeasonDatabaseHelper seasonHelper;

  public SeasonRequestHandler(Context context, ImageDownloader downloader) {
    super(context, downloader);
  }

  @Override public boolean canHandleRequest(Request data) {
    return ImageUri.ITEM_SEASON.equals(data.uri.getScheme());
  }

  @Override protected int getTmdbId(long id) {
    throw new RuntimeException("Not yet implemented");
  }

  @Override protected String getCachedPath(ImageType imageType, long id) {
    throw new IllegalArgumentException("Unsupported image type: " + imageType.toString());
  }

  protected void clearCachedPaths(long id) {
  }

  @Override protected String queryPath(ImageType imageType, long id, int tmdbId)
      throws IOException {
    throw new IllegalArgumentException("Unsupported image type: " + imageType.toString());
  }
}
