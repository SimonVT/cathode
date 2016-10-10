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

package net.simonvt.cathode.search;

import net.simonvt.cathode.api.enumeration.ItemType;

public class Result {

  private ItemType itemType;

  private long itemId;

  private String title;

  private String overview;

  private float rating;

  private int relevance;

  public Result(ItemType itemType, long itemId, String title, String overview, float rating, int relevance) {
    this.itemType = itemType;
    this.itemId = itemId;
    this.title = title;
    this.overview = overview;
    this.rating = rating;
    this.relevance = relevance;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public long getItemId() {
    return itemId;
  }

  public String getTitle() {
    return title;
  }

  public String getOverview() {
    return overview;
  }

  public float getRating() {
    return rating;
  }

  public int getRelevance() {
    return relevance;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Result result = (Result) o;

    if (itemId != result.itemId) return false;
    if (Float.compare(result.rating, rating) != 0) return false;
    if (relevance != result.relevance) return false;
    if (itemType != result.itemType) return false;
    if (title != null ? !title.equals(result.title) : result.title != null) return false;
    return overview != null ? overview.equals(result.overview) : result.overview == null;
  }

  @Override public int hashCode() {
    int result = itemType.hashCode();
    result = 31 * result + (int) (itemId ^ (itemId >>> 32));
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (overview != null ? overview.hashCode() : 0);
    result = 31 * result + (rating != +0.0f ? Float.floatToIntBits(rating) : 0);
    result = 31 * result + relevance;
    return result;
  }
}
