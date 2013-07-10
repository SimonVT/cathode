package net.simonvt.trakt.api.entity;

public class LastActivity {

    public static class ActivityItem {

        private Long watched;

        private Long scrobble;

        private Long seen;

        private Long checkin;

        private Long collection;

        private Long rating;

        private Long watchlist;

        private Long comment;

        private Long review;

        private Long shout;

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

        public Long getRating() {
            return rating;
        }

        public Long getWatchlist() {
            return watchlist;
        }

        public Long getComment() {
            return comment;
        }

        public Long getReview() {
            return review;
        }

        public Long getShout() {
            return shout;
        }
    }

    private Long all;

    private ActivityItem movie;

    private ActivityItem show;

    private ActivityItem episode;

    public Long getAll() {
        return all;
    }

    public ActivityItem getMovie() {
        return movie;
    }

    public ActivityItem getShow() {
        return show;
    }

    public ActivityItem getEpisode() {
        return episode;
    }
}
