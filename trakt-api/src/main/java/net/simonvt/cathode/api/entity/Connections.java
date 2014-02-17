package net.simonvt.cathode.api.entity;

public class Connections {

  public static class SocialNetwork {

    private Boolean connected;

    public Boolean isConnected() {
      return connected;
    }
  }

  private SocialNetwork facebook;

  private SocialNetwork twitter;

  private SocialNetwork tumblr;

  private SocialNetwork path;

  private SocialNetwork prowl;

  public SocialNetwork getFacebook() {
    return facebook;
  }

  public SocialNetwork getTwitter() {
    return twitter;
  }

  public SocialNetwork getTumblr() {
    return tumblr;
  }

  public SocialNetwork getPath() {
    return path;
  }

  public SocialNetwork getProwl() {
    return prowl;
  }
}
