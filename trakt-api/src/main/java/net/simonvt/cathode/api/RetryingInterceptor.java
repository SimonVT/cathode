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

package net.simonvt.cathode.api;

import com.squareup.okhttp.Interceptor;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import java.io.IOException;
import timber.log.Timber;

public class RetryingInterceptor implements Interceptor {

  private final int maxRetryCount;

  private final long retryDelay;

  public RetryingInterceptor(int maxRetryCount, long retryDelay) {
    this.maxRetryCount = maxRetryCount;
    this.retryDelay = retryDelay;
  }

  @Override public Response intercept(Chain chain) throws IOException {
    Request request = chain.request();
    return proceed(chain, request);
  }

  private Response proceed(Chain chain, Request request) throws IOException {
    return proceed(chain, request, 0);
  }

  private Response proceed(Chain chain, Request request, int retryCount) throws IOException {
    if (retryCount > 0 && retryDelay > 0) {
      Timber.d("Retrying request in %d milliseconds", retryDelay);
      try {
        Thread.sleep(retryDelay);
      } catch (InterruptedException e) {
        // Ignore
      }
    }
    Response response;
    try {
      response = chain.proceed(request);
      if (!response.isSuccessful()) {
        if (retryCount < maxRetryCount) {
          response = proceed(chain, request, retryCount + 1);
        }
      }
    } catch (IOException e) {
      if (retryCount < maxRetryCount) {
        response = proceed(chain, request, retryCount + 1);
      } else {
        throw e;
      }
    }

    return response;
  }
}
