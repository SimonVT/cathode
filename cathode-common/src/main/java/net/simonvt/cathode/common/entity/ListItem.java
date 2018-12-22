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

package net.simonvt.cathode.common.entity;

import net.simonvt.cathode.api.enumeration.ItemType;

public class ListItem {

  private long listItemId;

  private long listId;

  private ItemType type;

  private Movie movie;

  private Show show;

  private Season season;

  private Episode episode;

  private Person person;

  public ListItem(long listItemId, long listId, Movie movie) {
    this.listItemId = listItemId;
    this.listId = listId;
    this.movie = movie;
    type = ItemType.MOVIE;
  }

  public ListItem(long listItemId, long listId, Show show) {
    this.listItemId = listItemId;
    this.listId = listId;
    this.show = show;
    type = ItemType.SHOW;
  }

  public ListItem(long listItemId, long listId, Season season) {
    this.listItemId = listItemId;
    this.listId = listId;
    this.season = season;
    type = ItemType.SEASON;
  }

  public ListItem(long listItemId, long listId, Episode episode) {
    this.listItemId = listItemId;
    this.listId = listId;
    this.episode = episode;
    type = ItemType.EPISODE;
  }

  public ListItem(long listItemId, long listId, Person person) {
    this.listItemId = listItemId;
    this.listId = listId;
    this.person = person;
    type = ItemType.PERSON;
  }

  public long getListItemId() {
    return listItemId;
  }

  public long getListId() {
    return listId;
  }

  public ItemType getType() {
    return type;
  }

  public Movie getMovie() {
    return movie;
  }

  public Show getShow() {
    return show;
  }

  public Season getSeason() {
    return season;
  }

  public Episode getEpisode() {
    return episode;
  }

  public Person getPerson() {
    return person;
  }
}
