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
package net.simonvt.cathode.ui.credits

import android.content.Context
import android.os.Bundle
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class CreditFragment @Inject constructor(private val viewModelFactory: CathodeViewModelFactory) :
  ToolbarGridFragment<CreditAdapter.ViewHolder>(),
  CreditAdapter.OnCreditClickListener {

  private lateinit var itemType: ItemType
  private lateinit var department: Department
  private var itemId: Long = 0

  private lateinit var viewModel: CreditsViewModel

  private var adapter: CreditAdapter? = null

  private lateinit var navigationListener: NavigationListener

  private var columnCount: Int = 0

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    itemType = requireArguments().getSerializable(ARG_ITEM_TYPE) as ItemType
    itemId = requireArguments().getLong(ARG_ITEM_ID)
    department = requireArguments().getSerializable(ARG_DEPARTMENT) as Department

    columnCount = resources.getInteger(R.integer.creditColumns)

    when (department) {
      Department.CAST -> setTitle(R.string.person_department_cast)
      Department.PRODUCTION -> setTitle(R.string.person_department_production)
      Department.ART -> setTitle(R.string.person_department_art)
      Department.CREW -> setTitle(R.string.person_department_crew)
      Department.COSTUME_AND_MAKEUP -> setTitle(R.string.person_department_costume_makeup)
      Department.DIRECTING -> setTitle(R.string.person_department_directing)
      Department.WRITING -> setTitle(R.string.person_department_writing)
      Department.SOUND -> setTitle(R.string.person_department_sound)
      Department.CAMERA -> setTitle(R.string.person_department_camera)
    }

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(CreditsViewModel::class.java)
    viewModel.setItemTypeAndId(itemType, itemId)
    viewModel.credits.observe(this, Observer { credits ->
      when (department) {
        Department.CAST -> setCredits(credits.cast)
        Department.PRODUCTION -> setCredits(credits.production)
        Department.ART -> setCredits(credits.art)
        Department.CREW -> setCredits(credits.crew)
        Department.COSTUME_AND_MAKEUP -> setCredits(credits.costumeAndMakeUp)
        Department.DIRECTING -> setCredits(credits.directing)
        Department.WRITING -> setCredits(credits.writing)
        Department.SOUND -> setCredits(credits.sound)
        Department.CAMERA -> setCredits(credits.camera)
      }
    })
  }

  override fun getColumnCount(): Int {
    return columnCount
  }

  override fun onPersonClicked(personId: Long) {
    navigationListener.onDisplayPerson(personId)
  }

  private fun setCredits(credits: List<Credit>) {
    if (adapter == null) {
      adapter = CreditAdapter(credits, this)
      setAdapter(adapter)
    } else {
      adapter!!.setCredits(credits)
    }
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.person.CreditsFragment"

    private const val ARG_ITEM_TYPE = "net.simonvt.cathode.ui.credits.CreditFragment.itemType"
    private const val ARG_ITEM_ID = "net.simonvt.cathode.ui.credits.CreditFragment.itemId"
    private const val ARG_DEPARTMENT = "net.simonvt.cathode.ui.credits.CreditFragment.department"

    fun getTag(itemId: Long): String {
      return TAG + "/" + itemId + "/" + Ids.newId()
    }

    fun getArgs(itemType: ItemType, itemId: Long, department: Department): Bundle {
      Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0")

      val args = Bundle()
      args.putSerializable(ARG_ITEM_TYPE, itemType)
      args.putLong(ARG_ITEM_ID, itemId)
      args.putSerializable(ARG_DEPARTMENT, department)
      return args
    }
  }
}
