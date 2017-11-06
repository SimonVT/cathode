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
import com.google.gson.Gson;
import dagger.Module;
import dagger.Provides;
import java.io.IOException;
import javax.inject.Named;
import net.simonvt.cathode.IntPreference;
import net.simonvt.cathode.api.TraktModule;
import net.simonvt.cathode.settings.Settings;
import net.simonvt.cathode.sync.tmdb.TmdbSettings;
import okhttp3.Interceptor;
import okhttp3.OkHttpClient;
import okhttp3.Response;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import timber.log.Timber;

import static net.simonvt.cathode.api.TraktModule.NAMED_TRAKT;
import static net.simonvt.cathode.sync.tmdb.TmdbModule.NAMED_TMDB;
import static net.simonvt.cathode.sync.tmdb.TmdbModule.NAMED_TMDB_API_KEY;

@Module
public class DebugModule {

  public static final String NAMED_STATUS_CODE = "httpStatusCode";

  @Provides HttpLoggingInterceptor provideLoggingInterceptor() {
    return new HttpLoggingInterceptor();
  }

  @Provides @Named(NAMED_TRAKT) Retrofit provideRestAdapter(@Named(NAMED_TRAKT) OkHttpClient client,
      @Named(NAMED_TRAKT) Gson gson, @Named(NAMED_STATUS_CODE) final IntPreference httpStatusCode,
      HttpLoggingInterceptor loggingInterceptor) {
    OkHttpClient.Builder builder = client.newBuilder();
    builder.networkInterceptors().add(new Interceptor() {
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
    builder.interceptors().add(loggingInterceptor);
    return new Retrofit.Builder() //
        .baseUrl(TraktModule.API_URL)
        .client(builder.build())
        .addConverterFactory(GsonConverterFactory.create(gson))
        .build();
  }

  @Provides @Named(NAMED_STATUS_CODE) IntPreference provideHttpStatusCodePreference(Context context) {
    return new IntPreference(Settings.get(context), "debug_httpStatusCode", 200);
  }

  @Provides TmdbSettings tmdbSettings(@Named(NAMED_TMDB_API_KEY) String apiKey,
      @Named(NAMED_TMDB) OkHttpClient.Builder okBuilder,
      HttpLoggingInterceptor httpLoggingInterceptor) {
    okBuilder.interceptors().add(httpLoggingInterceptor);
    return new TmdbSettings(apiKey, okBuilder);
  }
}
