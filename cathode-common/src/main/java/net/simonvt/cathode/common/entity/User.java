/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

public class User {

  private Long id;
  private String username;
  private Boolean isPrivate;
  private String name;
  private Boolean vip;
  private Boolean vipEp;
  private Long joinedAt;
  private String location;
  private String about;
  private String gender;
  private Integer age;
  private String avatar;

  public User(Long id, String username, Boolean isPrivate, String name, Boolean vip, Boolean vipEp,
      Long joinedAt, String location, String about, String gender, Integer age, String avatar) {
    this.id = id;
    this.username = username;
    this.isPrivate = isPrivate;
    this.name = name;
    this.vip = vip;
    this.vipEp = vipEp;
    this.joinedAt = joinedAt;
    this.location = location;
    this.about = about;
    this.gender = gender;
    this.age = age;
    this.avatar = avatar;
  }

  public long getId() {
    return id;
  }

  public String getUsername() {
    return username;
  }

  public Boolean getPrivate() {
    return isPrivate;
  }

  public String getName() {
    return name;
  }

  public Boolean getVip() {
    return vip;
  }

  public Boolean getVipEp() {
    return vipEp;
  }

  public Long getJoinedAt() {
    return joinedAt;
  }

  public String getLocation() {
    return location;
  }

  public String getAbout() {
    return about;
  }

  public String getGender() {
    return gender;
  }

  public Integer getAge() {
    return age;
  }

  public String getAvatar() {
    return avatar;
  }
}
