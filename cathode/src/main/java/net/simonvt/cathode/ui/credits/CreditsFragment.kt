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
import android.widget.LinearLayout
import android.widget.TextView
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import butterknife.BindView
import butterknife.OnClick
import dagger.android.support.AndroidSupportInjection
import net.simonvt.cathode.R
import net.simonvt.cathode.api.enumeration.Department
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.ui.fragment.RefreshableToolbarFragment
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class CreditsFragment : RefreshableToolbarFragment() {

  @Inject
  lateinit var showScheduler: ShowTaskScheduler
  @Inject
  lateinit var movieScheduler: MovieTaskScheduler

  private lateinit var navigationListener: NavigationListener

  private var itemType: ItemType? = null

  private var itemId: Long = 0

  private var title: String? = null

  @Inject
  lateinit var viewModelFactory: CathodeViewModelFactory
  lateinit var viewModel: CreditsViewModel

  private var credits: Credits? = null

  private var itemCount: Int = 0

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

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidSupportInjection.inject(this)

    val args = requireArguments()
    itemId = args.getLong(ARG_ITEM_ID)
    title = args.getString(ARG_TITLE)
    itemType = args.getSerializable(ARG_TYPE) as ItemType

    setTitle(title)

    itemCount = resources.getInteger(R.integer.creditColumns)

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(CreditsViewModel::class.java)
    viewModel.setItemTypeAndId(itemType!!, itemId)
    viewModel.loading.observe(this, Observer { loading -> setRefreshing(loading) })
    viewModel.credits.observe(this, Observer { credits -> updateView(credits) })
  }

  override fun onRefresh() {
    viewModel.refresh()
  }

  override fun createView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
    return inflater.inflate(R.layout.fragment_credits, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    updateView(credits)
  }

  @OnClick(R.id.cast_header)
  fun onDisplayCastCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CAST)
  }

  @OnClick(R.id.production_header)
  fun onDisplayProductionCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.PRODUCTION)
  }

  @OnClick(R.id.art_header)
  fun onDisplayArtCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.ART)
  }

  @OnClick(R.id.crew_header)
  fun onDisplayCrewCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CREW)
  }

  @OnClick(R.id.costume_makeup_header)
  fun onDisplayCostumeMakeUpCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.COSTUME_AND_MAKEUP)
  }

  @OnClick(R.id.directing_header)
  fun onDisplayDirectingCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.DIRECTING)
  }

  @OnClick(R.id.writing_header)
  fun onDisplayWritingCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.WRITING)
  }

  @OnClick(R.id.sound_header)
  fun onDisplaySoundCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.SOUND)
  }

  @OnClick(R.id.camera_header)
  fun onDisplayCameraCredits() {
    navigationListener.onDisplayCredit(itemType, itemId, Department.CAMERA)
  }

  private fun updateView(credits: Credits?) {
    this.credits = credits
    if (credits != null && view != null) {
      updateItems(castHeader, castItems!!, credits.cast)
      updateItems(productionHeader, productionItems!!, credits.production)
      updateItems(artHeader, artItems!!, credits.art)
      updateItems(crewHeader, crewItems!!, credits.crew)
      updateItems(costumeMakeupHeader, costumeMakeupItems!!, credits.costumeAndMakeUp)
      updateItems(directingHeader, directingItems!!, credits.directing)
      updateItems(writingHeader, writingItems!!, credits.writing)
      updateItems(soundHeader, soundItems!!, credits.sound)
      updateItems(cameraHeader, cameraItems!!, credits.camera)
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

        val view =
          LayoutInflater.from(requireContext()).inflate(R.layout.credit_item_credit, items, false)

        val headshot = view.findViewById<RemoteImageView>(R.id.headshot)
        val name = view.findViewById<TextView>(R.id.name)
        val job = view.findViewById<TextView>(R.id.job)

        headshot.setImage(credit.getHeadshot())
        name.text = credit.getName()
        if (credit.getJob() != null) {
          job.text = credit.getJob()
        } else {
          job.text = credit.getCharacter()
        }

        view.setOnClickListener { navigationListener.onDisplayPerson(credit.getPersonId()) }

        items.addView(view)
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
    fun getArgs(itemType: ItemType, itemId: Long, title: String): Bundle {
      Preconditions.checkArgument(itemId >= 0, "itemId must be >= 0")

      val args = Bundle()
      args.putSerializable(ARG_TYPE, itemType)
      args.putLong(ARG_ITEM_ID, itemId)
      args.putString(ARG_TITLE, title)
      return args
    }
  }
}
