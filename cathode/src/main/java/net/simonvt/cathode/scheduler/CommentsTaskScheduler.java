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

package net.simonvt.cathode.scheduler;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import javax.inject.Inject;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.SyncService;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.SeasonDatabaseHelper;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.remote.action.comments.AddCommentJob;
import net.simonvt.cathode.remote.action.comments.CommentReplyJob;
import net.simonvt.cathode.remote.action.comments.DeleteCommentJob;
import net.simonvt.cathode.remote.action.comments.LikeCommentJob;
import net.simonvt.cathode.remote.action.comments.UnlikeCommentJob;
import net.simonvt.cathode.remote.action.comments.UpdateCommentJob;
import net.simonvt.schematic.Cursors;

public class CommentsTaskScheduler extends BaseTaskScheduler {

  @Inject SyncService syncService;

  @Inject ShowDatabaseHelper showHelper;
  @Inject SeasonDatabaseHelper seasonHelper;
  @Inject EpisodeDatabaseHelper episodeHelper;

  public CommentsTaskScheduler(Context context) {
    super(context);
  }

  public void comment(final ItemType type, final long showId, final String comment,
      final boolean spoiler) {
    execute(new Runnable() {
      @Override public void run() {
        final long traktId = showHelper.getTraktId(showId);
        queue(new AddCommentJob(type, traktId, comment, spoiler));
      }
    });
  }

  public void updateComment(final long commentId, final String comment, final boolean spoiler) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new UpdateCommentJob(commentId, comment, spoiler));

        ContentValues values = new ContentValues();
        values.put(CommentColumns.COMMENT, comment);
        values.put(CommentColumns.SPOILER, spoiler);
        context.getContentResolver().update(Comments.withId(commentId), values, null, null);
      }
    });
  }

  public void deleteComment(final long commentId) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new DeleteCommentJob(commentId));
        context.getContentResolver().delete(Comments.withId(commentId), null, null);
      }
    });
  }

  public void reply(final long parentId, final String comment, final boolean spoiler) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new CommentReplyJob(parentId, comment, spoiler));
      }
    });
  }

  public void like(final long commentId) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new LikeCommentJob(commentId));

        Cursor c = context.getContentResolver().query(Comments.withId(commentId), new String[] {
            CommentColumns.LIKES,
        }, null, null, null);
        if (c.moveToFirst()) {
          final int likes = Cursors.getInt(c, CommentColumns.LIKES);

          ContentValues values = new ContentValues();
          values.put(CommentColumns.LIKED, true);
          values.put(CommentColumns.LIKES, likes + 1);
          context.getContentResolver().update(Comments.withId(commentId), values, null, null);
        }
        c.close();
      }
    });
  }

  public void unlike(final long commentId) {
    execute(new Runnable() {
      @Override public void run() {
        queue(new UnlikeCommentJob(commentId));

        Cursor c = context.getContentResolver().query(Comments.withId(commentId), new String[] {
            CommentColumns.LIKES,
        }, null, null, null);
        if (c.moveToFirst()) {
          final int likes = Cursors.getInt(c, CommentColumns.LIKES);

          ContentValues values = new ContentValues();
          values.put(CommentColumns.LIKED, false);
          values.put(CommentColumns.LIKES, likes - 1);
          context.getContentResolver().update(Comments.withId(commentId), values, null, null);
        }
        c.close();
      }
    });
  }
}
