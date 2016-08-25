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

package net.simonvt.cathode.api.body;

import net.simonvt.cathode.api.enumeration.Privacy;

public class ListInfoBody {

  private String name;

  private String description;

  private Privacy privacy;

  private Boolean displayNumbers;

  private Boolean allowComments;

  private ListInfoBody() {
  }

  public static ListInfoBody name(String name) {
    ListInfoBody createList = new ListInfoBody();
    createList.name = name;
    return createList;
  }

  public ListInfoBody description(String description) {
    this.description = description;
    return this;
  }

  public ListInfoBody privacy(Privacy privacy) {
    this.privacy = privacy;
    return this;
  }

  public ListInfoBody displayNumbers(Boolean displayNumbers) {
    this.displayNumbers = displayNumbers;
    return this;
  }

  public ListInfoBody allowComments(Boolean allowComments) {
    this.allowComments = allowComments;
    return this;
  }
}
