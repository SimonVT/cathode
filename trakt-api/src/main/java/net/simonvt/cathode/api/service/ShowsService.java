package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.entity.TvShow;
import net.simonvt.cathode.api.entity.UpdatedShows;
import retrofit.http.GET;
import retrofit.http.Path;

public interface ShowsService {

  @GET("/shows/trending.json/{apikey}") List<TvShow> trending();

  @GET("/shows/updated.json/{apikey}/{since}") UpdatedShows updated(@Path("since") long since);
}
