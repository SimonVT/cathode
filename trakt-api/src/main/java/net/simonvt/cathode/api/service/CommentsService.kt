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

package net.simonvt.cathode.api.service

import net.simonvt.cathode.api.body.CommentBody
import net.simonvt.cathode.api.entity.Comment
import net.simonvt.cathode.api.enumeration.Extended
import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface CommentsService {

  @POST("/comments")
  fun post(@Body body: CommentBody): Call<Comment>

  @PUT("/comments/{id}")
  fun update(@Path("id") id: Long, @Body body: CommentBody): Call<Comment>

  @DELETE("/comments/{id}")
  fun delete(@Path("id") id: Long): Call<ResponseBody>

  @GET("/comment/{id}")
  fun comment(@Path("id") id: Long): Call<Comment>

  @GET("/comments/{id}/replies")
  fun replies(
    @Path("id") id: Long,
    @Query("page") page: Int,
    @Query("limit") limit: Int,
    @Query("extended") extended: Extended
  ): Call<List<Comment>>

  @POST("/comments/{id}/like")
  fun like(@Path("id") id: Long, @Body body: RequestBody): Call<ResponseBody>

  @DELETE("/comments/{id}/like")
  fun unlike(@Path("id") id: Long): Call<ResponseBody>

  @POST("/comments/{id}/replies")
  fun reply(@Path("id") id: Long, @Body body: CommentBody): Call<Comment>

  companion object {

    const val MIN_WORD_COUNT = 5
  }
}
