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

import android.app.Dialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import androidx.appcompat.app.AlertDialog.Builder
import androidx.appcompat.app.AppCompatDialogFragment
import net.simonvt.cathode.R
import net.simonvt.cathode.databinding.RowLicenseBinding
import java.util.ArrayList

class LicensesDialog : AppCompatDialogFragment() {

  data class License(val library: Int, val license: Int)

  override fun onCreateDialog(inState: Bundle?): Dialog {
    val builder =
      Builder(requireContext()).setTitle(R.string.licenses).setAdapter(LicenseAdapter(), null)
    return builder.create()
  }

  private inner class LicenseAdapter : BaseAdapter() {
    var licenses: MutableList<License> =
      ArrayList()

    override fun getCount(): Int {
      return licenses.size
    }

    override fun getItem(position: Int): Any {
      return licenses[position]
    }

    override fun getItemId(position: Int): Long {
      return position.toLong()
    }

    override fun areAllItemsEnabled(): Boolean {
      return false
    }

    override fun isEnabled(position: Int): Boolean {
      return false
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
      val view = if (convertView == null) {
        val binding =
          RowLicenseBinding.inflate(LayoutInflater.from(requireActivity()), parent, false)
        binding.root.tag = binding
        binding.root
      } else {
        convertView
      }

      val item = licenses[position]

      val binding = view.tag as RowLicenseBinding
      binding.library.setText(item.library)
      binding.license.setText(item.license)
      return view
    }

    init {
      licenses.add(
        License(
          R.string.license_android_support_library,
          R.string.license_android_support_library_text
        )
      )
      licenses.add(License(R.string.license_gson, R.string.license_gson_text))
      licenses.add(License(R.string.license_dagger, R.string.license_dagger_text))
      licenses.add(License(R.string.license_okhttp, R.string.license_okhttp_text))
      licenses.add(License(R.string.license_picasso, R.string.license_picasso_text))
      licenses.add(
        License(
          R.string.license_picasso_downloader,
          R.string.license_picasso_downloader_text
        )
      )
      licenses.add(License(R.string.license_retrofit, R.string.license_retrofit_text))
      licenses.add(License(R.string.license_timber, R.string.license_timber_text))
      licenses.add(License(R.string.license_tmdb_java, R.string.license_tmdb_java_text))
      licenses.add(License(R.string.license_schematic, R.string.license_schematic_text))
    }
  }
}
