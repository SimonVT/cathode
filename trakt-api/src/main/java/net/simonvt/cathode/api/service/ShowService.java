package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.body.CheckinBody;
import net.simonvt.cathode.api.body.ScrobbleBody;
import net.simonvt.cathode.api.body.SeasonBody;
import net.simonvt.cathode.api.body.ShowBody;
import net.simonvt.cathode.api.body.ShowEpisodeBody;
import net.simonvt.cathode.api.body.ShowWatchingBody;
import net.simonvt.cathode.api.body.ShowsBody;
import net.simonvt.cathode.api.entity.CheckinResponse;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Episode;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.entity.Season;
import net.simonvt.cathode.api.entity.TvEntity;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.entity.UserProfile;
import net.simonvt.cathode.api.enumeration.CommentType;
import net.simonvt.cathode.api.enumeration.DetailLevel;
import retrofit.http.Body;
import retrofit.http.GET;
import retrofit.http.POST;
import retrofit.http.Path;

public interface ShowService {

  @GET("/show/cancelcheckin/{apikey}") Response cancelCheckin();

  @GET("/show/cancelwatching/{apikey}") Response cancelWatching();

  @POST("/show/checkin/{apikey}") CheckinResponse checkin(@Body CheckinBody checkinBody);

  @GET("/show/comments.json/{apikey}/{title}/{type}")
  List<Comment> comments(@Path("title") String title, @Path("type") CommentType type);

  @GET("/show/episode/comments.json/{apikey}/{title}/{season}/{episode}/{type}")
  List<Comment> episodeComments(@Path("title") String title, @Path("season") Integer season,
      @Path("episode") Integer episode, @Path("type") CommentType type);

  @POST("/show/episode/library/{apikey}") Response episodeLibrary(@Body ShowEpisodeBody body);

  @POST("/show/episode/seen/{apikey}") Response episodeSeen(@Body ShowEpisodeBody body);

  @GET("/show/episode/summary.json/{apikey}/{tvdbid}/{season}/{episode}")
  TvEntity episodeSummary(@Path("tvdbid") int tvdbId, @Path("season") int season,
      @Path("episode") int episode);

  @POST("/show/episode/unlibrary/{apikey}") Response episodeUnlibrary(@Body ShowEpisodeBody body);

  @POST("/show/episode/unseen/{apikey}") Response episodeUnseen(@Body ShowEpisodeBody body);

  @POST("/show/episode/unwatchlist/{apikey}")
  Response episodeUnwatchlist(@Body ShowEpisodeBody body);

  @POST("/show/episode/watchlist/{apikey}") Response episodeWatchlist(@Body ShowEpisodeBody body);

  @GET("/show/episode/watchingnow.json/{apikey}/{title}/{season}/{episode}")
  List<UserProfile> episodeWatchingNow(@Path("title") String title, @Path("season") Integer season,
      @Path("episode") Integer episode);

  @POST("/show/library/{apikey}") Response library(@Body ShowBody shows);

  @GET("/show/related.json/{apikey}/{title}") List<TvShow> related();

  @GET("/show/scrobble/{apikey}") Response scrobble(@Body ScrobbleBody scrobble);

  @GET("/show/season.json/{apikey}/{tvdbid}/{season}")
  List<Episode> season(@Path("tvdbid") int tvdbId, @Path("season") int season);

  @POST("/show/season/library/{apikey}") Response seasonLibrary(@Body SeasonBody body);

  @POST("/show/season/seen/{apikey}") Response seasonSeen(@Body SeasonBody body);

  @GET("/show/seasons.json/{apikey}/{tvdbid}") List<Season> seasons(@Path("tvdbid") int tvdbId);

  @POST("/show/seen/{apikey}") Response seen(@Body ShowBody shows);

  @GET("/show/summary.json/{apikey}/{tvdbid}/{detail_level}")
  TvShow summary(@Path("tvdbid") int tvdbId, @Path("detail_level") DetailLevel detailLevel);

  @GET("/show/summary.json/{apikey}/{tvdbids}/{detail_level}")
  List<TvShow> summaries(@Path("tvdbids") String tvdbIds,
      @Path("detail_level") DetailLevel detailLevel);

  @POST("/show/unlibrary/{apikey}") Response unlibrary(@Body ShowBody shows);

  @POST("/show/unwatchlist/{apikey}") Response unwatchlist(@Body ShowsBody shows);

  @POST("/show/watching/{apikey}") Response wathing(@Body ShowWatchingBody body);

  @GET("/show/watchingnow.json/{apikey}/{title}")
  List<UserProfile> watchingNow(@Path("title") String title);

  @POST("/show/watchlist/{apikey}") Response watchlist(@Body ShowsBody shows);
}
