package net.simonvt.cathode.entity

import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortOrientation

data class UserList(
  val id: Long,
  val name: String,
  val description: String?,
  val privacy: Privacy,
  val displayNumbers: Boolean,
  val allowComments: Boolean,
  val sortBy: SortBy,
  val sortOrientation: SortOrientation,
  val updatedAt: Long,
  val likes: Int,
  val slug: String?,
  val traktId: Long
)
