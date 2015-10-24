/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

import android.content.Context;
import android.preference.PreferenceManager;
import com.google.gson.Gson;
import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Response;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import javax.inject.Singleton;
import net.simonvt.cathode.HttpStatusCode;
import net.simonvt.cathode.IntPreference;
import net.simonvt.cathode.api.Trakt;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.remote.InitialSyncJob;
import net.simonvt.cathode.ui.BaseActivity;
import retrofit.GsonConverterFactory;
import retrofit.Retrofit;
import timber.log.Timber;

@Module(
    addsTo = AppModule.class,
    overrides = true,
    injects = {
        BaseActivity.DebugInjects.class, InitialSyncJob.class
    }) public class DebugModule {

  private Context context;

  public DebugModule(Context context) {
    this.context = context;
  }

  @Provides @Singleton @Trakt Retrofit provideRestAdapter(@Trakt OkHttpClient client,
      @Trakt Gson gson, @HttpStatusCode final IntPreference httpStatusCode) {
    client.networkInterceptors().add(new Interceptor() {
      @Override public Response intercept(Chain chain) throws IOException {
        final int statusCode = httpStatusCode.get();
        Response response = chain.proceed(chain.request());
        if (statusCode != 200) {
          Timber.d("Rewriting status code: %d", statusCode);
          response = response.newBuilder().code(statusCode).build();
        }
        return response;
      }
    });
    return new Retrofit.Builder() //
        .baseUrl(TraktModule.API_URL)
        .client(client)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build();
  }

  @Provides @Singleton @HttpStatusCode IntPreference provideHttpStatusCodePreference() {
    return new IntPreference(PreferenceManager.getDefaultSharedPreferences(context),
        "debug_httpStatusCode", 200);
  }
}
