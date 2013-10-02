package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.body.MoviesBody;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface MovieService {

  @GET("/movie/cancelcheckin/{apikey}") Response cancelCheckin();

  @GET("/movie/cancelwatching/{apikey}") Response cancelWatching();

  //    movie/cancelcheckin
  //            POSTDEV
  //    movie/cancelwatching
  //            POSTDEV
  //    movie/checkin
  //            POSTDEV
  //    movie/comments
  //            GET
  //    movie/scrobble
  //            POSTDEV

  @POST("/movie/seen/{apikey}") Response seen(@Body MoviesBody movies);

  @POST("/movie/library/{apikey}") Response library(@Body MoviesBody movies);

  //    movie/related
  //            GET

  @GET("/movie/summary.json/{apikey}/{tmdbId}/{detailLevel}")
  Movie summary(@Path("tmdbId") Long tmdbId, @Path("detailLevel") DetailLevel detailLevel);

  @POST("/movie/unlibrary/{apikey}") Response unlibrary(@Body MoviesBody movies);

  @POST("/movie/unseen/{apikey}") Response unseen(@Body MoviesBody movies);

  @POST("/movie/unwatchlist/{apikey}") Response unwatchlist(@Body MoviesBody movies);

  //    movie/watching
  //            POSTDEV
  //    movie/watchingnow
  //            GET

  @POST("/movie/watchlist/{apikey}") Response watchlist(@Body MoviesBody movies);
}
