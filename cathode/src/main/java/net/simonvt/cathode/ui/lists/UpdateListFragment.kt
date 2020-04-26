/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.lists

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortOrientation
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.databinding.DialogListCreateBinding
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler
import javax.inject.Inject

class UpdateListFragment @Inject constructor(
  private val listsTaskScheduler: ListsTaskScheduler
) : DialogFragment() {

  private var _binding: DialogListCreateBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    if (showsDialog) {
      setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = DialogListCreateBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val args = arguments
    val listId = args!!.getLong(ARG_LIST_ID)

    binding.toolbarInclude.toolbar.setTitle(R.string.action_list_update)
    binding.toolbarInclude.toolbar.inflateMenu(R.menu.fragment_list_update)
    binding.toolbarInclude.toolbar.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.menu_update -> {
          val privacyValue = when (binding.privacy.selectedItemPosition) {
            1 -> Privacy.FRIENDS
            2 -> Privacy.PUBLIC
            else -> Privacy.PRIVATE
          }
          val sortByValue = when (binding.sortBy.selectedItemPosition) {
            1 -> SortBy.ADDED
            2 -> SortBy.TITLE
            3 -> SortBy.RELEASED
            4 -> SortBy.RUNTIME
            5 -> SortBy.POPULARITY
            6 -> SortBy.PERCENTAGE
            7 -> SortBy.VOTES
            8 -> SortBy.MY_RATING
            9 -> SortBy.RANDOM
            else -> SortBy.RANK
          }
          val sortOrientationValue = when (binding.sortOrientation.selectedItemPosition) {
            1 -> SortOrientation.DESC
            else -> SortOrientation.ASC
          }

          listsTaskScheduler.updateList(
            listId,
            binding.name.text.toString(),
            binding.description.text.toString(),
            privacyValue,
            binding.displayNumbers.isChecked,
            binding.allowComments.isChecked,
            sortByValue,
            sortOrientationValue
          )

          dismiss()
          return@OnMenuItemClickListener true
        }
      }

      false
    })

    val privacyAdapter = ArrayAdapter.createFromResource(
      requireContext(),
      R.array.list_privacy,
      android.R.layout.simple_spinner_item
    )
    privacyAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    binding.privacy.adapter = privacyAdapter

    val sortByAdapter = ArrayAdapter.createFromResource(
      requireContext(),
      R.array.list_sort_by,
      android.R.layout.simple_spinner_item
    )
    sortByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    binding.sortBy.adapter = sortByAdapter

    val sortOrientationAdapter = ArrayAdapter.createFromResource(
      requireContext(),
      R.array.list_sort_orientation,
      android.R.layout.simple_spinner_item
    )
    sortOrientationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    binding.sortOrientation.adapter = sortOrientationAdapter

    val name = args.getString(ARG_NAME)
    binding.name.setText(name)

    val description = args.getString(ARG_DESCRIPTION)
    binding.description.setText(description)

    when (Privacy.fromValue(args.getString(ARG_PRIVACY)!!)) {
      Privacy.FRIENDS -> binding.privacy.setSelection(1)
      Privacy.PUBLIC -> binding.privacy.setSelection(2)
      else -> binding.privacy.setSelection(0)
    }

    when (SortBy.fromValue(args.getString(ARG_SORT_BY))) {
      SortBy.RANK -> binding.sortBy.setSelection(0)
      SortBy.ADDED -> binding.sortBy.setSelection(1)
      SortBy.TITLE -> binding.sortBy.setSelection(2)
      SortBy.RELEASED -> binding.sortBy.setSelection(3)
      SortBy.RUNTIME -> binding.sortBy.setSelection(4)
      SortBy.POPULARITY -> binding.sortBy.setSelection(5)
      SortBy.PERCENTAGE -> binding.sortBy.setSelection(6)
      SortBy.VOTES -> binding.sortBy.setSelection(7)
      SortBy.MY_RATING -> binding.sortBy.setSelection(8)
      SortBy.RANDOM -> binding.sortBy.setSelection(9)
    }

    when (SortOrientation.fromValue(args.getString(ARG_SORT_ORIENTATION))) {
      SortOrientation.ASC -> binding.sortOrientation.setSelection(0)
      SortOrientation.DESC -> binding.sortOrientation.setSelection(1)
    }

    val displayNumbers = args.getBoolean(ARG_DISPLAY_NUMBERS)
    binding.displayNumbers.isChecked = displayNumbers

    val allowComments = args.getBoolean(ARG_ALLOW_COMMENTS)
    binding.allowComments.isChecked = allowComments
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  companion object {

    private const val ARG_LIST_ID = "net.simonvt.cathode.ui.lists.UpdateListFragment.listId"
    private const val ARG_NAME = "net.simonvt.cathode.ui.lists.UpdateListFragment.name"
    private const val ARG_DESCRIPTION =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.description"
    private const val ARG_PRIVACY = "net.simonvt.cathode.ui.lists.UpdateListFragment.privacy"
    private const val ARG_DISPLAY_NUMBERS =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.displayNumbers"
    private const val ARG_ALLOW_COMMENTS =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.allowComments"
    private const val ARG_SORT_BY = "net.simonvt.cathode.ui.lists.UpdateListFragment.sortBy"
    private const val ARG_SORT_ORIENTATION =
      "net.simonvt.cathode.ui.lists.UpdateListFragment.sortOrientation"

    fun getArgs(
      listId: Long,
      name: String,
      description: String?,
      privacy: Privacy,
      displayNumbers: Boolean,
      allowComments: Boolean,
      sortBy: SortBy,
      sortOrientation: SortOrientation
    ): Bundle {
      Preconditions.checkArgument(listId >= 0, "listId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_LIST_ID, listId)
      args.putString(ARG_NAME, name)
      args.putString(ARG_DESCRIPTION, description)
      args.putString(ARG_PRIVACY, privacy.toString())
      args.putBoolean(ARG_DISPLAY_NUMBERS, displayNumbers)
      args.putBoolean(ARG_ALLOW_COMMENTS, allowComments)
      args.putString(ARG_SORT_BY, sortBy.toString())
      args.putString(ARG_SORT_ORIENTATION, sortOrientation.toString())
      return args
    }
  }
}
