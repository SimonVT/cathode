package net.simonvt.trakt.api.enumeration;

import java.util.HashMap;
import java.util.Map;

public enum Gender {
    MALE("male"),
    FEMALE("female");

    private String mGender;

    Gender(String gender) {
        mGender = gender;
    }

    private static final Map<String, Gender> STRING_MAPPING = new HashMap<String, Gender>();

    static {
        for (Gender via : Gender.values()) {
            STRING_MAPPING.put(via.toString().toUpperCase(), via);
        }
    }

    public static Gender fromValue(String value) {
        return STRING_MAPPING.get(value.toUpperCase());
    }

    @Override
    public String toString() {
        return mGender;
    }
}
