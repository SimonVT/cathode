package net.simonvt.cathode.provider.generated

import android.content.ContentValues
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.DatabaseHelper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.ShowColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.entity.ItemTypeString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShowTriggersTest {

  @Test
  fun deleteCommentsTrigger() {
    val helper = DatabaseHelper.getInstance(ApplicationProvider.getApplicationContext<Context>())
    val db = helper.writableDatabase

    // Create show
    val showValues = ContentValues()
    showValues.put(ShowColumns.TRAKT_ID, 1)
    val showId = db.insert(Tables.SHOWS, null, showValues)
    assertThat(showId).isAtLeast(0L)

    // Create comment
    val commentValues = ContentValues()
    commentValues.put(CommentColumns.ITEM_TYPE, ItemTypeString.SHOW)
    commentValues.put(CommentColumns.ITEM_ID, showId)
    commentValues.put(CommentColumns.COMMENT, "Comment")
    val commentId = db.insert(Tables.COMMENTS, null, commentValues)
    assertThat(commentId).isAtLeast(0L)

    db.delete(Tables.SHOWS, null, null)

    val comments = db.query(Tables.COMMENTS, null, null, null, null, null, null, null)
    assertThat(comments.count).isEqualTo(0L)
  }
}
