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
package net.simonvt.cathode.ui.search

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProviders
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import butterknife.BindView
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.fragment.ToolbarGridFragment
import net.simonvt.cathode.common.widget.ErrorView
import net.simonvt.cathode.common.widget.SearchView
import net.simonvt.cathode.search.SearchHandler.SearchResult
import net.simonvt.cathode.settings.Settings
import net.simonvt.cathode.sync.scheduler.SearchTaskScheduler
import net.simonvt.cathode.ui.CathodeViewModelFactory
import net.simonvt.cathode.ui.LibraryType
import net.simonvt.cathode.ui.NavigationListener
import javax.inject.Inject

class SearchFragment @Inject constructor(
  private val viewModelFactory: CathodeViewModelFactory,
  private val searchScheduler: SearchTaskScheduler
) : ToolbarGridFragment<ViewHolder>(), SearchAdapter.OnResultClickListener {

  @BindView(R.id.errorView)
  @JvmField
  var errorView: ErrorView? = null

  private lateinit var viewModel: SearchViewModel

  private var searchView: SearchView? = null
  private var requestFocus: Boolean = false

  private var sortBy: SortBy? = null

  private val adapter = SearchAdapter(this)
  private var displayErrorView: Boolean = false
  private var resetScrollPosition: Boolean = false

  private lateinit var navigationListener: NavigationListener

  enum class SortBy(val key: String) {
    TITLE("title"), RATING("rating"), RELEVANCE("relevance");

    override fun toString(): String {
      return key
    }

    companion object {
      fun fromValue(value: String): SortBy? {
        return values().firstOrNull { it.key == value }
      }
    }
  }

  override fun onAttach(context: Context) {
    super.onAttach(context)
    navigationListener = requireActivity() as NavigationListener
  }

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    sortBy = SortBy.fromValue(
      Settings.get(requireContext()).getString(Settings.Sort.SEARCH, SortBy.TITLE.key)!!
    )

    setAdapter(adapter)
    setEmptyText(R.string.search_empty)

    if (inState == null) {
      requestFocus = true
    }

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(SearchViewModel::class.java)
    viewModel.recents.observe(
      this,
      Observer { recentQueries -> adapter.setRecentQueries(recentQueries) })
    viewModel.liveResults.observe(this, resultsObserver)
  }

  private val resultsObserver = Observer<SearchResult> { (success, results) ->
    adapter.setSearching(false)
    displayErrorView = !success
    updateErrorView()

    if (success) {
      adapter.setResults(results)
      resetScrollPosition = true
      updateScrollPosition()
      if (view != null) {
        updateScrollPosition()
      }
    } else {
      adapter.setResults(null)
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    return inflater.inflate(R.layout.search_fragment, container, false)
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    searchView = LayoutInflater.from(toolbar!!.context)
      .inflate(R.layout.search_view, toolbar, false) as SearchView
    toolbar!!.addView(searchView)

    searchView!!.setListener(object : SearchView.SearchViewListener {
      override fun onTextChanged(newText: String) {
        queryChanged(newText)
      }

      override fun onSubmit(query: String) {
        query(query)
        searchView!!.clearFocus()
      }
    })

    updateErrorView()

    if (requestFocus) {
      requestFocus = false
      searchView!!.onActionViewExpanded()
    }
  }

  override fun onDestroyView() {
    searchView = null
    super.onDestroyView()
  }

  override fun onViewStateRestored(inState: Bundle?) {
    super.onViewStateRestored(inState)
    updateScrollPosition()
  }

  private fun updateScrollPosition() {
    if (view != null && resetScrollPosition) {
      resetScrollPosition = false
      recyclerView.scrollToPosition(0)
    }
  }

  private fun updateErrorView() {
    if (displayErrorView) {
      errorView!!.show()
    } else {
      errorView!!.hide()
    }
  }

  private fun queryChanged(query: String) {
    viewModel.search(query)
  }

  private fun query(query: String) {
    if (!query.isBlank()) {
      adapter.setSearching(true)
      searchScheduler.insertRecentQuery(query)
    }

    viewModel.search(query)
  }

  override fun onShowClicked(showId: Long, title: String?, overview: String?) {
    navigationListener.onDisplayShow(showId, title, overview, LibraryType.WATCHED)
    if (title != null) {
      searchScheduler.insertRecentQuery(title)
    }
  }

  override fun onMovieClicked(movieId: Long, title: String?, overview: String?) {
    navigationListener.onDisplayMovie(movieId, title, overview)
    if (title != null) {
      searchScheduler.insertRecentQuery(title)
    }
  }

  override fun onQueryClicked(query: String) {
    searchView?.clearFocus()
    searchView?.query = query

    adapter.setSearching(true)
    query(query)
  }

  companion object {
    const val TAG = "net.simonvt.cathode.ui.search.SearchFragment"
  }
}
