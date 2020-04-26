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
package net.simonvt.cathode.ui.suggestions

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.viewpager.widget.PagerAdapter
import net.simonvt.cathode.common.ui.fragment.BaseFragment
import net.simonvt.cathode.databinding.FragmentSuggestionsBinding
import net.simonvt.cathode.ui.NavigationListener

abstract class SuggestionsFragment : BaseFragment() {

  private var _binding: FragmentSuggestionsBinding? = null
  protected val binding get() = _binding!!

  protected var navigationListener: NavigationListener? = null

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun displaysMenuIcon(): Boolean {
    return amITopLevel()
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = FragmentSuggestionsBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    binding.pager.adapter = adapter
    binding.pager.offscreenPageLimit = 2
    binding.tabLayout.setupWithViewPager(binding.pager)
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  protected abstract val adapter: PagerAdapter?
}
