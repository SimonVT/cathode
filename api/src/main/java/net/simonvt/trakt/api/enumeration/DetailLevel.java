package net.simonvt.trakt.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum DetailLevel {
    EXTENDED("true"),
    NORMAL(""),
    MIN("min");

    private String mValue;

    DetailLevel(String value) {
        mValue = value;
    }

    @Override
    public String toString() {
        return mValue;
    }

    private static final Map<String, DetailLevel> STRING_MAPPING = new HashMap<String, DetailLevel>();

    static {
        for (DetailLevel via : DetailLevel.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static DetailLevel fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }
}
