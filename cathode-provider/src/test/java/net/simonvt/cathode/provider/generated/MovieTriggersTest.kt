package net.simonvt.cathode.provider.generated

import android.content.ContentValues
import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.DatabaseHelper
import net.simonvt.cathode.provider.DatabaseContract.CommentColumns
import net.simonvt.cathode.provider.DatabaseContract.MovieColumns
import net.simonvt.cathode.provider.DatabaseSchematic.Tables
import net.simonvt.cathode.provider.entity.ItemTypeString
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@RunWith(RobolectricTestRunner::class)
class MovieTriggersTest {

  @Test
  fun deleteCommentsTrigger() {
    val helper = DatabaseHelper.getInstance(RuntimeEnvironment.application)
    val db = helper.writableDatabase

    // Create movie
    val movieValues = ContentValues()
    movieValues.put(MovieColumns.TRAKT_ID, 1)
    val movieId = db.insert(Tables.MOVIES, null, movieValues)
    assertThat(movieId).isAtLeast(0L)

    // Create comment
    val commentValues = ContentValues()
    commentValues.put(CommentColumns.ITEM_TYPE, ItemTypeString.MOVIE)
    commentValues.put(CommentColumns.ITEM_ID, movieId)
    commentValues.put(CommentColumns.COMMENT, "Comment")
    val commentId = db.insert(Tables.COMMENTS, null, commentValues)
    assertThat(commentId).isAtLeast(0L)

    db.delete(Tables.MOVIES, null, null)

    val comments = db.query(Tables.COMMENTS, null, null, null, null, null, null, null)
    assertThat(comments.count).isEqualTo(0L)
  }
}
