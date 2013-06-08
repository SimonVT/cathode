package net.simonvt.trakt.api.body;

import com.google.gson.annotations.SerializedName;

public class RateBody {

    private static final String TAG = "RateBody";

    @SerializedName("tvdb_id") private Long tvdbId;

    @SerializedName("tmdb_id") private Long tmdbId;

    private int episode;

    private int rating;

    public static class Builder {

        RateBody mBody = new RateBody();

        public Builder tvdbId(Long tvdbId) {
            mBody.tvdbId = tvdbId;
            return this;
        }

        public Builder tmdbId(Long tmdbId) {
            mBody.tmdbId = tmdbId;
            return this;
        }

        public Builder episode(int episode) {
            mBody.episode = episode;
            return this;
        }

        public Builder rating(int rating) {
            mBody.rating = rating;
            return this;
        }

        public RateBody build() {
            return mBody;
        }
    }
}
