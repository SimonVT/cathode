/*
 * Copyright (C) 2018 Simon Vig Therkildsen
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

package net.simonvt.cathode.common.entity

data class Comment(
  val id: Long,
  val comment: String,
  val isSpoiler: Boolean,
  val isReview: Boolean,
  val parentId: Long,
  val createdAt: Long,
  val replies: Int,
  val likes: Int,
  val userRating: Int,
  val user: User,
  val isUserComment: Boolean,
  val isLiked: Boolean,
  val likedAt: Long,
  val lastSync: Long
)
