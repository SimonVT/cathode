/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.appwidget;

public class WidgetItem {

  public static final int TYPE_DAY = 0;
  public static final int TYPE_ITEM = 1;

  int itemType;

  public WidgetItem(int itemType) {
    this.itemType = itemType;
  }

  public int getItemType() {
    return itemType;
  }
}
