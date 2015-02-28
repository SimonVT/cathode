/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.module;

import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import com.squareup.otto.Bus;
import dagger.Module;
import dagger.Provides;
import javax.inject.Singleton;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.ApiKey;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.api.UserToken;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.RequestFailedEvent;
import net.simonvt.cathode.settings.Settings;
import retrofit.ErrorHandler;
import retrofit.RetrofitError;
import retrofit.client.Response;
import timber.log.Timber;

@Module(
    complete = false,
    library = true,

    includes = {
        TraktModule.class
    })
public class ApiModule {

  private static final Handler MAIN_HANDLER = new Handler(Looper.getMainLooper());

  @Provides @ApiKey String provideClientId() {
    return BuildConfig.TRAKT_CLIENT_ID;
  }

  @Provides @Singleton ErrorHandler provideErrorHandler(final Bus bus) {
    return new ErrorHandler() {
      @Override public Throwable handleError(RetrofitError error) {
        switch (error.getKind()) {
          case HTTP:
            Response response = error.getResponse();
            if (response != null) {
              final int statusCode = response.getStatus();
              Timber.i("Status code: " + statusCode);

              if (statusCode == 401) {
                MAIN_HANDLER.post(new Runnable() {
                  @Override public void run() {
                    bus.post(new AuthFailedEvent());
                  }
                });
              } else if (statusCode == 404) {
                // Handled in JobService
                return error;
              } else if (statusCode >= 500 && statusCode < 600) {
                MAIN_HANDLER.post(new Runnable() {
                  @Override public void run() {
                    bus.post(new RequestFailedEvent(R.string.error_5xx_retrying));
                  }
                });
              } else {
                Timber.e(error, "Kind: HTTP");
                MAIN_HANDLER.post(new Runnable() {
                  @Override public void run() {
                    bus.post(new RequestFailedEvent(R.string.error_unknown_retrying));
                  }
                });
              }

              Timber.d(error, "Kind: HTTP");
            } else {
              Timber.e(error, "Kind: HTTP");
            }
            break;

          case CONVERSION:
            Timber.e(error, "Kind: CONVERSION");
            break;

          case UNEXPECTED:
            Timber.e(error, "Kind: UNEXPECTED");
            break;

          case NETWORK:
            Timber.d(error, "Kind: NETWORK");

            MAIN_HANDLER.post(new Runnable() {
              @Override public void run() {
                bus.post(new RequestFailedEvent(R.string.error_network_retrying));
              }
            });
            break;
        }

        return error;
      }
    };
  }

  @Provides @Singleton UserToken provideUserToken(CathodeApp app) {
    SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(app);

    String token = settings.getString(Settings.TRAKT_TOKEN, null);

    return new UserToken(token);
  }
}
