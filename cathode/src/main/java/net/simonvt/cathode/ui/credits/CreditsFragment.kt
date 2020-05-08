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
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.fragment.RefreshableToolbarFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.databinding.CreditItemCreditBinding
import net.simonvt.cathode.databinding.FragmentCreditsBinding
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class CreditsFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory
) : RefreshableToolbarFragment() {

  private lateinit var navigationListener: NavigationListener

  private lateinit var itemType: ItemType

  private var itemId: Long = 0

  private var title: String? = null

  private val viewModel: CreditsViewModel by viewModels { viewModelFactory }

  private var credits: Credits? = null

  private var itemCount: Int = 0

  private var _binding: FragmentCreditsBinding? = null
  private val binding get() = _binding!!

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    val args = requireArguments()
    itemId = args.getLong(ARG_ITEM_ID)
    title = args.getString(ARG_TITLE)
    itemType = args.getSerializable(ARG_TYPE) as ItemType

    setTitle(title)

    itemCount = resources.getInteger(R.integer.creditColumns)

    viewModel.setItemTypeAndId(itemType, itemId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.credits.observe(this, Observer { credits -> updateView(credits) })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = FragmentCreditsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    binding.castHeader.setOnClickListener(castHeaderClickListener)
    binding.productionHeader.setOnClickListener(productionHeaderClickListener)
    binding.artHeader.setOnClickListener(artHeaderClickListener)
    binding.crewHeader.setOnClickListener(crewHeaderClickListener)
    binding.costumeMakeupHeader.setOnClickListener(costumeMakeupHeaderClickListener)
    binding.directingHeader.setOnClickListener(directingHeaderClickListener)
    binding.writingHeader.setOnClickListener(writingHeaderClickListener)
    binding.soundHeader.setOnClickListener(soundHeaderClickListener)
    binding.cameraHeader.setOnClickListener(cameraHeaderClickListener)
    updateView(credits)
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  private val castHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CAST)
  }

  private val productionHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.PRODUCTION)
  }

  private val artHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.ART)
  }

  private val crewHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CREW)
  }

  private val costumeMakeupHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.COSTUME_AND_MAKEUP)
  }

  private val directingHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.DIRECTING)
  }

  private val writingHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.WRITING)
  }

  private val soundHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.SOUND)
  }

  private val cameraHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CAMERA)
  }

  private fun updateView(credits: Credits?) {
    this.credits = credits
    if (credits != null && view != null) {
      updateItems(binding.castHeader, binding.castItems, credits.cast)
      updateItems(binding.productionHeader, binding.productionItems, credits.production)
      updateItems(binding.artHeader, binding.artItems, credits.art)
      updateItems(binding.crewHeader, binding.crewItems, credits.crew)
      updateItems(binding.costumeMakeupHeader, binding.costumeMakeupItems, credits.costumeAndMakeUp)
      updateItems(binding.directingHeader, binding.directingItems, credits.directing)
      updateItems(binding.writingHeader, binding.writingItems, credits.writing)
      updateItems(binding.soundHeader, binding.soundItems, credits.sound)
      updateItems(binding.cameraHeader, binding.cameraItems, credits.camera)
    }
  }

  private fun updateItems(header: View?, items: ViewGroup, credits: List<Credit>?) {
    items.removeAllViews()

    val size = credits?.size ?: 0
    if (size > 0) {
      header!!.visibility = View.VISIBLE
      items.visibility = View.VISIBLE

      var i = 0
      while (i < size && i < itemCount) {
        val credit = credits!![i]

        val itemBinding =
          CreditItemCreditBinding.inflate(LayoutInflater.from(requireContext()), items, false)

        itemBinding.headshot.setImage(credit.getHeadshot())
        itemBinding.name.text = credit.getName()
        if (credit.getJob() != null) {
          itemBinding.job.text = credit.getJob()
        } else {
          itemBinding.job.text = credit.getCharacter()
        }

        itemBinding.root.setOnClickListener { navigationListener.onDisplayPerson(credit.getPersonId()) }

        items.addView(itemBinding.root)
        i++
      }
    } else {
      header!!.visibility = View.GONE
      items.visibility = View.GONE
    }
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.credits.CreditsFragment"

    private const val ARG_TYPE = "net.simonvt.cathode.ui.credits.CreditsFragment.itemType"
    private const val ARG_ITEM_ID = "net.simonvt.cathode.ui.credits.CreditsFragment.itemId"
    private const val ARG_TITLE = "net.simonvt.cathode.ui.credits.CreditsFragment.title"

    @JvmStatic
    fun getTag(itemId: Long): String {
      return TAG + "/" + itemId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(itemType: ItemType, itemId: Long, title: String?): Bundle {
      Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0")

      val args = Bundle()
      args.putSerializable(ARG_TYPE, itemType)
      args.putLong(ARG_ITEM_ID, itemId)
      args.putString(ARG_TITLE, title)
      return args
    }
  }
}
