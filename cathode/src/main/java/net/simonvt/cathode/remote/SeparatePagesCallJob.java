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

package net.simonvt.cathode.remote;

import java.io.IOException;
import java.util.List;
import okhttp3.Headers;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public abstract class SeparatePagesCallJob<T> extends ErrorHandlerJob<List<T>> {

  private static final String HEADER_PAGE_COUNT = "x-pagination-page-count";

  public SeparatePagesCallJob() {
  }

  public SeparatePagesCallJob(int flags) {
    super(flags);
  }

  @Override public final boolean perform() {
    try {
      int page = 1;
      int pageCount = 0;
      do {
        Call<List<T>> call = getCall(page);
        Response<List<T>> response = call.execute();
        if (!response.isSuccessful()) {
          return !isError(response);
        }

        Headers headers = response.headers();
        String pageCountStr = headers.get(HEADER_PAGE_COUNT);
        if (pageCountStr != null) {
          pageCount = Integer.valueOf(pageCountStr);
        }

        List<T> result = response.body();
        if (!handleResponse(page, result)) {
          return false;
        }

        page++;
      } while (page <= pageCount);

      return onDone();
    } catch (IOException e) {
      Timber.d(e, "Job failed: %s", key());
    }

    return false;
  }

  public abstract Call<List<T>> getCall(int page);

  public abstract boolean handleResponse(int page, List<T> response);

  public boolean onDone() {
    return true;
  }
}
