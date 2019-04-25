package net.simonvt.cathode.provider.generated

import android.content.ContentValues
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.DatabaseHelper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.entity.ItemTypeString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class EpisodeTriggersTest {

  @Test
  fun deleteCommentsTrigger() {
    val helper = DatabaseHelper.getInstance(RuntimeEnvironment.application)
    val db = helper.writableDatabase

    // Create episode
    val episodeValues = ContentValues()
    episodeValues.put(EpisodeColumns.EPISODE, 1)
    val episodeId = db.insert(Tables.EPISODES, null, episodeValues)
    assertThat(episodeId).isAtLeast(0L)

    // Create comment
    val commentValues = ContentValues()
    commentValues.put(CommentColumns.ITEM_TYPE, ItemTypeString.EPISODE)
    commentValues.put(CommentColumns.ITEM_ID, episodeId)
    commentValues.put(CommentColumns.COMMENT, "Comment")
    val commentId = db.insert(Tables.COMMENTS, null, commentValues)
    assertThat(commentId).isAtLeast(0L)

    db.delete(Tables.EPISODES, null, null)

    val comments = db.query(Tables.COMMENTS, null, null, null, null, null, null, null)
    assertThat(comments.count).isEqualTo(0L)
  }
}
