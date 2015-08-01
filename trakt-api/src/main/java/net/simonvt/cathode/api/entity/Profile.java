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

package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.Gender;

public class Profile {

  String username;

  Boolean isPrivate;

  String name;

  Boolean vip;

  Boolean vipEp;

  IsoTime joinedAt;

  String location;

  String about;

  Gender gender;

  Integer age;

  Images images;

  public String getUsername() {
    return username;
  }

  public Boolean isPrivate() {
    return isPrivate;
  }

  public String getName() {
    return name;
  }

  public Boolean isVip() {
    return vip;
  }

  public Boolean isVipEP() {
    return vipEp;
  }

  public IsoTime getJoinedAt() {
    return joinedAt;
  }

  public String getLocation() {
    return location;
  }

  public String getAbout() {
    return about;
  }

  public Gender getGender() {
    return gender;
  }

  public Integer getAge() {
    return age;
  }

  public Images getImages() {
    return images;
  }
}
