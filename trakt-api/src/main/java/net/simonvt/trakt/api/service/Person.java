package net.simonvt.trakt.api.service;

import java.util.Date;
import net.simonvt.trakt.api.entity.Images;

public class Person {

  private String name;

  private String url;

  private String biography;

  private Date birthday;

  private String birthplace;

  private Integer tmdbId;

  private Images images;

  public String getName() {
    return name;
  }

  public String getUrl() {
    return url;
  }

  public String getBiography() {
    return biography;
  }

  public Date getBirthday() {
    return birthday;
  }

  public String getBirthplace() {
    return birthplace;
  }

  public Integer getTmdbId() {
    return tmdbId;
  }

  public Images getImages() {
    return images;
  }
}
