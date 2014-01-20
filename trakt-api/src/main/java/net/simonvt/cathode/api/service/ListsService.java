package net.simonvt.cathode.api.service;

import net.simonvt.cathode.api.body.DeleteListBody;
import net.simonvt.cathode.api.body.ListBody;
import net.simonvt.cathode.api.body.ListItemBody;
import net.simonvt.cathode.api.entity.ListItemResponse;
import net.simonvt.cathode.api.entity.ListResponse;
import net.simonvt.cathode.api.entity.Response;
import retrofit.http.Body;
import retrofit.http.POST;

public interface ListsService {

  @POST("/lists/add/{apikey}") ListResponse add(@Body ListBody body);

  @POST("/lists/delete/{apikey}") Response delete(@Body DeleteListBody body);

  @POST("/lists/items/add/{apikey}") ListItemResponse addItem(@Body ListItemBody body);

  @POST("/lists/items/delete/{apikey}") ListItemResponse deleteItem(@Body ListItemBody body);

  @POST("/lists/add/{apikey}") ListResponse update(@Body ListBody body);
}
