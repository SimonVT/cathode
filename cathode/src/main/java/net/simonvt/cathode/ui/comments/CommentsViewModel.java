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

package net.simonvt.cathode.ui.comments;

import android.app.Application;
import android.net.Uri;
import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import java.util.List;
import net.simonvt.cathode.api.enumeration.ItemType;
import net.simonvt.cathode.common.data.MappedCursorLiveData;
import net.simonvt.cathode.common.entity.Comment;
import net.simonvt.cathode.entitymapper.CommentListMapper;
import net.simonvt.cathode.entitymapper.CommentMapper;
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns;
import net.simonvt.cathode.provider.ProviderSchematic.Comments;

public class CommentsViewModel extends AndroidViewModel {

  private ItemType itemType;
  private long itemId;

  private LiveData<List<Comment>> comments;

  public CommentsViewModel(@NonNull Application application) {
    super(application);
  }

  public void setItemTypeAndId(ItemType itemType, long itemId) {
    if (this.itemType == null) {
      this.itemType = itemType;
      this.itemId = itemId;

      Uri uri;
      switch (itemType) {
        case SHOW:
          uri = Comments.fromShow(itemId);
          break;

        case EPISODE:
          uri = Comments.fromEpisode(itemId);
          break;

        case MOVIE:
          uri = Comments.fromMovie(itemId);
          break;

        default:
          throw new IllegalArgumentException("Type " + itemType.toString() + " not supported");
      }

      comments = new MappedCursorLiveData<>(getApplication(), uri, CommentMapper.PROJECTION,
          CommentColumns.PARENT_ID + "=0", null, CommentColumns.IS_USER_COMMENT
          + " DESC, "
          + CommentColumns.LIKES
          + " DESC, "
          + CommentColumns.CREATED_AT
          + " DESC", new CommentListMapper());
    }
  }

  public LiveData<List<Comment>> getComments() {
    return comments;
  }
}
