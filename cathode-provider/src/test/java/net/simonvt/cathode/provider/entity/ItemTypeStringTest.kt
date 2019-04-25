package net.simonvt.cathode.provider.entity

import com.google.common.truth.Truth.assertThat
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.enumeration.ItemType.COMMENT
import net.simonvt.cathode.api.enumeration.ItemType.EPISODE
import net.simonvt.cathode.api.enumeration.ItemType.LIST
import net.simonvt.cathode.api.enumeration.ItemType.MOVIE
import net.simonvt.cathode.api.enumeration.ItemType.PERSON
import net.simonvt.cathode.api.enumeration.ItemType.SEASON
import net.simonvt.cathode.api.enumeration.ItemType.SHOW
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ItemTypeStringTest {

  @Test
  fun ensureMatch() {
    ItemType.values().forEach { itemType ->
      when (itemType) {
        SHOW -> assertThat(itemType.value).isEqualTo(ItemTypeString.SHOW)
        SEASON -> assertThat(itemType.value).isEqualTo(ItemTypeString.SEASON)
        EPISODE -> assertThat(itemType.value).isEqualTo(ItemTypeString.EPISODE)
        MOVIE -> assertThat(itemType.value).isEqualTo(ItemTypeString.MOVIE)
        PERSON -> assertThat(itemType.value).isEqualTo(ItemTypeString.PERSON)
        LIST -> assertThat(itemType.value).isEqualTo(ItemTypeString.LIST)
        COMMENT -> assertThat(itemType.value).isEqualTo(ItemTypeString.COMMENT)
        else -> throw RuntimeException("Unknown item type: " + itemType.value)
      }
    }
  }
}
