package net.simonvt.trakt.api.entity;

import java.util.List;

public class Movie {

    private String title;

    private Integer year;

    private Long released;

    private String url;

    private String trailer;

    private Integer runtime;

    private String tagline;

    private String overview;

    private String certification;

    private String imdbId;

    private Long tmdbId;

    private Long rtId;

    private long lastUpdated;

    private String poster;

    private Images images;

    // TODO: private List<UserProfile> topWatchers;

    private Ratings ratings;

    private Stats stats;

    // TODO: private List<Person> people;

    private List<String> genres;

    private Boolean watched;

    private Integer plays;

    private Boolean rating;

    private Integer ratingAdvanced;

    private Boolean inWatchlist;

    private Boolean inCollection;
}
