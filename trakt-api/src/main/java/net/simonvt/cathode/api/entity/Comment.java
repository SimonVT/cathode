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

package net.simonvt.cathode.api.entity;

public class Comment {

  private Long id;

  private String comment;

  private Boolean spoiler;

  private Boolean review;

  private Long parentId;

  private IsoTime createdAt;

  private Integer replies;

  private Integer likes;

  private Integer userRating;

  private Profile user;

  public Long getId() {
    return id;
  }

  public String getComment() {
    return comment;
  }

  public Boolean isSpoiler() {
    return spoiler;
  }

  public Boolean isReview() {
    return review;
  }

  public Long getParentId() {
    return parentId;
  }

  public IsoTime getCreatedAt() {
    return createdAt;
  }

  public Integer getReplies() {
    return replies;
  }

  public Integer getLikes() {
    return likes;
  }

  public Integer getUserRating() {
    return userRating;
  }

  public Profile getUser() {
    return user;
  }
}
