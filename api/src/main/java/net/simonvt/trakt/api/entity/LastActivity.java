package net.simonvt.trakt.api.entity;

public class LastActivity {

    public static class ActivityItem {

        private Long watched;

        private Long scrobble;

        private Long seen;

        private Long checkin;

        private Long collection;

        public Long getWatched() {
            return watched;
        }

        public Long getScrobble() {
            return scrobble;
        }

        public Long getSeen() {
            return seen;
        }

        public Long getCheckin() {
            return checkin;
        }

        public Long getCollection() {
            return collection;
        }
    }

    private Long all;

    private ActivityItem movie;

    private ActivityItem episode;

    public Long getAll() {
        return all;
    }

    public ActivityItem getMovie() {
        return movie;
    }

    public ActivityItem getEpisode() {
        return episode;
    }
}
