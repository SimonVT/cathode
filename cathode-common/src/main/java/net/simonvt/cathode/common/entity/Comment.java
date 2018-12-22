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

package net.simonvt.cathode.common.entity;

public class Comment {

  private long id;
  private String comment;
  private boolean spoiler;
  private boolean review;
  private long parentId;
  private long createdAt;
  private int replies;
  private int likes;
  private int userRating;
  private User user;
  private boolean isUserComment;
  private boolean liked;
  private long likedAt;

  public Comment(long id, String comment, boolean spoiler, boolean review, long parentId,
      long createdAt, int replies, int likes, int userRating, User user, boolean isUserComment,
      boolean liked, long likedAt) {
    this.id = id;
    this.comment = comment;
    this.spoiler = spoiler;
    this.review = review;
    this.parentId = parentId;
    this.createdAt = createdAt;
    this.replies = replies;
    this.likes = likes;
    this.userRating = userRating;
    this.user = user;
    this.isUserComment = isUserComment;
    this.liked = liked;
    this.likedAt = likedAt;
  }

  public long getId() {
    return id;
  }

  public String getComment() {
    return comment;
  }

  public boolean isSpoiler() {
    return spoiler;
  }

  public boolean isReview() {
    return review;
  }

  public long getParentId() {
    return parentId;
  }

  public long getCreatedAt() {
    return createdAt;
  }

  public int getReplies() {
    return replies;
  }

  public int getLikes() {
    return likes;
  }

  public int getUserRating() {
    return userRating;
  }

  public User getUser() {
    return user;
  }

  public boolean isUserComment() {
    return isUserComment;
  }

  public boolean isLiked() {
    return liked;
  }

  public long getLikedAt() {
    return likedAt;
  }
}
