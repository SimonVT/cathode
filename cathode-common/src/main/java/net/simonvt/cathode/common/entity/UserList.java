package net.simonvt.cathode.common.entity;

import net.simonvt.cathode.api.enumeration.Privacy;

public class UserList {

  long id;
  String name;
  String description;
  Privacy privacy;
  Boolean displayNumbers;
  Boolean allowComments;
  Long updatedAt;
  Integer likes;
  String slug;
  Long traktId;

  public UserList(long id, String name, String description, Privacy privacy, Boolean displayNumbers,
      Boolean allowComments, Long updatedAt, Integer likes, String slug, Long traktId) {
    this.id = id;
    this.name = name;
    this.description = description;
    this.privacy = privacy;
    this.displayNumbers = displayNumbers;
    this.allowComments = allowComments;
    this.updatedAt = updatedAt;
    this.likes = likes;
    this.slug = slug;
    this.traktId = traktId;
  }

  public long getId() {
    return id;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public Privacy getPrivacy() {
    return privacy;
  }

  public Boolean getDisplayNumbers() {
    return displayNumbers;
  }

  public Boolean getAllowComments() {
    return allowComments;
  }

  public Long getUpdatedAt() {
    return updatedAt;
  }

  public Integer getLikes() {
    return likes;
  }

  public String getSlug() {
    return slug;
  }

  public Long getTraktId() {
    return traktId;
  }
}
