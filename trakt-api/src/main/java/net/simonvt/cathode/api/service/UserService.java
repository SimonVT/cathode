package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.entity.ActivityItem;
import net.simonvt.cathode.api.entity.CalendarDate;
import net.simonvt.cathode.api.entity.LastActivity;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.ProgressItem;
import net.simonvt.cathode.api.entity.RatingItem;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.entity.UserProfile;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import net.simonvt.cathode.api.enumeration.Rating;
import net.simonvt.cathode.api.enumeration.Sort;
import retrofit.http.GET;
import retrofit.http.Path;

public interface UserService {

  @GET("/user/calendar/shows.json/{apikey}/{username}/{date}/{days}")
  List<CalendarDate> calendarShows(@Path("date") String date, @Path("days") int days);

  @GET("/user/lastactivity.json/{apikey}/{username}") LastActivity lastActivity();

  @GET("/user/library/movies/all.json/{apikey}/{username}/{detailLevel}")
  List<Movie> moviesAll(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/library/movies/collection.json/{apikey}/{username}/{detailLevel}")
  List<Movie> moviesCollection(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/library/movies/watched.json/{apikey}/{username}/{detailLevel}")
  List<Movie> moviesWatched(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/library/shows/all.json/{apikey}/{username}/{detailLevel}")
  List<TvShow> libraryShowsAll(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/library/shows/collection.json/{apikey}/{username}/{detailLevel}")
  List<TvShow> libraryShowsCollection(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/library/shows/watched.json/{apikey}/{username}/{detailLevel}")
  List<TvShow> libraryShowsWatched(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/list.json/{apikey}/{username}/{slug}")
  net.simonvt.cathode.api.entity.List list(@Path("slug") String slug);

  @GET("/user/lists.json/{apikey}/{username}")
  List<net.simonvt.cathode.api.entity.List> lists();

  @GET("/user/network/followers.json/{apikey}/{username}")
  List<UserProfile> followers();

  @GET("/user/network/following.json/{apikey}/{username}")
  List<UserProfile> following();

  @GET("/user/network/friends.json/{apikey}/{username}")
  List<UserProfile> friends();

  @GET("/user/profile.json/{apikey}/{username}")
  UserProfile profile();

  @GET("/user/progress/collected.json/{apikey}/{username}/{title}/{sort}/{detailLevel}")
  List<ProgressItem> progressCollected(@Path("title") String title, @Path("sort") Sort sort,
      @Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/progress/watched.json/{apikey}/{username}/{title}/{sort}/{detailLevel}")
  List<ProgressItem> progressWatched(@Path("title") String title, @Path("sort") Sort sort,
      @Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/ratings/episodes.json/{apikey}/{username}/all/min")
  List<RatingItem> ratingsEpisodes();

  @GET("/user/ratings/episodes.json/{apikey}/{username}/{rating}/min")
  List<RatingItem> ratingsEpisodes(@Path("rating") Rating rating);

  @GET("/user/ratings/episodes.json/{apikey}/{username}/{rating}/{detailLevel}")
  List<RatingItem> ratingsEpisodes(@Path("rating") Rating rating,
      @Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/ratings/episodes.json/{apikey}/{username}/{rating}/min")
  List<RatingItem> ratingsEpisodes(@Path("rating") Integer rating);

  @GET("/user/ratings/episodes.json/{apikey}/{username}/{rating}/{detailLevel}")
  List<RatingItem> ratingsEpisodes(@Path("rating") Integer rating,
      @Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/ratings/episodes.json/{apikey}/{username}/all/{detailLevel}")
  List<RatingItem> ratingsEpisodes(@Path("detailLevel") DetailLevel detailLevel);

  @GET("/user/ratings/movies.json/{apikey}/{username}/all/min") List<RatingItem> ratingsMovies();

  @GET("/user/ratings/shows.json/{apikey}/{username}/all/min") List<RatingItem> ratingsShows();

  @GET("/user/watching.json/{apikey}/{username}") ActivityItem watching();

  @GET("/user/watchlist/episodes.json/{apikey}/{username}") List<TvShow> watchlistEpisodes();

  @GET("/user/watchlist/movies.json/{apikey}/{username}") List<Movie> watchlistMovies();

  @GET("/user/watchlist/shows.json/{apikey}/{username}") List<TvShow> watchlistShows();
}
