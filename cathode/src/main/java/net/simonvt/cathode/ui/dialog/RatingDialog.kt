/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.dialog

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.RatingBar.OnRatingBarChangeListener
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import net.simonvt.cathode.R.array
import net.simonvt.cathode.R.string
import net.simonvt.cathode.databinding.DialogRatingBinding
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.sync.scheduler.ShowTaskScheduler
import net.simonvt.cathode.ui.dialog.RatingDialog.Type.EPISODE
import net.simonvt.cathode.ui.dialog.RatingDialog.Type.MOVIE
import net.simonvt.cathode.ui.dialog.RatingDialog.Type.SHOW
import javax.inject.Inject

class RatingDialog @Inject constructor(
  private val showScheduler: ShowTaskScheduler,
  private val episodeScheduler: EpisodeTaskScheduler,
  private val movieScheduler: MovieTaskScheduler
) : DialogFragment() {

  enum class Type {
    SHOW, EPISODE, MOVIE
  }

  private lateinit var type: Type
  private var id: Long = -1L
  private lateinit var ratingTexts: Array<String>

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    type = requireArguments().getSerializable(ARG_TYPE) as Type
    id = requireArguments().getLong(ARG_ID)
    ratingTexts = resources.getStringArray(array.ratings)
  }

  override fun onCreateDialog(inState: Bundle?): Dialog {
    val ratingArg = requireArguments().getInt(ARG_RATING)
    val initialRating = ratingArg / 2.0f
    val builder = Builder(requireContext())
    val binding = DialogRatingBinding.inflate(LayoutInflater.from(builder.context))
    binding.ratingText.text = ratingTexts[ratingArg]
    binding.rating.rating = initialRating
    binding.rating.onRatingBarChangeListener = OnRatingBarChangeListener { ratingBar, v, b ->
      val rating = (v * 2).toInt()
      binding.ratingText.text = ratingTexts[rating]
    }
    builder.setView(binding.root)
    builder.setPositiveButton(string.action_rate) { dialogInterface, i ->
      val rating = (binding.rating.rating * 2).toInt()
      when (type) {
        SHOW -> showScheduler.rate(id, rating)
        EPISODE -> episodeScheduler.rate(id, rating)
        MOVIE -> movieScheduler.rate(id, rating)
      }
    }
    return builder.create()
  }

  companion object {
    private const val ARG_TYPE = "net.simonvt.cathode.ui.dialog.RatingDialog.type"
    private const val ARG_ID = "net.simonvt.cathode.ui.dialog.RatingDialog.id"
    private const val ARG_RATING = "net.simonvt.cathode.ui.dialog.RatingDialog.rating"

    fun getArgs(type: Type?, id: Long, rating: Int): Bundle {
      val args = Bundle()
      args.putSerializable(ARG_TYPE, type)
      args.putLong(ARG_ID, id)
      args.putInt(ARG_RATING, rating)
      return args
    }
  }
}
