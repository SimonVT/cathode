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
package net.simonvt.cathode.ui.credits;

public class Credit {

  long personId;

  String headshot;

  String name;

  String character;

  String job;

  public static Credit character(String character, long personId, String name, String headshot) {
    Credit credit = new Credit();
    credit.character = character;
    credit.personId = personId;
    credit.name = name;
    credit.headshot = headshot;
    return credit;
  }

  public static Credit job(String job, long personId, String name, String headshot) {
    Credit credit = new Credit();
    credit.job = job;
    credit.personId = personId;
    credit.name = name;
    credit.headshot = headshot;
    return credit;
  }

  public long getPersonId() {
    return personId;
  }

  public String getHeadshot() {
    return headshot;
  }

  public String getName() {
    return name;
  }

  public String getCharacter() {
    return character;
  }

  public String getJob() {
    return job;
  }

  @Override public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    Credit credit = (Credit) o;

    if (personId != credit.personId) return false;
    if (headshot != null ? !headshot.equals(credit.headshot) : credit.headshot != null) {
      return false;
    }
    if (character != null ? !character.equals(credit.character) : credit.character != null) {
      return false;
    }
    return job != null ? job.equals(credit.job) : credit.job == null;
  }

  @Override public int hashCode() {
    int result = (int) (personId ^ (personId >>> 32));
    result = 31 * result + (headshot != null ? headshot.hashCode() : 0);
    result = 31 * result + (character != null ? character.hashCode() : 0);
    result = 31 * result + (job != null ? job.hashCode() : 0);
    return result;
  }
}
