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
package net.simonvt.cathode.ui.person

import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.util.TraktUtils
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.databinding.FragmentPersonBinding
import net.simonvt.cathode.databinding.PersonItemCreditBinding
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class PersonFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory
) : RefreshableAppBarFragment() {

  private var personId: Long = -1L

  private val viewModel: PersonViewModel by viewModels { viewModelFactory }

  private var person: Person? = null

  private var itemCount: Int = 0

  private var _binding: FragmentPersonBinding? = null
  private val binding get() = _binding!!

  lateinit var navigationListener: NavigationListener

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    personId = requireArguments().getLong(ARG_PERSON_ID)

    itemCount = resources.getInteger(R.integer.personCreditColumns)

    viewModel.setPersonId(personId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.person.observe(this, Observer { person -> updateView(person) })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
    _binding = FragmentPersonBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val linkDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_link_black_24dp, null)
    binding.viewOnTrakt.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    binding.castHeader.setOnClickListener(castHeaderClickListener)
    binding.productionHeader.setOnClickListener(productionHeaderClickListener)
    binding.artHeader.setOnClickListener(artHeaderClickListener)
    binding.crewHeader.setOnClickListener(crewHeaderClickListener)
    binding.costumeMakeupHeader.setOnClickListener(costumeMakeupHeaderClickListener)
    binding.directingHeader.setOnClickListener(directingHeaderClickListener)
    binding.writingHeader.setOnClickListener(writingHeaderClickListener)
    binding.soundHeader.setOnClickListener(soundHeaderClickListener)
    binding.cameraHeader.setOnClickListener(cameraHeaderClickListener)

    updateView(person)
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  private val castHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.CAST)
  }

  private val productionHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.PRODUCTION)
  }

  private val artHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.ART)
  }

  private val crewHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.CREW)
  }

  private val costumeMakeupHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.COSTUME_AND_MAKEUP)
  }

  private val directingHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.DIRECTING)
  }

  private val writingHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.WRITING)
  }

  private val soundHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.SOUND)
  }

  private val cameraHeaderClickListener = View.OnClickListener {
    navigationListener.onDisplayPersonCredit(personId, Department.CAMERA)
  }

  private fun updateView(person: Person?) {
    this.person = person
    if (person != null && view != null) {
      setTitle(person.name)
      setBackdrop(person.screenshot)
      binding.headshot.setImage(person.headshot)

      if (!TextUtils.isEmpty(person.birthday)) {
        binding.bornTitle.visibility = View.VISIBLE
        binding.birthday.visibility = View.VISIBLE
        binding.birthplace.visibility = View.VISIBLE

        binding.birthday.text = person.birthday
        binding.birthplace.text = person.birthplace
      } else {
        binding.bornTitle.visibility = View.GONE
        binding.birthday.visibility = View.GONE
        binding.birthplace.visibility = View.GONE
      }

      if (!TextUtils.isEmpty(person.death)) {
        binding.deathTitle.visibility = View.VISIBLE
        binding.death.visibility = View.VISIBLE
        binding.death.text = person.death
      } else {
        binding.deathTitle.visibility = View.GONE
        binding.death.visibility = View.GONE
      }

      binding.biography.text = person.biography

      updateItems(binding.castHeader, binding.castItems, person.credits.cast)
      updateItems(binding.productionHeader, binding.productionItems, person.credits.production)
      updateItems(binding.artHeader, binding.artItems, person.credits.art)
      updateItems(binding.crewHeader, binding.crewItems, person.credits.crew)
      updateItems(
        binding.costumeMakeupHeader,
        binding.costumeMakeupItems,
        person.credits.costumeAndMakeUp
      )
      updateItems(binding.directingHeader, binding.directingItems, person.credits.directing)
      updateItems(binding.writingHeader, binding.writingItems, person.credits.writing)
      updateItems(binding.soundHeader, binding.soundItems, person.credits.sound)
      updateItems(binding.cameraHeader, binding.cameraItems, person.credits.camera)

      binding.viewOnTrakt.setOnClickListener {
        Intents.openUrl(
          requireContext(),
          TraktUtils.getTraktPersonUrl(person.traktId)
        )
      }
    }
  }

  private fun updateItems(header: View?, items: ViewGroup, credits: List<PersonCredit>?) {
    items.removeAllViews()

    val size = credits?.size ?: 0
    if (size > 0) {
      header!!.visibility = View.VISIBLE
      items.visibility = View.VISIBLE

      var i = 0
      while (i < size && i < itemCount) {
        val credit = credits!![i]

        val itemBinding =
          PersonItemCreditBinding.inflate(LayoutInflater.from(requireContext()), items, false)

        itemBinding.poster.setImage(credit.getPoster())
        itemBinding.title.text = credit.getTitle()

        if (credit.getJob() != null) {
          itemBinding.job.text = credit.getJob()
        } else {
          itemBinding.job.text = credit.getCharacter()
        }

        itemBinding.root.setOnClickListener {
          if (credit.getItemType() === ItemType.SHOW) {
            navigationListener.onDisplayShow(
              credit.getItemId(),
              credit.getTitle(),
              credit.getOverview(),
              LibraryType.WATCHED
            )
          } else {
            navigationListener.onDisplayMovie(
              credit.getItemId(),
              credit.getTitle(),
              credit.getOverview()
            )
          }
        }

        items.addView(itemBinding.root)
        i++
      }
    } else {
      header!!.visibility = View.GONE
      items.visibility = View.GONE
    }
  }

  companion object {

    private const val TAG = "net.simonvt.cathode.ui.person.PersonFragment"

    private const val ARG_PERSON_ID = "net.simonvt.cathode.ui.person.PersonFragment.personId"

    @JvmStatic
    fun getTag(personId: Long): String {
      return TAG + "/" + personId + "/" + Ids.newId()
    }

    @JvmStatic
    fun getArgs(personId: Long): Bundle {
      Preconditions.checkArgument(personId >= 0, "personId must be >= 0")

      val args = Bundle()
      args.putLong(ARG_PERSON_ID, personId)
      return args
    }
  }
}
