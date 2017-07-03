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
import android.content.SharedPreferences;
import android.net.Uri;
import android.preference.PreferenceManager;
import com.squareup.picasso.Request;
import com.squareup.picasso.RequestHandler;
import com.uwetrottmann.tmdb2.entities.Configuration;
import com.uwetrottmann.tmdb2.services.ConfigurationService;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.Injector;
import net.simonvt.cathode.common.tmdb.TmdbRateLimiter;
import net.simonvt.cathode.settings.Settings;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

import static net.simonvt.cathode.images.ImageRequestTransformer.QUERY_SIZE;

public abstract class BaseUrlRequestHandler extends RequestHandler {

  @Inject ConfigurationService configurationService;

  protected final Context context;

  protected final SharedPreferences settings;

  private static final Object LOCK_BASE_URL = new Object();

  private volatile String secureBaseUrl;

  public BaseUrlRequestHandler(Context context) {
    this.context = context;

    Injector.obtain().inject(this);

    settings = PreferenceManager.getDefaultSharedPreferences(context);
  }

  String getBaseUrl() throws IOException {
    if (secureBaseUrl == null) {
      synchronized (LOCK_BASE_URL) {
        if (secureBaseUrl == null) {
          String s = settings.getString(Settings.TMDB_IMAGES_SECURE_BASE_URL, null);
          if (s == null) {
            TmdbRateLimiter.acquire();
            Call<Configuration> call = configurationService.configuration();
            Response<Configuration> response = call.execute();
            if (response.isSuccessful()) {
              Configuration configuration = response.body();
              secureBaseUrl = configuration.images.secure_base_url;

              ImageSettings.updateTmdbConfiguration(context, configuration);
            }
          } else {
            secureBaseUrl = s;
          }
        }
      }
    }

    return secureBaseUrl;
  }

  protected String transform(Request request, Uri uri) throws IOException {
    final String image = uri.getPath();
    String size = request.uri.getQueryParameter(QUERY_SIZE);

    String url = getBaseUrl() + size + image;
    Timber.d("Url: %s", url);
    return url;
  }
}
