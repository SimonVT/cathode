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

public class Person {

  private long traktId;

  private String name;

  private String headshot;

  private String screenshot;

  private String biography;

  private String birthday;

  private String death;

  private String birthplace;

  private String homepage;

  private long lastSync;

  private PersonCredits credits;

  public Person(long traktId, String name, String headshot, String screenshot, String biography,
      String birthday, String death, String birthplace, String homepage, long lastSync,
      PersonCredits credits) {
    this.traktId = traktId;
    this.name = name;
    this.headshot = headshot;
    this.screenshot = screenshot;
    this.biography = biography;
    this.birthday = birthday;
    this.death = death;
    this.birthplace = birthplace;
    this.homepage = homepage;
    this.lastSync = lastSync;
    this.credits = credits;
  }

  public long getTraktId() {
    return traktId;
  }

  public String getName() {
    return name;
  }

  public String getHeadshot() {
    return headshot;
  }

  public String getScreenshot() {
    return screenshot;
  }

  public String getBiography() {
    return biography;
  }

  public String getBirthday() {
    return birthday;
  }

  public String getDeath() {
    return death;
  }

  public String getBirthplace() {
    return birthplace;
  }

  public String getHomepage() {
    return homepage;
  }

  public long getLastSync() {
    return lastSync;
  }

  public PersonCredits getCredits() {
    return credits;
  }
}
