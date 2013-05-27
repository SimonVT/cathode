package net.simonvt.trakt.api.entity;

import java.util.List;

public class UpdatedShows {

    public static class Timestamp {

        private Long start;

        private Long current;

        public Long getStart() {
            return start;
        }

        public Long getCurrent() {
            return current;
        }
    }

    public static class ShowTimestamp {

        private Long lastUpdated;

        private Integer tvdbId;

        public Long getLastUpdated() {
            return lastUpdated;
        }

        public Integer getTvdbId() {
            return tvdbId;
        }
    }

    private Timestamp timestamps;

    private List<ShowTimestamp> shows;

    public Timestamp getTimestamps() {
        return timestamps;
    }

    public List<ShowTimestamp> getShows() {
        return shows;
    }
}
