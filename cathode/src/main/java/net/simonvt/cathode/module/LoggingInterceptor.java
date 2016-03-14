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

import java.io.IOException;
import java.nio.charset.Charset;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.api.AuthInterceptor;
import net.simonvt.cathode.api.FourOhFourException;
import net.simonvt.cathode.api.TraktException;
import okhttp3.Headers;
import okhttp3.Interceptor;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import okio.Buffer;
import okio.BufferedSource;
import timber.log.Timber;

public class LoggingInterceptor implements Interceptor {

  private static final Charset UTF8 = Charset.forName("UTF-8");

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    Response response = chain.proceed(request);

    final int statusCode = response.code();

    if (statusCode == 404 || statusCode == 412) {
      Timber.i("Url: %s", request.url().toString());
      Timber.i("Status code: %d", statusCode);

      Headers headers = response.headers();
      for (int i = 0; i < headers.size(); i++) {
        String name = headers.name(i);
        if (BuildConfig.DEBUG || !AuthInterceptor.HEADER_AUTHORIZATION.equals(name)) {
          Timber.i("%s: %s", name, headers.get(headers.name(i)));
        }
      }

      if (BuildConfig.DEBUG || statusCode == 404) {
        Timber.d("%s", response.body().string());
      }

      if (statusCode == 404) {
        Timber.e(new FourOhFourException("Status code " + statusCode), "Status code %d",
            statusCode);
      } else {
        Timber.e(new TraktException("Status code " + statusCode), "Status code %d", statusCode);
      }
    } else if (BuildConfig.DEBUG && statusCode >= 400) {
      Timber.d("Url: %s", request.url().toString());
      Timber.d("Status code: %d", statusCode);

      Headers headers = response.headers();
      for (int i = 0; i < headers.size(); i++) {
        String name = headers.name(i);
        Timber.d("%s: %s", name, headers.get(headers.name(i)));
      }

      // Timber.d("%s", response.body().string());
      ResponseBody responseBody = response.body();
      BufferedSource source = responseBody.source();
      source.request(Long.MAX_VALUE); // Buffer the entire body.
      Buffer buffer = source.buffer();

      Charset charset = UTF8;
      MediaType contentType = responseBody.contentType();
      if (contentType != null) {
        charset = contentType.charset(UTF8);
      }

      if (responseBody.contentLength() != 0) {
        Timber.d(buffer.clone().readString(charset));
      }
    }

    return response;
  }
}
