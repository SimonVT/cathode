package net.simonvt.trakt.api.entity;

import net.simonvt.trakt.api.enumeration.Status;

public class TraktResponse {

    private Status status;

    private String message;

    private String error;

    public Status getStatus() {
        return status;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }
}
