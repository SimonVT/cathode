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

package net.simonvt.cathode.ui.comments

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.data.MappedCursorLiveData
import net.simonvt.cathode.entity.Comment
import net.simonvt.cathode.entitymapper.CommentListMapper
import net.simonvt.cathode.entitymapper.CommentMapper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.ProviderSchematic.Comments

class CommentsViewModel(application: Application) : AndroidViewModel(application) {

  private var itemType: ItemType? = null
  private var itemId: Long = 0

  lateinit var comments: LiveData<List<Comment>>

  fun setItemTypeAndId(itemType: ItemType, itemId: Long) {
    if (this.itemType == null) {
      this.itemType = itemType
      this.itemId = itemId

      val uri: Uri
      when (itemType) {
        ItemType.SHOW -> uri = Comments.fromShow(itemId)
        ItemType.EPISODE -> uri = Comments.fromEpisode(itemId)
        ItemType.MOVIE -> uri = Comments.fromMovie(itemId)
        else -> throw IllegalArgumentException("Type $itemType not supported")
      }

      comments = MappedCursorLiveData(
        getApplication(),
        uri,
        CommentMapper.projection,
        CommentColumns.PARENT_ID + "=0",
        null,
        CommentColumns.IS_USER_COMMENT + " DESC, " + CommentColumns.LIKES + " DESC, " + CommentColumns.CREATED_AT + " DESC",
        CommentListMapper
      )
    }
  }
}
