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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import butterknife.BindView
import butterknife.OnClick
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.api.util.TraktUtils
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class PersonFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory
) : RefreshableAppBarFragment() {

  private var personId: Long = -1L

  private lateinit var viewModel: PersonViewModel

  private var person: Person? = null

  private var itemCount: Int = 0

  @BindView(R.id.headshot)
  @JvmField
  var headshot: RemoteImageView? = null
  @BindView(R.id.bornTitle)
  @JvmField
  var bornTitle: View? = null
  @BindView(R.id.birthday)
  @JvmField
  var birthday: TextView? = null
  @BindView(R.id.birthplace)
  @JvmField
  var birthplace: TextView? = null
  @BindView(R.id.deathTitle)
  @JvmField
  var deathTitle: View? = null
  @BindView(R.id.death)
  @JvmField
  var death: TextView? = null
  @BindView(R.id.biography)
  @JvmField
  var biography: TextView? = null

  @BindView(R.id.cast_header)
  @JvmField
  var castHeader: LinearLayout? = null
  @BindView(R.id.cast_items)
  @JvmField
  var castItems: LinearLayout? = null

  @BindView(R.id.production_header)
  @JvmField
  var productionHeader: LinearLayout? = null
  @BindView(R.id.production_items)
  @JvmField
  var productionItems: LinearLayout? = null

  @BindView(R.id.art_header)
  @JvmField
  var artHeader: LinearLayout? = null
  @BindView(R.id.art_items)
  @JvmField
  var artItems: LinearLayout? = null

  @BindView(R.id.crew_header)
  @JvmField
  var crewHeader: LinearLayout? = null
  @BindView(R.id.crew_items)
  @JvmField
  var crewItems: LinearLayout? = null

  @BindView(R.id.costume_makeup_header)
  @JvmField
  var costumeMakeupHeader: LinearLayout? = null
  @BindView(R.id.costume_makeup_items)
  @JvmField
  var costumeMakeupItems: LinearLayout? = null

  @BindView(R.id.directing_header)
  @JvmField
  var directingHeader: LinearLayout? = null
  @BindView(R.id.directing_items)
  @JvmField
  var directingItems: LinearLayout? = null

  @BindView(R.id.writing_header)
  @JvmField
  var writingHeader: LinearLayout? = null
  @BindView(R.id.writing_items)
  @JvmField
  var writingItems: LinearLayout? = null

  @BindView(R.id.sound_header)
  @JvmField
  var soundHeader: LinearLayout? = null
  @BindView(R.id.sound_items)
  @JvmField
  var soundItems: LinearLayout? = null

  @BindView(R.id.camera_header)
  @JvmField
  var cameraHeader: LinearLayout? = null
  @BindView(R.id.camera_items)
  @JvmField
  var cameraItems: LinearLayout? = null

  @BindView(R.id.viewOnTrakt)
  @JvmField
  var viewOnTrakt: TextView? = null

  lateinit var navigationListener: NavigationListener

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    personId = requireArguments().getLong(ARG_PERSON_ID)

    itemCount = resources.getInteger(R.integer.personCreditColumns)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(PersonViewModel::class.java)
    viewModel.setPersonId(personId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.person.observe(this, Observer { person -> updateView(person) })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_person, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    val linkDrawable = VectorDrawableCompat.create(resources, R.drawable.ic_link_black_24dp, null)
    viewOnTrakt!!.setCompoundDrawablesWithIntrinsicBounds(linkDrawable, null, null, null)
    updateView(person)
  }

  @OnClick(R.id.cast_header)
  fun onDisplayCastCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.CAST)
  }

  @OnClick(R.id.production_header)
  fun onDisplayProductionCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.PRODUCTION)
  }

  @OnClick(R.id.art_header)
  fun onDisplayArtCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.ART)
  }

  @OnClick(R.id.crew_header)
  fun onDisplayCrewCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.CREW)
  }

  @OnClick(R.id.costume_makeup_header)
  fun onDisplayCostumeMakeUpCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.COSTUME_AND_MAKEUP)
  }

  @OnClick(R.id.directing_header)
  fun onDisplayDirectingCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.DIRECTING)
  }

  @OnClick(R.id.writing_header)
  fun onDisplayWritingCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.WRITING)
  }

  @OnClick(R.id.sound_header)
  fun onDisplaySoundCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.SOUND)
  }

  @OnClick(R.id.camera_header)
  fun onDisplayCameraCredits() {
    navigationListener.onDisplayPersonCredit(personId, Department.CAMERA)
  }

  private fun updateView(person: Person?) {
    this.person = person
    if (person != null && view != null) {
      setTitle(person.name)
      setBackdrop(person.screenshot)
      headshot!!.setImage(person.headshot)

      if (!TextUtils.isEmpty(person.birthday)) {
        bornTitle!!.visibility = View.VISIBLE
        birthday!!.visibility = View.VISIBLE
        birthplace!!.visibility = View.VISIBLE

        birthday!!.text = person.birthday
        birthplace!!.text = person.birthplace
      } else {
        bornTitle!!.visibility = View.GONE
        birthday!!.visibility = View.GONE
        birthplace!!.visibility = View.GONE
      }

      if (!TextUtils.isEmpty(person.death)) {
        deathTitle!!.visibility = View.VISIBLE
        death!!.visibility = View.VISIBLE
        death!!.text = person.death
      } else {
        deathTitle!!.visibility = View.GONE
        death!!.visibility = View.GONE
      }

      biography!!.text = person.biography

      updateItems(castHeader, castItems!!, person.credits.cast)
      updateItems(productionHeader, productionItems!!, person.credits.production)
      updateItems(artHeader, artItems!!, person.credits.art)
      updateItems(crewHeader, crewItems!!, person.credits.crew)
      updateItems(
        costumeMakeupHeader, costumeMakeupItems!!,
        person.credits.costumeAndMakeUp
      )
      updateItems(directingHeader, directingItems!!, person.credits.directing)
      updateItems(writingHeader, writingItems!!, person.credits.writing)
      updateItems(soundHeader, soundItems!!, person.credits.sound)
      updateItems(cameraHeader, cameraItems!!, person.credits.camera)

      viewOnTrakt!!.setOnClickListener {
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

        val view =
          LayoutInflater.from(requireContext()).inflate(R.layout.person_item_credit, items, false)

        val poster = view.findViewById<RemoteImageView>(R.id.poster)
        val title = view.findViewById<TextView>(R.id.title)
        val job = view.findViewById<TextView>(R.id.job)

        poster.setImage(credit.getPoster())
        title.text = credit.getTitle()

        if (credit.getJob() != null) {
          job.text = credit.getJob()
        } else {
          job.text = credit.getCharacter()
        }

        view.setOnClickListener {
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

        items.addView(view)
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
