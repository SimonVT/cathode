package net.simonvt.trakt.api.entity;

import net.simonvt.trakt.api.enumeration.Gender;

public class Profile {

  private String username;

  private String fullName;

  private Gender gender;

  private Integer age;

  private String location;

  private String about;

  private Long joined;

  private Long lastLogin;

  private String avatar;

  private String url;

  private Boolean vip;

  public String getUsername() {
    return username;
  }

  public String getFullName() {
    return fullName;
  }

  public Gender getGender() {
    return gender;
  }

  public Integer getAge() {
    return age;
  }

  public String getLocation() {
    return location;
  }

  public String getAbout() {
    return about;
  }

  public Long getJoined() {
    return joined;
  }

  public Long getLastLogin() {
    return lastLogin;
  }

  public String getAvatar() {
    return avatar;
  }

  public String getUrl() {
    return url;
  }

  public Boolean isVip() {
    return vip;
  }
}
