package net.simonvt.trakt.api.service;

import retrofit.http.GET;
import retrofit.http.Path;

import net.simonvt.trakt.api.entity.Movie;
import net.simonvt.trakt.api.enumeration.DetailLevel;

public interface MovieService {

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
    //    movie/seen
    //            POST
    //    movie/library
    //            POST
    //    movie/related
    //            GET

    @GET("/movie/summary.json/{apikey}/{tmdbId}/{detailLevel}")
    Movie summary(@Path("tmdbId") Long tmdbId, @Path("detailLevel") DetailLevel detailLevel);

    //    movie/summary
    //            GET
    //    movie/unlibrary
    //            POST
    //    movie/unseen
    //            POST
    //    movie/unwatchlist
    //            POST
    //    movie/watching
    //            POSTDEV
    //    movie/watchingnow
    //            GET
    //    movie/watchlist
    //            POST

}
