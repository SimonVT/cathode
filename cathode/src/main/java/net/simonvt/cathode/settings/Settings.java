/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.settings;

import android.preference.PreferenceActivity;

public class Settings extends PreferenceActivity {

  public static final String SHOW_HIDDEN = "showHidden";

  public static final String SHOWS_LAST_UPDATED = "showsLastUpdated";

  public static final String MOVIES_LAST_UPDATED = "showsLastUpdated";

  public static final String VERSION_CODE = "versionCode";

  public static final String ACTIVITY_STREAM_SYNC = "activityStreamSync";
  public static final String FULL_SYNC = "fullSync";

  // For saving timestamps returned by user/lastactivity
  public static final String ALL = "allActivity";

  public static final String EPISODE_WATCHED = "episodeWatched";
  public static final String EPISODE_SCROBBLE = "episodeScrobble";
  public static final String EPISODE_SEEN = "episodeSeen";
  public static final String EPISODE_CHECKIN = "episodeCheckin";
  public static final String EPISODE_COLLECTION = "episodeCollection";
  public static final String EPISODE_RATING = "episodeRating";
  public static final String EPISODE_WATCHLIST = "episodeWatchlist";
  public static final String EPISODE_COMMENT = "episodeComment";
  public static final String EPISODE_REVIEW = "episodeReview";
  public static final String EPISODE_SHOUT = "episodeShout";

  public static final String SHOW_RATING = "showRating";
  public static final String SHOW_WATCHLIST = "showWatchlist";
  public static final String SHOW_COMMENT = "showComment";
  public static final String SHOW_REVIEW = "showReview";
  public static final String SHOW_SHOUT = "showShout";

  public static final String MOVIE_WATCHED = "movieWatched";
  public static final String MOVIE_SCROBBLE = "movieScrobble";
  public static final String MOVIE_SEEN = "movieSeen";
  public static final String MOVIE_CHECKIN = "movieCheckin";
  public static final String MOVIE_COLLECTION = "movieCollection";
  public static final String MOVIE_RATING = "movieRating";
  public static final String MOVIE_WATCHLIST = "movieWatchlist";
  public static final String MOVIE_COMMENT = "movieComment";
  public static final String MOVIE_REVIEW = "movieReview";
  public static final String MOVIE_SHOUT = "movieShout";

  // Last trending and recommendations sync
  public static final String TRENDING = "trending";
  public static final String RECOMMENDATIONS = "recommendations";

  // Whether the initial sync has been performed
  public static final String INITIAL_SYNC = "initial Sync";

  // Sorting options
  public static final String SORT_SHOW_UPCOMING = "sortShowUpcoming";
  public static final String SORT_SHOW_SEARCH = "sortShowSearch";
  public static final String SORT_SHOW_TRENDING = "sortShowTrending";
  public static final String SORT_SHOW_RECOMMENDED = "sortShowRecommended";

  public static final String SORT_MOVIE_SEARCH = "sortMovieSearch";
  public static final String SORT_MOVIE_TRENDING = "sortMovieTrending";
  public static final String SORT_MOVIE_RECOMMENDED = "sortMovieRecommended";

  // User settings returned by account/settings
  public static final String PROFILE_FULL_NAME = "profileFullName";
  public static final String PROFILE_GENDER = "profileGender";
  public static final String PROFILE_AGE = "profileAge";
  public static final String PROFILE_LOCATION = "profileLocation";
  public static final String PROFILE_ABOUT = "profileAbout";
  public static final String PROFILE_JOINED = "profileJoined";
  public static final String PROFILE_LAST_LOGIN = "profileLastLogin";
  public static final String PROFILE_AVATAR = "profileAvatar";
  public static final String PROFILE_URL = "profileUrl";
  public static final String PROFILE_VIP = "profileVip";

  public static final String PROFILE_TIMEZONE = "profileTimezone";
  public static final String PROFILE_USE_24_HOUR = "profileUse24hour";

  public static final String PROFILE_PROTECTED = "profileProtected";

  public static final String PROFILE_RATING_MODE = "profileRatingMode";

  public static final String PROFILE_SHOUTS_SHOW_BADGES = "profileShowBadges";
  public static final String PROFILE_SHOUTS_SHOW_SPOILERS = "profileShowSpoilers";

  public static final String PROFILE_CONNECTION_FACEBOOK = "profileConnectionFacebook";
  public static final String PROFILE_CONNECTION_TWITTER = "profileConnectionTwitter";
  public static final String PROFILE_CONNECTION_TUMBLR = "profileConnectionTumblr";
  public static final String PROFILE_CONNECTION_PATH = "profileConnectionPath";
  public static final String PROFILE_CONNECTION_PROWL = "profileConnectionProwl";

  public static final String PROFILE_SHARING_TEXT_WATCHING = "profileSharingTextWatching";
  public static final String PROFILE_SHARING_TEXT_WATCHED = "profileSharingTextWatched";
}
