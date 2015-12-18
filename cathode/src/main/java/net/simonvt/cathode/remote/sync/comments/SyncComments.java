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
import android.content.OperationApplicationException;
import android.database.Cursor;
import android.os.RemoteException;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.api.entity.Comment;
import net.simonvt.cathode.api.entity.Profile;
import net.simonvt.cathode.api.enumeration.Extended;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.api.service.CommentsService;
import net.simonvt.cathode.api.service.EpisodeService;
import net.simonvt.cathode.api.service.MoviesService;
import net.simonvt.cathode.api.service.ShowsService;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobFailedException;
import net.simonvt.cathode.provider.CommentsHelper;
import net.simonvt.cathode.provider.DatabaseContract;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.DatabaseSchematic.Tables;
import net.simonvt.cathode.provider.EpisodeDatabaseHelper;
import net.simonvt.cathode.provider.MovieDatabaseHelper;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;
import net.simonvt.cathode.provider.ShowDatabaseHelper;
import net.simonvt.cathode.provider.UserDatabaseHelper;
import net.simonvt.cathode.provider.generated.CathodeProvider;
import net.simonvt.cathode.remote.PagedCallJob;
import net.simonvt.cathode.util.Cursors;
import retrofit.Call;
import timber.log.Timber;

public class SyncComments extends PagedCallJob<Comment> {

  private static final int LIMIT = 100;

  @Inject transient ShowsService showsService;
  @Inject transient EpisodeService episodeService;
  @Inject transient MoviesService moviesService;
  @Inject transient CommentsService commentsService;

  @Inject transient ShowDatabaseHelper showHelper;
  @Inject transient EpisodeDatabaseHelper episodeHelper;
  @Inject transient MovieDatabaseHelper movieHelper;
  @Inject transient UserDatabaseHelper usersHelper;

  private ItemType type;

  private long traktId;

  private int season;

  private int episode;

  public SyncComments(ItemType type, long traktId) {
    this.type = type;
    this.traktId = traktId;
  }

  public SyncComments(ItemType type, long traktId, int season, int episode) {
    this.type = type;
    this.traktId = traktId;
    this.season = season;
    this.episode = episode;
  }

  @Override public String key() {
    if (type == ItemType.EPISODE) {
      return "SyncComments&type="
          + type.toString()
          + "&traktId="
          + traktId
          + "&season="
          + season
          + "&episode="
          + episode;
    } else {
      return "SyncComments&type=" + type.toString() + "&traktId=" + traktId;
    }
  }

  @Override public int getPriority() {
    return PRIORITY_EXTRAS;
  }

  @Override public Call<List<Comment>> getCall(int page) {
    switch (type) {
      case SHOW:
        return showsService.getComments(traktId, page, LIMIT, Extended.FULL_IMAGES);

      case EPISODE:
        return episodeService.getComments(traktId, season, episode, page, LIMIT,
            Extended.FULL_IMAGES);

      case MOVIE:
        return moviesService.getComments(traktId, page, LIMIT, Extended.FULL_IMAGES);

      case COMMENT:
        return commentsService.getReplies(traktId, page, LIMIT, Extended.FULL_IMAGES);

      default:
        throw new RuntimeException("Unknown type: " + type);
    }
  }

  @Override public void handleResponse(List<Comment> comments) {
    int itemType;
    long itemId;
    switch (type) {
      case SHOW:
        itemType = DatabaseContract.ItemType.SHOW;
        itemId = showHelper.getId(traktId);
        break;

      case EPISODE:
        itemType = DatabaseContract.ItemType.EPISODE;
        itemId = episodeHelper.getId(traktId);
        break;

      case MOVIE:
        itemType = DatabaseContract.ItemType.MOVIE;
        itemId = movieHelper.getId(traktId);
        break;

      case COMMENT:
        Cursor c = null;
        try {
          c = getContentResolver().query(Comments.withId(traktId), new String[] {
              CommentColumns.ITEM_TYPE, CommentColumns.ITEM_ID
          }, null, null, null);
          if (c.moveToFirst()) {
            itemType = Cursors.getInt(c, CommentColumns.ITEM_TYPE);
            itemId = Cursors.getLong(c, CommentColumns.ITEM_ID);
          } else {
            return;
          }
        } finally {
          if (c != null) {
            c.close();
          }
        }
        break;

      default:
        throw new RuntimeException("Unknown type: " + type);
    }

    ArrayList<ContentProviderOperation> ops = new ArrayList<>();
    List<Long> existingComments = new ArrayList<>();
    List<Long> deleteComments = new ArrayList<>();
    List<Job> jobs = new ArrayList<>();

    if (type == ItemType.COMMENT) {
      Cursor c = getContentResolver().query(Comments.withParent(traktId), new String[] {
          Tables.COMMENTS + "." + CommentColumns.ID,
      }, null, null, null);
      while (c.moveToNext()) {
        final long id = c.getLong(c.getColumnIndex(CommentColumns.ID));
        existingComments.add(id);
        deleteComments.add(id);
      }
      c.close();
    } else {
      Cursor c = getContentResolver().query(Comments.COMMENTS, new String[] {
          CommentColumns.ID,
      }, CommentColumns.ITEM_TYPE + "=? AND " + CommentColumns.ITEM_ID + "=?", new String[] {
          String.valueOf(itemType), String.valueOf(itemId),
      }, null);
      while (c.moveToNext()) {
        final long id = c.getLong(c.getColumnIndex(CommentColumns.ID));
        existingComments.add(id);
        deleteComments.add(id);
      }
      c.close();
    }

    for (Comment comment : comments) {
      Profile profile = comment.getUser();
      UserDatabaseHelper.IdResult idResult = usersHelper.updateOrCreate(profile);
      final long userId = idResult.id;

      ContentValues values = CommentsHelper.getValues(comment);
      values.put(CommentColumns.USER_ID, userId);

      values.put(CommentColumns.ITEM_TYPE, itemType);
      values.put(CommentColumns.ITEM_ID, itemId);

      final long commentId = comment.getId();
      boolean exists = existingComments.contains(commentId);
      if (!exists) {
        // May have been created by user likes
        Cursor c = getContentResolver().query(Comments.withId(commentId), new String[] {
            CommentColumns.ID,
        }, null, null, null);
        exists = c.moveToFirst();
        c.close();
      }

      if (exists) {
        deleteComments.remove(commentId);
        ContentProviderOperation.Builder op =
            ContentProviderOperation.newUpdate(Comments.withId(commentId)).withValues(values);
        ops.add(op.build());
      } else {
        // The same comment can exist multiple times in the result from Trakt, so any comments we
        // insert are added to the list of existing comments.
        existingComments.add(commentId);
        ContentProviderOperation.Builder op =
            ContentProviderOperation.newInsert(Comments.COMMENTS).withValues(values);
        ops.add(op.build());
      }

      if (comment.getReplies() > 0) {
        jobs.add(new SyncComments(ItemType.COMMENT, commentId));
      }
    }

    for (Long id : deleteComments) {
      ContentProviderOperation.Builder op = ContentProviderOperation.newDelete(Comments.withId(id));
      ops.add(op.build());
    }

    try {
      getContentResolver().applyBatch(CathodeProvider.AUTHORITY, ops);
    } catch (RemoteException e) {
      Timber.e(e, "Updating comments failed");
      throw new JobFailedException(e);
    } catch (OperationApplicationException e) {
      Timber.e(e, "Updating comments failed");
      throw new JobFailedException(e);
    }

    for (Job job : jobs) {
      queue(job);
    }
  }
}
