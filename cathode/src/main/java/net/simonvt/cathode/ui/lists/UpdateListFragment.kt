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
import android.widget.CheckBox
import android.widget.EditText
import android.widget.Spinner
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.DialogFragment
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.Unbinder
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Privacy
import net.simonvt.cathode.api.enumeration.SortBy
import net.simonvt.cathode.api.enumeration.SortOrientation
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.sync.scheduler.ListsTaskScheduler
import javax.inject.Inject

class UpdateListFragment : DialogFragment() {

  @Inject
  lateinit var listsTaskScheduler: ListsTaskScheduler

  private var unbinder: Unbinder? = null

  @BindView(R.id.toolbar)
  @JvmField
  var toolbar: Toolbar? = null

  @BindView(R.id.name)
  @JvmField
  var name: EditText? = null

  @BindView(R.id.description)
  @JvmField
  var description: EditText? = null

  @BindView(R.id.privacy)
  @JvmField
  var privacy: Spinner? = null

  @BindView(R.id.displayNumbers)
  @JvmField
  var displayNumbers: CheckBox? = null

  @BindView(R.id.allowComments)
  @JvmField
  var allowComments: CheckBox? = null

  @BindView(R.id.sortBy)
  @JvmField
  var sortBy: Spinner? = null

  @BindView(R.id.sortOrientation)
  @JvmField
  var sortOrientation: Spinner? = null

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    if (showsDialog) {
      setStyle(DialogFragment.STYLE_NO_TITLE, 0)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.dialog_list_create, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    unbinder = ButterKnife.bind(this, view)

    val args = arguments
    val listId = args!!.getLong(ARG_LIST_ID)

    toolbar!!.setTitle(R.string.action_list_update)
    toolbar!!.inflateMenu(R.menu.fragment_list_update)
    toolbar!!.setOnMenuItemClickListener(Toolbar.OnMenuItemClickListener { item ->
      when (item.itemId) {
        R.id.menu_update -> {
          val privacyValue = when (privacy!!.selectedItemPosition) {
            1 -> Privacy.FRIENDS
            2 -> Privacy.PUBLIC
            else -> Privacy.PRIVATE
          }
          val sortByValue = when (sortBy!!.selectedItemPosition) {
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
          val sortOrientationValue = when (sortOrientation!!.selectedItemPosition) {
            1 -> SortOrientation.DESC
            else -> SortOrientation.ASC
          }

          listsTaskScheduler.updateList(
            listId,
            name!!.text.toString(),
            description!!.text.toString(),
            privacyValue,
            displayNumbers!!.isChecked,
            allowComments!!.isChecked,
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
    privacy!!.adapter = privacyAdapter

    val sortByAdapter = ArrayAdapter.createFromResource(
      requireContext(),
      R.array.list_sort_by,
      android.R.layout.simple_spinner_item
    )
    sortByAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    sortBy!!.adapter = sortByAdapter

    val sortOrientationAdapter = ArrayAdapter.createFromResource(
      requireContext(),
      R.array.list_sort_orientation,
      android.R.layout.simple_spinner_item
    )
    sortOrientationAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
    sortOrientation!!.adapter = sortOrientationAdapter

    val name = args.getString(ARG_NAME)
    this.name!!.setText(name)

    val description = args.getString(ARG_DESCRIPTION)
    this.description!!.setText(description)

    when (Privacy.fromValue(args.getString(ARG_PRIVACY)!!)) {
      Privacy.FRIENDS -> privacy!!.setSelection(1)
      Privacy.PUBLIC -> privacy!!.setSelection(2)
      else -> this.privacy!!.setSelection(0)
    }

    when (SortBy.fromValue(args.getString(ARG_SORT_BY))) {
      SortBy.RANK -> sortBy!!.setSelection(0)
      SortBy.ADDED -> sortBy!!.setSelection(1)
      SortBy.TITLE -> sortBy!!.setSelection(2)
      SortBy.RELEASED -> sortBy!!.setSelection(3)
      SortBy.RUNTIME -> sortBy!!.setSelection(4)
      SortBy.POPULARITY -> sortBy!!.setSelection(5)
      SortBy.PERCENTAGE -> sortBy!!.setSelection(6)
      SortBy.VOTES -> sortBy!!.setSelection(7)
      SortBy.MY_RATING -> sortBy!!.setSelection(8)
      SortBy.RANDOM -> sortBy!!.setSelection(9)
    }

    when (SortOrientation.fromValue(args.getString(ARG_SORT_ORIENTATION))) {
      SortOrientation.ASC -> sortOrientation!!.setSelection(0)
      SortOrientation.DESC -> sortOrientation!!.setSelection(1)
    }

    val displayNumbers = args.getBoolean(ARG_DISPLAY_NUMBERS)
    this.displayNumbers!!.isChecked = displayNumbers

    val allowComments = args.getBoolean(ARG_ALLOW_COMMENTS)
    this.allowComments!!.isChecked = allowComments
  }

  override fun onDestroyView() {
    unbinder!!.unbind()
    unbinder = null
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
