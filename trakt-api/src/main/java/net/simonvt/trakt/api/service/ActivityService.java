package net.simonvt.trakt.api.service;

import net.simonvt.trakt.api.entity.Activity;
import retrofit.http.GET;
import retrofit.http.Path;

public interface ActivityService {

  @GET("/activity/community.json/{apikey}") Activity community();

  @GET("/activity/community.json/{apikey}/{types}") Activity community(@Path("types") String types);

  @GET("/activity/community.json/{apikey}/{types}/{actions}")
  Activity community(@Path("types") String types, @Path("actions") String actions);

  @GET("/activity/community.json/{apikey}/{types}/{actions}/{startTs}")
  Activity community(@Path("types") String types, @Path("actions") String actions,
      @Path("startTs") long startTs);

  @GET("/activity/community.json/{apikey}/{types}/{actions}/{startTs}/{endTs}")
  Activity community(@Path("types") String types, @Path("actions") String actions,
      @Path("startTs") long startTs, @Path("endTs") long endTs);

  @GET("/activity/episodes.json/{apikey}/{title}/{seasons}/{episodes}")
  Activity episodes(@Path("title") String title, @Path("seasons") String seasons,
      @Path("episodes") String episodes);

  @GET("/activity/episodes.json/{apikey}/{title}/{seasons}/{episodes}/{actions}")
  Activity episodes(@Path("title") String title, @Path("seasons") String seasons,
      @Path("episodes") String episodes, @Path("actions") String actions);

  @GET("/activity/episodes.json/{apikey}/{title}/{seasons}/{episodes}/{actions}/{startTs}")
  Activity episodes(@Path("title") String title, @Path("seasons") String seasons,
      @Path("episodes") String episodes, @Path("actions") String actions,
      @Path("startTs") long startTs);

  @GET("/activity/episodes.json/{apikey}/{title}/{seasons}/{episodes}/{actions}/{startTs}/{endTs}")
  Activity episodes(@Path("title") String title, @Path("seasons") String seasons,
      @Path("episodes") String episodes, @Path("actions") String actions,
      @Path("startTs") long startTs, @Path("endTs") long endTs);

  @GET("/activity/friends.json/{apikey}") Activity friends();

  @GET("/activity/friends.json/{apikey}/{types}") Activity friends(@Path("types") String types);

  @GET("/activity/friends.json/{apikey}/{types}/{actions}")
  Activity friends(@Path("types") String types, @Path("actions") String actions);

  @GET("/activity/friends.json/{apikey}/{types}/{actions}/{startTs}")
  Activity friends(@Path("types") String types, @Path("actions") String actions,
      @Path("startTs") long startTs);

  @GET("/activity/friends.json/{apikey}/{types}/{actions}/{startTs}/{endTs}")
  Activity friends(@Path("types") String types, @Path("actions") String actions,
      @Path("startTs") long startTs, @Path("endTs") long endTs);

  @GET("/activity/movies.json/{apikey}/{title}") Activity movies(@Path("title") String title);

  @GET("/activity/movies.json/{apikey}/{title}/{actions}")
  Activity movies(@Path("title") String title, @Path("actions") String actions);

  @GET("/activity/movies.json/{apikey}/{title}/{actions}/{startTs}")
  Activity movies(@Path("title") String title, @Path("actions") String actions,
      @Path("startTs") long startTs);

  @GET("/activity/movies.json/{apikey}/{title}/{actions}/{startTs}/{endTs}")
  Activity movies(@Path("title") String title, @Path("actions") String actions,
      @Path("startTs") long startTs, @Path("endTs") long endTs);

  @GET("/activity/seasons.json/{apikey}/{title}/{seasons}")
  Activity seasons(@Path("title") String title, @Path("seasons") String seasons);

  @GET("/activity/seasons.json/{apikey}/{title}/{seasons}/{actions}")
  Activity seasons(@Path("title") String title, @Path("seasons") String seasons,
      @Path("actions") String actions);

  @GET("/activity/seasons.json/{apikey}/{title}/{seasons}/{actions}/{startTs}")
  Activity seasons(@Path("title") String title, @Path("seasons") String seasons,
      @Path("actions") String actions, @Path("startTs") long startTs);

  @GET("/activity/seasons.json/{apikey}/{title}/{seasons}/{actions}/{startTs}/{endTs}")
  Activity seasons(@Path("title") String title, @Path("seasons") String seasons,
      @Path("actions") String actions, @Path("startTs") long startTs, @Path("endTs") long endTs);

  @GET("/activity/shows.json/{apikey}/{title}") Activity shows(@Path("title") String title);

  @GET("/activity/shows.json/{apikey}/{title}/{actions}")
  Activity shows(@Path("title") String title, @Path("actions") String actions);

  @GET("/activity/shows.json/{apikey}/{title}/{actions}/{startTs}")
  Activity shows(@Path("title") String title, @Path("actions") String actions,
      @Path("startTs") long startTs);

  @GET("/activity/shows.json/{apikey}/{title}/{actions}/{startTs}/{endTs}")
  Activity shows(@Path("title") String title, @Path("actions") String actions,
      @Path("startTs") long startTs, @Path("endTs") long endTs);

  @GET("/activity/user.json/{apikey}/{username}") Activity user();

  @GET("/activity/user.json/{apikey}/{username}/{types}")
  Activity user(@Path("types") String types);

  @GET("/activity/user.json/{apikey}/{username}/{types}/{actions}")
  Activity user(@Path("types") String types, @Path("actions") String actions);

  @GET("/activity/user.json/{apikey}/{username}/{types}/{actions}/{startTs}")
  Activity user(@Path("types") String types, @Path("actions") String actions,
      @Path("startTs") long startTs);

  @GET("/activity/user.json/{apikey}/{username}/{types}/{actions}/{startTs}/{endTs}")
  Activity user(@Path("types") String types, @Path("actions") String actions,
      @Path("startTs") long startTs, @Path("endTs") long endTs);
}
