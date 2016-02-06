/*
 * Copyright (C) 2015 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.simonvt.cathode.api.service;

import java.util.List;
import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.enumeration.Extended;
import okhttp3.ResponseBody;
import retrofit2.Call;
import retrofit2.http.Body;
import retrofit2.http.DELETE;
import retrofit2.http.GET;
import retrofit2.http.POST;
import retrofit2.http.PUT;
import retrofit2.http.Path;
import retrofit2.http.Query;

public interface CommentsService {

  int MIN_WORD_COUNT = 5;

  @POST("/comments") Call<Comment> post(@Body CommentBody body);

  @PUT("/comments/{id}") Call<Comment> update(@Path("id") long id, @Body CommentBody body);

  @DELETE("/comments/{id}") Call<ResponseBody> delete(@Path("id") long id);

  @GET("/comments/{id}/replies") Call<List<Comment>> getReplies(@Path("id") long id,
      @Query("page") int page, @Query("limit") int limit, @Query("extended") Extended extended);

  @POST("/comments/{id}/like") Call<ResponseBody> like(@Path("id") long id, @Body String body);

  @DELETE("/comments/{id}/like") Call<ResponseBody> unlike(@Path("id") long id);

  @POST("/comments/{id}/replies") Call<Comment> reply(@Path("id") long id, @Body CommentBody body);
}
