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
package net.simonvt.cathode.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatDialogFragment
import androidx.fragment.app.DialogFragment
import net.simonvt.cathode.BuildConfig
import net.simonvt.cathode.R.string
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.databinding.DialogAboutBinding

class AboutDialog : AppCompatDialogFragment() {

  private var _binding: DialogAboutBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    setStyle(DialogFragment.STYLE_NO_TITLE, theme)
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = DialogAboutBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    binding.version.text = BuildConfig.VERSION_NAME
    binding.credits.setOnClickListener {
      CreditsDialog().show(
        parentFragmentManager,
        DIALOG_CREDITS
      )
    }
    binding.licenses.setOnClickListener {
      LicensesDialog().show(
        parentFragmentManager,
        DIALOG_LICENSES
      )
    }
    binding.version.setOnClickListener {
      Intents.openUrl(
        activity,
        getString(string.play_store_url)
      )
    }
    binding.github.setOnClickListener { Intents.openUrl(activity, getString(string.dev_github)) }
    binding.source.setOnClickListener { Intents.openUrl(activity, getString(string.source_url)) }
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  companion object {
    private const val DIALOG_CREDITS = "net.simonvt.cathode.settings.AboutDialog.credits"
    private const val DIALOG_LICENSES = "net.simonvt.cathode.settings.AboutDialog.licenses"
  }
}
