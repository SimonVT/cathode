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

package net.simonvt.cathode.jobqueue;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import java.io.ByteArrayInputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import timber.log.Timber;

public class Converter {

  private final Gson gson;

  public Converter() {
    GsonBuilder builder = new GsonBuilder();
    builder.registerTypeAdapter(Job.class, new JobSerializer());
    gson = builder.create();
  }

  public Job from(byte[] bytes) {
    Reader reader = new InputStreamReader(new ByteArrayInputStream(bytes));
    return gson.fromJson(reader, Job.class);
  }

  public byte[] to(Job job) {
    try {
      return gson.toJson(job, Job.class).getBytes("UTF-8");
    } catch (UnsupportedEncodingException e) {
      Timber.e(e, "Unable to convert Job to json");
    }

    return null;
  }
}
