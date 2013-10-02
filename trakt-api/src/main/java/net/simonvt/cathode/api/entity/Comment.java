package net.simonvt.cathode.api.entity;

import net.simonvt.cathode.api.enumeration.CommentType;
import net.simonvt.cathode.api.enumeration.Rating;

public class Comment {

  public class Ratings {

    private Rating rating;

    private Integer ratingAdvanced;

    public Rating getRating() {
      return rating;
    }

    public Integer getRatingAdvanced() {
      return ratingAdvanced;
    }
  }

  private Long id;

  private Long inserted;

  private String text;

  private String textHtml;

  private Boolean spoiler;

  private CommentType type;

  private Integer likes;

  private Integer replies;

  private UserProfile userProfile;

  private Ratings userRatings;

  public Long getId() {
    return id;
  }

  public Long getInserted() {
    return inserted;
  }

  public String getText() {
    return text;
  }

  public String getTextHtml() {
    return textHtml;
  }

  public Boolean getSpoiler() {
    return spoiler;
  }

  public CommentType getType() {
    return type;
  }

  public Integer getLikes() {
    return likes;
  }

  public Integer getReplies() {
    return replies;
  }

  public UserProfile getUserProfile() {
    return userProfile;
  }

  public Ratings getUserRatings() {
    return userRatings;
  }
}
