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
import net.simonvt.cathode.jobqueue.Job;
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

    return ErrorHandler.isError(response);
  }
}
