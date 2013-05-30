package net.simonvt.trakt.event;

public class SearchFailureEvent {

    public enum Type {
        SHOW,
        MOVIE,
    }

    private static final String TAG = "SearchFailureEvent";

    private Type mType;

    public SearchFailureEvent(Type type) {
        mType = type;
    }

    public Type getType() {
        return mType;
    }
}
