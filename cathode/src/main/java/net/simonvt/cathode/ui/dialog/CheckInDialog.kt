/*
 * Copyright (C) 2014 Simon Vig Therkildsen
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
import android.view.View
import androidx.appcompat.app.AlertDialog.Builder
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentActivity
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.databinding.DialogCheckInBinding
import net.simonvt.cathode.settings.ProfileSettings
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.sync.scheduler.MovieTaskScheduler
import net.simonvt.cathode.ui.dialog.CheckInDialog.Type.SHOW
import timber.log.Timber
import javax.inject.Inject

class CheckInDialog @Inject constructor(
  private val episodeScheduler: EpisodeTaskScheduler,
  private val movieScheduler: MovieTaskScheduler
) : DialogFragment() {

  enum class Type {
    SHOW, MOVIE
  }

  override fun onCreateDialog(inState: Bundle?): Dialog {
    val builder = Builder(requireContext()).setTitle(string.action_checkin)
    val type = requireArguments().getSerializable(ARG_TYPE) as Type?
    val titleArg = requireArguments().getString(ARG_TITLE)
    val id = requireArguments().getLong(ARG_ID)
    val binding = DialogCheckInBinding.inflate(LayoutInflater.from(builder.context))
    binding.title.text = titleArg
    val facebookShare =
      ProfileSettings.get(requireContext()).getBoolean(ProfileSettings.CONNECTION_FACEBOOK, false)
    val twitterShare =
      ProfileSettings.get(requireContext()).getBoolean(ProfileSettings.CONNECTION_TWITTER, false)
    val tumblrShare =
      ProfileSettings.get(requireContext()).getBoolean(ProfileSettings.CONNECTION_TUMBLR, false)
    binding.facebook.visibility = if (facebookShare) View.VISIBLE else View.GONE
    binding.twitter.visibility = if (twitterShare) View.VISIBLE else View.GONE
    binding.tumblr.visibility = if (tumblrShare) View.VISIBLE else View.GONE
    if (facebookShare || twitterShare || tumblrShare) {
      binding.shareTitle
      binding.messageTitle.visibility = View.VISIBLE
      binding.message.visibility = View.VISIBLE
      binding.shareTitle.visibility = View.VISIBLE
    } else {
      binding.messageTitle.visibility = View.GONE
      binding.message.visibility = View.GONE
      binding.shareTitle.visibility = View.GONE
    }
    var shareMessage = ProfileSettings.get(requireContext())
      .getString(ProfileSettings.SHARING_TEXT_WATCHING, getString(string.checkin_message_default))
    shareMessage = shareMessage!!.replace("[item]", titleArg!!)
    binding.message.setText(shareMessage)
    builder.setView(view)
      .setPositiveButton(string.action_checkin) { dialog, which ->
        val facebookShare = binding.facebook.isChecked
        val twitterShare = binding.twitter.isChecked
        val tumblrShare = binding.tumblr.isChecked
        val shareMessage = binding.message.text.toString()
        ProfileSettings.get(requireContext())
          .edit()
          .putBoolean(ProfileSettings.CONNECTION_FACEBOOK, facebookShare)
          .putBoolean(ProfileSettings.CONNECTION_TWITTER, twitterShare)
          .putBoolean(ProfileSettings.CONNECTION_TUMBLR, tumblrShare)
          .putString(ProfileSettings.SHARING_TEXT_WATCHING, shareMessage)
          .apply()
        if (type == SHOW) {
          episodeScheduler.checkin(id, shareMessage, facebookShare, twitterShare, tumblrShare)
        } else {
          movieScheduler.checkin(id, shareMessage, facebookShare, twitterShare, tumblrShare)
        }
      }

    return builder.create()
  }

  companion object {
    private const val ARG_TYPE = "net.simonvt.cathode.ui.dialog.CheckInDialog.type"
    private const val ARG_TITLE = "net.simonvt.cathode.ui.dialog.CheckInDialog.title"
    private const val ARG_ID = "net.simonvt.cathode.ui.dialog.CheckInDialog.id"
    private const val DIALOG_TAG = "net.simonvt.cathode.ui.dialog.CheckInDialog.dialog"
    fun showDialogIfNecessary(
      activity: FragmentActivity,
      type: Type,
      title: String?,
      id: Long
    ): Boolean {
      if (title == null) {
        // TODO: Remove eventually
        Timber.e(Exception("Title is null"), "Type: %s", type.toString())
        return true
      }
      val facebookShare =
        ProfileSettings.get(activity).getBoolean(ProfileSettings.CONNECTION_FACEBOOK, false)
      val twitterShare =
        ProfileSettings.get(activity).getBoolean(ProfileSettings.CONNECTION_TWITTER, false)
      val tumblrShare =
        ProfileSettings.get(activity).getBoolean(ProfileSettings.CONNECTION_TUMBLR, false)
      if (facebookShare || twitterShare || tumblrShare) {
        FragmentsUtils.instantiate(
          activity.supportFragmentManager,
          CheckInDialog::class.java,
          getArgs(type, title, id)
        ).show(activity.supportFragmentManager, DIALOG_TAG)
        return true
      } else {
        return false
      }
    }

    private fun getArgs(type: Type, title: String, id: Long): Bundle {
      val args = Bundle()
      args.putSerializable(ARG_TYPE, type)
      args.putString(ARG_TITLE, title)
      args.putLong(ARG_ID, id)
      return args
    }
  }
}
