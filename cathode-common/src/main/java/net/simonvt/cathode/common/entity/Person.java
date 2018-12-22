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

package net.simonvt.cathode.common.entity;

public class Person {

  Long id;
  String name;
  String biography;
  String birthday;
  String death;
  String birthplace;
  String homepage;

  public Person(Long id, String name, String biography, String birthday, String death,
      String birthplace, String homepage) {
    this.id = id;
    this.name = name;
    this.biography = biography;
    this.birthday = birthday;
    this.death = death;
    this.birthplace = birthplace;
    this.homepage = homepage;
  }

  public Long getId() {
    return id;
  }

  public String getName() {
    return name;
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
}
