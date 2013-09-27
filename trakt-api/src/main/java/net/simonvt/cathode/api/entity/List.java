package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.ListPrivacy;

public class List {

  String name;

  String slug;

  String url;

  String description;

  ListPrivacy privacy;

  Boolean showNumbers;

  Boolean allowShouts;

  java.util.List<ListItem> items;

  public String getName() {
    return name;
  }

  public String getSlug() {
    return slug;
  }

  public String getUrl() {
    return url;
  }

  public String getDescription() {
    return description;
  }

  public ListPrivacy getPrivacy() {
    return privacy;
  }

  public Boolean showNumbers() {
    return showNumbers;
  }

  public Boolean allowShouts() {
    return allowShouts;
  }

  public java.util.List<ListItem> getItems() {
    return items;
  }
}
