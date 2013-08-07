package net.simonvt.trakt.api.service;

import net.simonvt.trakt.api.body.MoviesBody;
import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface MovieService {

  @GET("/movie/cancelcheckin/{apikey}") TraktResponse cancelCheckin();

  @GET("/movie/cancelwatching/{apikey}") TraktResponse cancelWatching();

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

  @POST("/movie/seen/{apikey}") TraktResponse seen(@Body MoviesBody movies);

  @POST("/movie/library/{apikey}") TraktResponse library(@Body MoviesBody movies);

  //    movie/related
  //            GET

  @GET("/movie/summary.json/{apikey}/{tmdbId}/{detailLevel}")
  Movie summary(@Path("tmdbId") Long tmdbId, @Path("detailLevel") DetailLevel detailLevel);

  @POST("/movie/unlibrary/{apikey}") TraktResponse unlibrary(@Body MoviesBody movies);

  @POST("/movie/unseen/{apikey}") TraktResponse unseen(@Body MoviesBody movies);

  @POST("/movie/unwatchlist/{apikey}") TraktResponse unwatchlist(@Body MoviesBody movies);

  //    movie/watching
  //            POSTDEV
  //    movie/watchingnow
  //            GET

  @POST("/movie/watchlist/{apikey}") TraktResponse watchlist(@Body MoviesBody movies);
}
