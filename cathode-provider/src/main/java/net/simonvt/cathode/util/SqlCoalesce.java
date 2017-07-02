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

package net.simonvt.cathode.util;

import net.simonvt.cathode.api.util.Joiner;

public class SqlCoalesce {

  private SqlCoalesce() {
  }

  StringBuilder builder = new StringBuilder();

  public static SqlCoalesce coaloesce(String... columns) {
    SqlCoalesce coalesce = new SqlCoalesce();
    coalesce.builder.append("coalesce(");
    coalesce.builder.append(Joiner.on(", ").join(columns));
    coalesce.builder.append(")");
    return coalesce;
  }

  public String as(String column) {
    return builder.append(" AS ").append(column).toString();
  }
}
