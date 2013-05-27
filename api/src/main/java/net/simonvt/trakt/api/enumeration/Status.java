package net.simonvt.trakt.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum Status {
    SUCCESS("success"),
    FAILURE("failure");

    private final String mValue;

    private Status(String value) {
        this.mValue = value;
    }

    private static final Map<String, Status> STRING_MAPPING = new HashMap<String, Status>();

    static {
        for (Status via : Status.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static Status fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }

    @Override
    public String toString() {
        return mValue;
    }
}
