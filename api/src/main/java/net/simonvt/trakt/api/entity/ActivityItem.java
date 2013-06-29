package net.simonvt.trakt.api.entity;

import com.google.gson.annotations.SerializedName;

import net.simonvt.trakt.api.enumeration.ActivityAction;
import net.simonvt.trakt.api.enumeration.ActivityType;
import net.simonvt.trakt.api.enumeration.Rating;

public class ActivityItem {

    public class When {

        private String day;

        private String time;

        public String getDay() {
            return day;
        }

        public String getTime() {
            return time;
        }
    }

    public class Elapsed {

        private String full;

        @SerializedName("short") private String _short;

        public String getFull() {
            return full;
        }

        public String getShort() {
            return _short;
        }
    }

    private long timestamp;

    private When when;

    private Elapsed elapsed;

    private ActivityType type;

    private ActivityAction action;

    private UserProfile user;

    private TvShow show;

    private Episode episode;

    private Movie movie;

    private Rating rating;

    private int ratingAdvanced;

    public long getTimestamp() {
        return timestamp;
    }

    public When getWhen() {
        return when;
    }

    public Elapsed getElapsed() {
        return elapsed;
    }

    public ActivityType getType() {
        return type;
    }

    public ActivityAction getAction() {
        return action;
    }

    public UserProfile getUser() {
        return user;
    }

    public TvShow getShow() {
        return show;
    }

    public Episode getEpisode() {
        return episode;
    }

    public Movie getOvie() {
        return movie;
    }

    public Rating getRating() {
        return rating;
    }

    public int getRatingAdvanced() {
        return ratingAdvanced;
    }
}
