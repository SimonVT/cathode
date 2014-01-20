package net.simonvt.cathode.api.entity;

public class Person {

  private String name;

  private String job;

  private String character;

  private Images images;

  private Boolean executive;

  private String url;

  private String biography;

  private String birthday;

  private String birthplace;

  private Integer tmdbId;

  public String getName() {
    return name;
  }

  public String getJob() {
    return job;
  }

  public String getCharacter() {
    return character;
  }

  public Images getImages() {
    return images;
  }

  public Boolean isExecutive() {
    return executive;
  }

  public String getUrl() {
    return url;
  }

  public String getBiography() {
    return biography;
  }

  public String getBirthday() {
    return birthday;
  }

  public String getBirthplace() {
    return birthplace;
  }

  public Integer getTmdbId() {
    return tmdbId;
  }
}
