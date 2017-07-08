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

package net.simonvt.cathode.remote.sync.comments;

import android.content.ContentProviderOperation;
import android.content.ContentValues;
import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Like;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.enumeration.ItemTypes;
import net.simonvt.cathode.api.service.UsersService;
import net.simonvt.cathode.jobqueue.JobPriority;
import net.simonvt.cathode.provider.CommentsHelper;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.UserDatabaseHelper;
import net.simonvt.cathode.remote.Flags;
import net.simonvt.cathode.remote.PagedCallJob;
import net.simonvt.schematic.Cursors;
import retrofit2.Call;

public class SyncCommentLikes extends PagedCallJob<Like> {

  private static final int LIMIT = 100;

  @Inject transient UsersService usersService;
  @Inject transient UserDatabaseHelper userHelper;

  public SyncCommentLikes() {
    super(Flags.REQUIRES_AUTH);
  }

  @Override public String key() {
    return "SyncCommentLikes";
  }

  @Override public int getPriority() {
    return JobPriority.USER_DATA;
  }

  @Override public Call<List<Like>> getCall(int page) {
    return usersService.getLikes(ItemTypes.COMMENTS, page, LIMIT);
  }

  @Override public boolean handleResponse(List<Like> likes) {
    List<Long> existingLikes = new ArrayList<>();
    List<Long> deleteLikes = new ArrayList<>();
    Cursor c = getContentResolver().query(Comments.COMMENTS, new String[] {
        CommentColumns.ID,
    }, CommentColumns.LIKED + "=1", null, null);
    while (c.moveToNext()) {
      final long id = Cursors.getLong(c, CommentColumns.ID);
      existingLikes.add(id);
      deleteLikes.add(id);
    }
    c.close();

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();

    for (Like like : likes) {
      Comment comment = like.getComment();
      final long commentId = comment.getId();
      final long likedAt = like.getLikedAt().getTimeInMillis();

      boolean exists = existingLikes.contains(commentId);
      if (!exists) {
        // May have been created by user likes
        c = getContentResolver().query(Comments.withId(commentId), new String[] {
            CommentColumns.ID,
        }, null, null, null);
        exists = c.moveToFirst();
        c.close();
      }

      if (exists) {
        deleteLikes.remove(commentId);

        ContentProviderOperation.Builder op =
            ContentProviderOperation.newUpdate(Comments.withId(commentId))
                .withValue(CommentColumns.LIKED, true)
                .withValue(CommentColumns.LIKED_AT, likedAt)
                .withValue(CommentColumns.IS_USER_COMMENT, true);
        ops.add(op.build());
      } else {
        Profile profile = comment.getUser();
        UserDatabaseHelper.IdResult idResult = userHelper.updateOrCreate(profile);
        final long userId = idResult.id;

        ContentValues values = CommentsHelper.getValues(comment);
        values.put(CommentColumns.USER_ID, userId);

        values.put(CommentColumns.LIKED, true);
        values.put(CommentColumns.LIKED_AT, likedAt);
        values.put(CommentColumns.IS_USER_COMMENT, true);

        ContentProviderOperation.Builder op =
            ContentProviderOperation.newInsert(Comments.COMMENTS).withValues(values);
        ops.add(op.build());
        existingLikes.add(commentId);
      }
    }

    for (Long id : deleteLikes) {
      ContentProviderOperation.Builder op = ContentProviderOperation.newUpdate(Comments.withId(id));
      op.withValue(CommentColumns.LIKED, false);
      ops.add(op.build());
    }

    return applyBatch(ops);
  }
}
