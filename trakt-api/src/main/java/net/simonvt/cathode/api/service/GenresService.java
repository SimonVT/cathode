package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.entity.Genre;
import retrofit.http.GET;

public interface GenresService {

  @GET("/genres/movies.json/{apikey}") List<Genre> movies();

  @GET("/genres/shows.json/{apikey}") List<Genre> shows();
}
