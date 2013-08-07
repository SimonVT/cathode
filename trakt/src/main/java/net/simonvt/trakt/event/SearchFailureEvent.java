package net.simonvt.trakt.event;

public class SearchFailureEvent {

  public enum Type {
    SHOW,
    MOVIE,
  }

  private static final String TAG = "SearchFailureEvent";

  private Type type;

  public SearchFailureEvent(Type type) {
    this.type = type;
  }

  public Type getType() {
    return type;
  }
}
