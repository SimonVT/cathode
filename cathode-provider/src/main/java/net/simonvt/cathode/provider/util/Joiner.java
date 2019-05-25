/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
package net.simonvt.cathode.provider.util;

import java.util.List;

public class Joiner {

  private String separator;

  private Joiner(String separator) {
    this.separator = separator;
  }

  public static Joiner on(String separator) {
    return new Joiner(separator);
  }

  public String join(Object... objects) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (Object o : objects) {
      if (!first) {
        sb.append(separator);
      } else {
        first = false;
      }

      sb.append(o.toString());
    }

    return sb.toString();
  }

  public String join(List<?> objects) {
    StringBuilder sb = new StringBuilder();
    boolean first = true;

    for (Object o : objects) {
      if (!first) {
        sb.append(separator);
      } else {
        first = false;
      }

      sb.append(o.toString());
    }

    return sb.toString();
  }
}
