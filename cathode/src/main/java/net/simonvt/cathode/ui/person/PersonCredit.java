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

package net.simonvt.cathode.ui.person;

import net.simonvt.cathode.api.enumeration.ItemType;

public class PersonCredit {

  String character;

  String job;

  ItemType itemType;

  long itemId;

  String poster;

  String title;

  String overview;

  int year;

  public static PersonCredit character(String character, ItemType itemType, long itemId,
      String poster, String title, String overview, int year) {
    PersonCredit credit = new PersonCredit();
    credit.character = character;
    credit.itemType = itemType;
    credit.itemId = itemId;
    credit.poster = poster;
    credit.title = title;
    credit.overview = overview;
    credit.year = year;
    return credit;
  }

  public static PersonCredit job(String job, ItemType itemType, long itemId, String poster,
      String title, String overview, int year) {
    PersonCredit credit = new PersonCredit();
    credit.job = job;
    credit.itemType = itemType;
    credit.itemId = itemId;
    credit.poster = poster;
    credit.title = title;
    credit.overview = overview;
    credit.year = year;
    return credit;
  }

  public String getCharacter() {
    return character;
  }

  public String getJob() {
    return job;
  }

  public ItemType getItemType() {
    return itemType;
  }

  public long getItemId() {
    return itemId;
  }

  public String getPoster() {
    return poster;
  }

  public String getTitle() {
    return title;
  }

  public String getOverview() {
    return overview;
  }

  public int getYear() {
    return year;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    PersonCredit that = (PersonCredit) o;

    if (itemId != that.itemId) return false;
    if (year != that.year) return false;
    if (character != null ? !character.equals(that.character) : that.character != null) {
      return false;
    }
    if (job != null ? !job.equals(that.job) : that.job != null) return false;
    if (itemType != that.itemType) return false;
    if (poster != null ? !poster.equals(that.poster) : that.poster != null) return false;
    if (title != null ? !title.equals(that.title) : that.title != null) return false;
    return overview != null ? overview.equals(that.overview) : that.overview == null;
  }

  @Override public int hashCode() {
    int result = character != null ? character.hashCode() : 0;
    result = 31 * result + (job != null ? job.hashCode() : 0);
    result = 31 * result + (itemType != null ? itemType.hashCode() : 0);
    result = 31 * result + (int) (itemId ^ (itemId >>> 32));
    result = 31 * result + (poster != null ? poster.hashCode() : 0);
    result = 31 * result + (title != null ? title.hashCode() : 0);
    result = 31 * result + (overview != null ? overview.hashCode() : 0);
    result = 31 * result + year;
    return result;
  }
}
