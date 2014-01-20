package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.body.CheckinBody;
import net.simonvt.cathode.api.body.MoviesBody;
import net.simonvt.cathode.api.body.ScrobbleBody;
import net.simonvt.cathode.api.body.WatchingBody;
import net.simonvt.cathode.api.entity.CheckinResponse;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Movie;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.entity.Stats;
import net.simonvt.cathode.api.entity.UserProfile;
import net.simonvt.cathode.api.entity.WatchingResponse;
import net.simonvt.cathode.api.enumeration.CommentType;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface MovieService {

  @GET("/movie/cancelcheckin/{apikey}") Response cancelCheckin();

  @GET("/movie/cancelwatching/{apikey}") Response cancelWatching();

  @POST("/movie/checkin/{apikey}") CheckinResponse checkin(@Body CheckinBody body);

  @GET("/movie/comments.json/{apikey}/{tmdbId}") List<Comment> comments(@Path("tmdbId") int tmdbId);

  @GET("/movie/comments.json/{apikey}/{tmdbId}/{type}")
  List<Comment> comments(@Path("tmdbId") int tmdbId, @Path("type") CommentType type);

  @POST("/movie/scrobble/{apikey}") CheckinResponse scrobble(@Body ScrobbleBody body);

  @POST("/movie/seen/{apikey}") Response seen(@Body MoviesBody movies);

  @POST("/movie/library/{apikey}") Response library(@Body MoviesBody movies);

  @GET("/movie/related.json/{apikey}/{tmdbId}") List<Movie> related(@Path("tmdbId") long tmdbId);

  @GET("/movie/related.json/{apikey}/{tmdbId}/{hidewatched}")
  List<Movie> related(@Path("tmdbId") long tmdbId, @Path("hidewatched") boolean hideWatched);

  @GET("/movie/stats.json/{apikey}/{tmdbId}") Stats stats(@Path("tmdbId") long tmdbId);

  @GET("/movie/summary.json/{apikey}/{tmdbId}/{detailLevel}")
  Movie summary(@Path("tmdbId") Long tmdbId, @Path("detailLevel") DetailLevel detailLevel);

  @GET("/movie/summaries.json/{apikey}/{tmdbIds}")
  List<Movie> summaries(@Path("tmdbIds") String tmdbIds);

  @GET("/movie/summaries.json/{apikey}/{tmdbIds}/{detailLevel}")
  List<Movie> summaries(@Path("tmdbIds") String tmdbIds,
      @Path("detailLevel") DetailLevel detailLevel);

  @POST("/movie/unlibrary/{apikey}") Response unlibrary(@Body MoviesBody movies);

  @POST("/movie/unseen/{apikey}") Response unseen(@Body MoviesBody movies);

  @POST("/movie/unwatchlist/{apikey}") Response unwatchlist(@Body MoviesBody movies);

  @POST("/movie/watching/{apikey}") WatchingResponse watching(@Body WatchingBody body);

  @GET("/movie/watchingnow.json/{apikey}/{tmdbId}")
  List<UserProfile> watchingNow(@Path("tmdbId") Long tmdbId);

  @POST("/movie/watchlist/{apikey}") Response watchlist(@Body MoviesBody movies);
}
