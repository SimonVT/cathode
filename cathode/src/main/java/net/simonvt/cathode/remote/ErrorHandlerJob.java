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
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.RequestFailedEvent;
import net.simonvt.cathode.jobqueue.Job;
import okhttp3.Headers;
import retrofit2.Response;
import timber.log.Timber;

public abstract class ErrorHandlerJob<T> extends Job {

  public ErrorHandlerJob() {
  }

  public ErrorHandlerJob(int flags) {
    super(flags);
  }

  /**
   * @return true if it's an isError
   */
  protected boolean isError(Response<T> response) throws IOException {
    final int statusCode = response.code();
    Timber.d("[%s] Status code: %d", key(), statusCode);

    if (statusCode == 401) {
      AuthFailedEvent.post();
      return true;
    } else if (response.code() == 404) {
      // TODO: Check body?
      return false;
    } else if (statusCode == 412) {
      response.raw().request().url().toString();
      Timber.i("Url: %s", response.raw().request().url().toString());

      Headers headers = response.headers();
      for (int i = 0; i < headers.size(); i++) {
        Timber.i("%s: %s", headers.name(i), headers.value(i));
      }

      String body = response.errorBody().string();
      Timber.i("Body: %s", body);

      Timber.e(new FourOneTwoException(), "Precondition failed");

      RequestFailedEvent.post(R.string.error_unknown_retrying);
      return true;
    } else if (statusCode >= 500 && statusCode < 600) {
      RequestFailedEvent.post(R.string.error_5xx_retrying);
      return true;
    } else {
      RequestFailedEvent.post(R.string.error_unknown_retrying);
      return true;
    }
  }
}
