package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.entity.CalendarDate;
import retrofit.http.GET;
import retrofit.http.Path;

public interface CalendarService {

  @GET("/calendar/premieres.json/{apikey}") List<CalendarDate> premieres();

  @GET("/calendar/premieres.json/{apikey}/{date}/{days}")
  List<CalendarDate> premieres(@Path("date") String date, @Path("days") int days);

  @GET("/calendar/shows.json/{apikey}") List<CalendarDate> shows();

  @GET("/calendar/shows.json/{apikey}/{date}/{days}")
  List<CalendarDate> shows(@Path("date") String date, @Path("days") int days);
}
