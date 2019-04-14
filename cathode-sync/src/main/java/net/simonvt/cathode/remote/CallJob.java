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

package net.simonvt.cathode.remote;

import java.io.IOException;
import retrofit2.Call;
import retrofit2.Response;
import timber.log.Timber;

public abstract class CallJob<T> extends ErrorHandlerJob<T> {

  @Override public boolean perform() {
    try {
      Call<T> call = getCall();
      Response<T> response = call.execute();

      if (response.isSuccessful()) {
        return handleResponse(response.body());
      } else {
        return !isError(response);
      }
    } catch (IOException e) {
      Timber.d(e, "Job failed: %s", key());
      return false;
    }
  }

  public abstract Call<T> getCall();

  public abstract boolean handleResponse(T response);
}
