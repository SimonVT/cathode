/*
 * Copyright (C) 2017 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui.show

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.viewModels
import androidx.lifecycle.Observer
import net.simonvt.cathode.R
import net.simonvt.cathode.R.layout
import net.simonvt.cathode.common.ui.fragment.RefreshableAppBarFragment
import net.simonvt.cathode.common.util.DateStringUtils
import net.simonvt.cathode.common.util.Ids
import net.simonvt.cathode.common.util.guava.Preconditions
import net.simonvt.cathode.databinding.HistoryFragmentBinding
import net.simonvt.cathode.databinding.HistoryRowBinding
import net.simonvt.cathode.entity.Episode
import net.simonvt.cathode.images.ImageType.STILL
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.provider.util.DataHelper
import net.simonvt.cathode.sync.scheduler.EpisodeTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment.Type.EMPTY
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment.Type.ERROR
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment.Type.ITEM
import net.simonvt.cathode.ui.show.EpisodeHistoryFragment.Type.LOADING
import net.simonvt.cathode.ui.show.EpisodeHistoryLiveData.Result
import java.util.ArrayList
import javax.inject.Inject

class EpisodeHistoryFragment @Inject constructor(
  private val episodeScheduler: EpisodeTaskScheduler,
  private val viewModelFactory: CathodeViewModelFactory
) : RefreshableAppBarFragment() {

  enum class Type {
    LOADING, ERROR, EMPTY, ITEM
  }

  private var episode: Episode? = null
  private var result: Result? = null
  private var episodeId: Long = 0
  private var showTitle: String? = null

  private val viewModel: EpisodeHistoryViewModel by viewModels { viewModelFactory }

  private var _binding: HistoryFragmentBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    episodeId = requireArguments().getLong(ARG_EPISODEID)
    showTitle = requireArguments().getString(ARG_SHOW_TITLE)
    setTitle(showTitle)
    viewModel.setEpisodeId(episodeId)
    viewModel.episode!!.observe(this, Observer { episode -> setEpisode(episode) })
    viewModel.history!!.observe(
      this,
      Observer { result ->
        setResult(result)
        setRefreshing(false)
      })
  }

  override fun createView(inflater: LayoutInflater, container: ViewGroup?, inState: Bundle?): View {
    _binding = HistoryFragmentBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    setResult(result)
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  fun onRemoveHistoryItem(historyId: Long, position: Int) {
    result!!.getItems().removeAt(position)
    setResult(result)
    episodeScheduler.removeHistoryItem(episodeId, historyId, result!!.getItems().size == 0)
  }

  override fun onRefresh() {
    viewModel.history!!.loadData()
  }

  private fun setEpisode(episode: Episode) {
    this.episode = episode
    val title = DataHelper.getEpisodeTitle(
      requireContext(),
      episode.title,
      episode.season,
      episode.episode,
      episode.watched
    )
    val firstAiredString =
      DateStringUtils.getAirdateInterval(requireContext(), episode.firstAired, true)
    binding.top.topSubtitle.text = firstAiredString
    showTitle = episode.showTitle
    setTitle(showTitle)
    binding.top.topTitle.text = title
    val screenshotUri = ImageUri.create(ImageUri.ITEM_EPISODE, STILL, episodeId)
    setBackdrop(screenshotUri)
  }

  private fun typeOrClear(parent: ViewGroup, type: Type): Boolean {
    if (parent.childCount > 0) {
      val firstChild = parent.getChildAt(0)
      val childType = firstChild.getTag(R.id.historyType) as Type
      if (childType == type) {
        return true
      } else {
        parent.removeAllViews()
      }
    }
    return false
  }

  private fun setResult(result: Result?) {
    this.result = result
    if (view == null) {
      return
    }
    if (result == null) {
      if (typeOrClear(binding.content.content, LOADING)) {
        return
      }
      val v = LayoutInflater.from(binding.content.content.context)
        .inflate(layout.history_progress, binding.content.content, false)
      v.setTag(R.id.historyType, LOADING)
      binding.content.content.addView(v)
    } else if (!result.isSuccessful()) {
      if (typeOrClear(binding.content.content, ERROR)) {
        return
      }
      val v = LayoutInflater.from(binding.content.content.context)
        .inflate(layout.history_error, binding.content.content, false)
      v.setTag(R.id.historyType, ERROR)
      binding.content.content.addView(v)
    } else if (result.getItems().size == 0) {
      if (typeOrClear(binding.content.content, EMPTY)) {
        return
      }
      val v = LayoutInflater.from(binding.content.content.context)
        .inflate(layout.history_empty, binding.content.content, false)
      v.setTag(R.id.historyType, ERROR)
      binding.content.content.addView(v)
    } else {
      val items = result.getItems()
      val ids: MutableList<Long> = ArrayList()
      for (item in items) {
        ids.add(item.historyId)
      }
      for (i in binding.content.content.childCount - 1 downTo 0) {
        val v = binding.content.content.getChildAt(i)
        if (v.getTag(R.id.historyType) !== ITEM) {
          binding.content.content.removeViewAt(i)
          continue
        }
        val id = v.getTag(R.id.historyId) as Long
        if (!ids.contains(id)) {
          binding.content.content.removeViewAt(i)
        }
      }
      var i = 0
      val size = items.size
      while (i < size) {
        val item = items[i]
        val position = i
        var v: View? = null
        if (i < binding.content.content.childCount) {
          v = binding.content.content.getChildAt(i)
        }
        if (v != null) {
          val id = v.getTag(R.id.historyId) as Long
          if (item.historyId == id) {
            val itemBinding = v.tag as HistoryRowBinding
            itemBinding.watchedAt.text = item.watchedAt
            itemBinding.remove.setOnClickListener { onRemoveHistoryItem(item.historyId, position) }
            i++
            continue
          }
        }
        val itemBinding = HistoryRowBinding.inflate(
          LayoutInflater.from(binding.content.content.context),
          binding.content.content,
          false
        )
        itemBinding.root.tag = itemBinding
        itemBinding.root.setTag(R.id.historyType, ITEM)
        itemBinding.root.setTag(R.id.historyId, item.historyId)
        itemBinding.watchedAt.text = item.watchedAt
        itemBinding.remove.setOnClickListener { onRemoveHistoryItem(item.historyId, position) }
        binding.content.content.addView(itemBinding.root, i)
        i++
      }
    }
  }

  companion object {
    private const val TAG = "net.simonvt.cathode.ui.show.EpisodeHistoryFragment"
    private const val ARG_EPISODEID = "$TAG.episodeId"
    private const val ARG_SHOW_TITLE = "$TAG.showTitle"

    fun getTag(episodeId: Long): String {
      return TAG + "/" + episodeId + "/history/" + Ids.newId()
    }

    fun getArgs(episodeId: Long, showTitle: String?): Bundle {
      Preconditions.checkArgument(
        episodeId >= 0,
        "episodeId must be >= 0, was $episodeId"
      )
      val args = Bundle()
      args.putLong(ARG_EPISODEID, episodeId)
      args.putString(ARG_SHOW_TITLE, showTitle)
      return args
    }
  }
}
