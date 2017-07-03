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

package net.simonvt.cathode.remote.action.comments;

import android.content.ContentValues;
import java.io.IOException;
import javax.inject.Inject;
import net.simonvt.cathode.jobs.R;
import net.simonvt.cathode.api.body.CommentBody;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.common.event.RequestFailedEvent;
import net.simonvt.cathode.provider.CommentsHelper;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.UserDatabaseHelper;
import net.simonvt.cathode.remote.CallJob;
import net.simonvt.cathode.remote.Flags;
import retrofit2.Call;
import retrofit2.Response;

public class AddCommentJob extends CallJob<Comment> {

  @Inject transient CommentsService commentsService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient SeasonDatabaseHelper seasonHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;
  @Inject transient MovieDatabaseHelper movieHelper;
  @Inject transient UserDatabaseHelper userHelper;

  private ItemType type;

  private long traktId;

  private String comment;

  private Boolean spoiler;

  public AddCommentJob(ItemType type, long traktId, String comment, boolean spoiler) {
    super(Flags.REQUIRES_AUTH);
    this.type = type;
    this.traktId = traktId;
    this.comment = comment;
    this.spoiler = spoiler;
  }

  @Override public String key() {
    return "AddCommentJob?type=" + type.toString() + "&traktId=" + traktId + "&comment=" + comment;
  }

  @Override public boolean allowDuplicates() {
    return true;
  }

  @Override public int getPriority() {
    return PRIORITY_ACTIONS;
  }

  @Override public Call<Comment> getCall() {
    CommentBody body = CommentBody.comment(comment);
    if (spoiler) {
      body.spoiler();
    }

    switch (type) {
      case SHOW:
        body.show(traktId);
        break;

      case EPISODE:
        body.episode(traktId);
        break;

      case MOVIE:
        body.movie(traktId);
        break;

      default:
        throw new IllegalStateException("Unknown type: " + type);
    }

    return commentsService.post(body);
  }

  @Override protected boolean isError(Response<Comment> response) throws IOException {
    final int statusCode = response.code();
    if (statusCode == 422) {
      RequestFailedEvent.post(R.string.comment_submit_error);
      return false;
    }

    return super.isError(response);
  }

  @Override public boolean handleResponse(Comment comment) {
    ContentValues values = CommentsHelper.getValues(comment);
    values.put(CommentColumns.IS_USER_COMMENT, true);

    Profile profile = comment.getUser();
    UserDatabaseHelper.IdResult result = userHelper.updateOrCreate(profile);
    final long profileId = result.id;
    values.put(CommentColumns.USER_ID, profileId);

    switch (type) {
      case SHOW:
        final long showId = showHelper.getId(traktId);
        values.put(CommentColumns.ITEM_TYPE, DatabaseContract.ItemType.SHOW);
        values.put(CommentColumns.ITEM_ID, showId);
        break;

      case EPISODE:
        final long episodeId = episodeHelper.getId(traktId);
        values.put(CommentColumns.ITEM_TYPE, DatabaseContract.ItemType.EPISODE);
        values.put(CommentColumns.ITEM_ID, episodeId);
        break;

      case MOVIE:
        final long movieId = movieHelper.getId(traktId);
        values.put(CommentColumns.ITEM_TYPE, DatabaseContract.ItemType.MOVIE);
        values.put(CommentColumns.ITEM_ID, movieId);
        break;
    }

    getContentResolver().insert(Comments.COMMENTS, values);

    return true;
  }
}
