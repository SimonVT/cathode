package net.simonvt.trakt.api.service;

import java.util.List;
import net.simonvt.trakt.api.body.ShowBody;
import net.simonvt.trakt.api.body.ShowEpisodeBody;
import net.simonvt.trakt.api.body.ShowsBody;
import net.simonvt.trakt.api.entity.Episode;
import net.simonvt.trakt.api.entity.Season;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.entity.TvEntity;
import net.simonvt.trakt.api.entity.TvShow;
import net.simonvt.trakt.api.enumeration.DetailLevel;
import retrofit.RetrofitError;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface ShowService {

  @GET("/show/cancelcheckin/{apikey}") TraktResponse cancelCheckin(@Path("apikey") String apiKey)
      throws RetrofitError;

  @GET("/show/cancelwatching/{apikey}") TraktResponse cancelWatching(@Path("apikey") String apiKey)
      throws RetrofitError;

  // show/checkin
  // show/comments
  // show/episode/comments

  @POST("/show/episode/library/{apikey}") TraktResponse episodeLibrary(@Body ShowEpisodeBody body)
      throws RetrofitError;

  @POST("/show/episode/seen/{apikey}") TraktResponse episodeSeen(@Body ShowEpisodeBody body)
      throws RetrofitError;

  @GET("/show/episode/summary.json/{apikey}/{tvdbid}/{season}/{episode}")
  TvEntity episodeSummary(@Path("tvdbid") int tvdbId, @Path("season") int season,
      @Path("episode") int episode) throws RetrofitError;

  @POST("/show/episode/unlibrary/{apikey}")
  TraktResponse episodeUnlibrary(@Body ShowEpisodeBody body) throws RetrofitError;

  @POST("/show/episode/unseen/{apikey}") TraktResponse episodeUnseen(@Body ShowEpisodeBody body)
      throws RetrofitError;

  @POST("/show/episode/unwatchlist/{apikey}")
  TraktResponse episodeUnwatchlist(@Body ShowEpisodeBody body) throws RetrofitError;

  @POST("/show/episode/watchlist/{apikey}")
  TraktResponse episodeWatchlist(@Body ShowEpisodeBody body) throws RetrofitError;

  //    show/episode/watchingnow

  @POST("/show/library/{apikey}") TraktResponse library(@Body ShowBody shows);

  //    show/related
  //    show/scrobble

  @GET("/show/season.json/{apikey}/{tvdbid}/{season}")
  List<Episode> season(@Path("tvdbid") int tvdbId, @Path("season") int season) throws RetrofitError;

  //    show/season/library
  //    show/season/seen

  @GET("/show/seasons.json/{apikey}/{tvdbid}") List<Season> seasons(@Path("tvdbid") int tvdbId)
      throws RetrofitError;

  @POST("/show/seen/{apikey}") TraktResponse seen(@Body ShowBody shows);

  @GET("/show/summary.json/{apikey}/{tvdbid}/{detail_level}")
  TvShow summary(@Path("tvdbid") int tvdbId, @Path("detail_level") DetailLevel detailLevel)
      throws RetrofitError;

  @POST("/show/unlibrary/{apikey}") TraktResponse unlibrary(@Body ShowBody shows);

  @POST("/show/unwatchlist/{apikey}") TraktResponse unwatchlist(@Body ShowsBody shows);

  //    show/watching
  //    show/watchingnow

  @POST("/show/watchlist/{apikey}") TraktResponse watchlist(@Body ShowsBody shows);
}
