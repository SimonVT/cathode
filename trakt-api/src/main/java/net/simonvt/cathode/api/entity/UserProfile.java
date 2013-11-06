package net.simonvt.cathode.api.entity;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import net.simonvt.cathode.api.enumeration.ActivityAction;
import net.simonvt.cathode.api.enumeration.ActivityType;
import net.simonvt.cathode.api.enumeration.Gender;

public class UserProfile {

  public static class Stats {

    public static class Shows {

      private Integer library;

      private Integer watched;

      private Integer collection;

      private Integer shouts;

      private Integer loved;

      private Integer hated;

      public Integer getLibrary() {
        return library;
      }

      public Integer getWatched() {
        return watched;
      }

      public Integer getCollection() {
        return collection;
      }

      public Integer getShouts() {
        return shouts;
      }

      public Integer getLoved() {
        return loved;
      }

      public Integer getHated() {
        return hated;
      }
    }

    public static class Episodes {

      private Integer watched;

      private Integer watchedUnique;

      private Integer scrobbles;

      private Integer scrobblesUnique;

      private Integer checkins;

      private Integer checkinsUnique;

      private Integer seen;

      private Integer unwatched;

      private Integer collection;

      private Integer shouts;

      private Integer loved;

      private Integer hated;

      public Integer getWatched() {
        return watched;
      }

      public Integer getWatchedUnique() {
        return watchedUnique;
      }

      public Integer getScrobbles() {
        return scrobbles;
      }

      public Integer getScrobblesUnique() {
        return scrobblesUnique;
      }

      public Integer getCheckins() {
        return checkins;
      }

      public Integer getCheckinsUnique() {
        return checkinsUnique;
      }

      public Integer getSeen() {
        return seen;
      }

      public Integer getUnwatched() {
        return unwatched;
      }

      public Integer getCollection() {
        return collection;
      }

      public Integer getShouts() {
        return shouts;
      }

      public Integer getLoved() {
        return loved;
      }

      public Integer getHated() {
        return hated;
      }
    }

    public static class Movies {

      private Integer watched;

      private Integer watchedUnique;

      private Integer scrobbles;

      private Integer scrobblesUnique;

      private Integer checkins;

      private Integer checkinsUnique;

      private Integer seen;

      private Integer library;

      private Integer unwatched;

      private Integer collection;

      private Integer shouts;

      private Integer loved;

      private Integer hated;

      public Integer getWatched() {
        return watched;
      }

      public Integer getWatchedUnique() {
        return watchedUnique;
      }

      public Integer getScrobbles() {
        return scrobbles;
      }

      public Integer getScrobblesUnique() {
        return scrobblesUnique;
      }

      public Integer getCheckins() {
        return checkins;
      }

      public Integer getCheckinsUnique() {
        return checkinsUnique;
      }

      public Integer getSeen() {
        return seen;
      }

      public Integer getLibrary() {
        return library;
      }

      public Integer getUnwatched() {
        return unwatched;
      }

      public Integer getCollection() {
        return collection;
      }

      public Integer getShouts() {
        return shouts;
      }

      public Integer getLoved() {
        return loved;
      }

      public Integer getHated() {
        return hated;
      }
    }

    private Integer friends;

    private Shows shows;

    private Episodes episodes;

    private Movies movies;

    public Integer getFriends() {
      return friends;
    }

    public Shows getShows() {
      return shows;
    }

    public Episodes getEpisodes() {
      return episodes;
    }

    public Movies getOvies() {
      return movies;
    }
  }

  public static class ActivityItem {

    private ActivityType type;

    private ActivityAction action;

    private Long watched;

    private Movie movie;

    private TvShow show;

    private Episode episode;

    public ActivityType getType() {
      return type;
    }

    public ActivityAction getAction() {
      return action;
    }

    public Long getWatched() {
      return watched;
    }

    public Movie getOvie() {
      return movie;
    }

    public TvShow getShow() {
      return show;
    }

    public Episode getEpisode() {
      return episode;
    }
  }

  private String username;

  @SerializedName("protected") private Boolean isProtected;

  private String fullName;

  private Gender gender;

  private Integer age;

  private String location;

  private String about;

  private Long joined;

  private String avatar;

  private String url;

  private Boolean vip;

  private Stats stats;

  private ActivityItem watching;

  private List<ActivityItem> watched;

  private Integer plays;

  public String getUsername() {
    return username;
  }

  public Boolean isProtected() {
    return isProtected;
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

  public String getAvatar() {
    return avatar;
  }

  public String getUrl() {
    return url;
  }

  public Boolean getVip() {
    return vip;
  }

  public Stats getStats() {
    return stats;
  }

  public ActivityItem getWatching() {
    return watching;
  }

  public List<ActivityItem> getWatched() {
    return watched;
  }

  public Integer getPlays() {
    return plays;
  }
}
