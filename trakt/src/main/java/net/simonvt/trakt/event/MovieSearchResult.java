package net.simonvt.trakt.event;

import java.util.List;

public class MovieSearchResult {

    private List<Long> mMovieIds;

    public MovieSearchResult(List<Long> movieIds) {
        mMovieIds = movieIds;
    }

    public List<Long> getMovieIds() {
        return mMovieIds;
    }
}
