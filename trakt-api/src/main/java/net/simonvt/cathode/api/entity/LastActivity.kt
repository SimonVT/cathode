/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.entity

data class LastActivity(
  val movies: ActivityItem,
  val shows: ActivityItem,
  val seasons: ActivityItem,
  val episodes: ActivityItem,
  val comments: ActivityItem,
  val lists: ActivityItem
)

data class ActivityItem(
  val watched_at: IsoTime?,
  val collected_at: IsoTime?,
  val rated_at: IsoTime?,
  val watchlisted_at: IsoTime?,
  val commented_at: IsoTime?,
  val paused_at: IsoTime?,
  val hidden_at: IsoTime?,
  val liked_at: IsoTime?,
  val updated_at: IsoTime?
)
