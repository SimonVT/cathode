package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Response;
import retrofit.http.Body;
import retrofit.http.POST;

public interface CommentService {

  @POST("/comment/episode.json/{apikey}") Response episode(@Body CommentBody comment);

  @POST("/comment/movie.json/{apikey}") Response movie(@Body CommentBody comment);

  @POST("/comment/show.json/{apikey}") Response show(@Body CommentBody comment);
}
