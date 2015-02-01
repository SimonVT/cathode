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

import java.io.IOException;
import java.util.ArrayList;
import net.simonvt.cathode.IntPreference;
import retrofit.client.Client;
import retrofit.client.Header;
import retrofit.client.OkClient;
import retrofit.client.Request;
import retrofit.client.Response;

public class DebugClient implements Client {

  private OkClient okClient;

  private IntPreference httpStatusCode;

  public DebugClient(IntPreference httpStatusCode) {
    this.httpStatusCode = httpStatusCode;

    okClient = new OkClient();
  }

  @Override public Response execute(Request request) throws IOException {
    final int statusCode = httpStatusCode.get();
    if (statusCode == 200) {
      return okClient.execute(request);
    }

    return new Response(request.getUrl(), httpStatusCode.get(), "Debug resposne",
        new ArrayList<Header>(), null);
  }
}
