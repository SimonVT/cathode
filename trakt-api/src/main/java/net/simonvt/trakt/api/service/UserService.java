package net.simonvt.trakt.api.service;

import java.util.List;
import net.simonvt.trakt.api.entity.CalendarDate;
import net.simonvt.trakt.api.entity.LastActivity;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
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

  //    user/list
  //            GET
  //    user/lists
  //            GET
  //    user/network/followers
  //            GET
  //    user/network/following
  //            GET
  //    user/network/friends
  //            GET
  //    user/profile
  //            GET
  //    user/progress/collected
  //            GET
  //    user/progress/watched
  //            GET
  //    user/ratings/episodes
  //            GET
  //    user/ratings/movies
  //            GET
  //    user/ratings/shows
  //            GET
  //    user/watching
  //            GET

  @GET("/user/watchlist/episodes.json/{apikey}/{username}") List<TvShow> watchlistEpisodes();

  @GET("/user/watchlist/movies.json/{apikey}/{username}") List<Movie> watchlistMovies();

  @GET("/user/watchlist/shows.json/{apikey}/{username}") List<TvShow> watchlistShows();
}
