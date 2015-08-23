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

public class SqlIndex {

  private String name;

  private String table;

  private String[] columns;

  private boolean ifNotExists;

  public static SqlIndex index(String name) {
    SqlIndex index = new SqlIndex();
    index.name = name;
    return index;
  }

  public SqlIndex onTable(String table) {
    this.table = table;
    return this;
  }

  public SqlIndex forColumns(String... columns) {
    this.columns = columns;
    return this;
  }

  public SqlIndex ifNotExists() {
    ifNotExists = true;
    return this;
  }

  public String build() {
    StringBuilder builder = new StringBuilder("CREATE INDEX ");
    if (ifNotExists) {
      builder.append("IF NOT EXISTS ");
    }
    builder.append(name)
        .append(" ON ")
        .append(table)
        .append("(")
        .append(Joiner.on(",").join(columns))
        .append(")");
    return builder.toString();
  }
}
