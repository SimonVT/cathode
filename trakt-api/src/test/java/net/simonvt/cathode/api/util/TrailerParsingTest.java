/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.util;

import org.junit.Test;

import static com.google.common.truth.Truth.assertThat;

public class TrailerParsingTest {

  private static final String[] YOUTUBE_URLS = new String[] {
      "http://youtube.com/watch?v=dQw4w9WgXcQ",
      "http://youtu.be/dQw4w9WgXcQ",
      "http://www.youtube.com/watch?v=dQw4w9WgXcQ&feature=channel",
  };

  @Test public void testYoutubeIdParsing() {
    for (String url : YOUTUBE_URLS) {
      String id = TraktUtils.getYoutubeId(url);
      assertThat(id).isEqualTo("dQw4w9WgXcQ");
    }
  }
}
