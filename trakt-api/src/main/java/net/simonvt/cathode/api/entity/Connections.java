package net.simonvt.cathode.api.entity;

public class Connections {

  private Facebook facebook;

  private Twitter twitter;

  private Tumblr tumblr;

  public Facebook getFacebook() {
    return facebook;
  }

  public Twitter getTwitter() {
    return twitter;
  }

  public Tumblr getTumblr() {
    return tumblr;
  }

  public static class Facebook {

    private Boolean connected;
    private Boolean timelineEnabled;
    private Boolean shareScrobblesStart;
    private Boolean shareScrobblesEnd;
    private Boolean shareTv;
    private Boolean shareMovies;
    private Boolean shareRatings;
    private Boolean shareCheckins;

    public Boolean isConnected() {
      return connected;
    }

    public Boolean isTimelineEnabled() {
      return timelineEnabled;
    }

    public Boolean isShareScrobblesStart() {
      return shareScrobblesStart;
    }

    public Boolean isShareScrobblesEnd() {
      return shareScrobblesEnd;
    }

    public Boolean isShareTv() {
      return shareTv;
    }

    public Boolean isShareMovies() {
      return shareMovies;
    }

    public Boolean isShareRatings() {
      return shareRatings;
    }

    public Boolean isShareCheckins() {
      return shareCheckins;
    }
  }

  public static class Twitter {

    private Boolean connected;
    private Boolean shareScrobblesStart;
    private Boolean shareScrobblesEnd;
    private Boolean shareTv;
    private Boolean shareMovies;
    private Boolean shareRatings;
    private Boolean shareCheckins;

    public Boolean isConnected() {
      return connected;
    }

    public Boolean isShareScrobblesStart() {
      return shareScrobblesStart;
    }

    public Boolean isShareScrobblesEnd() {
      return shareScrobblesEnd;
    }

    public Boolean isShareTv() {
      return shareTv;
    }

    public Boolean isShareMovies() {
      return shareMovies;
    }

    public Boolean isShareRatings() {
      return shareRatings;
    }

    public Boolean isShareCheckins() {
      return shareCheckins;
    }
  }

  public static class Tumblr {

    private Boolean connected;
    private Boolean shareScrobblesStart;
    private Boolean shareScrobblesEnd;
    private Boolean shareTv;
    private Boolean shareMovies;
    private Boolean shareRatings;
    private Boolean shareCheckins;

    public Boolean isConnected() {
      return connected;
    }

    public Boolean isShareScrobblesStart() {
      return shareScrobblesStart;
    }

    public Boolean isShareScrobblesEnd() {
      return shareScrobblesEnd;
    }

    public Boolean isShareTv() {
      return shareTv;
    }

    public Boolean isShareMovies() {
      return shareMovies;
    }

    public Boolean isShareRatings() {
      return shareRatings;
    }

    public Boolean isShareCheckins() {
      return shareCheckins;
    }
  }
}
