package net.simonvt.cathode.ui.search

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import androidx.vectordrawable.graphics.drawable.VectorDrawableCompat
import net.simonvt.cathode.R
import net.simonvt.cathode.R.id
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.ui.search.SearchAdapter.OnResultClickListener

class RecentsViewHolder constructor(view: View, listener: OnResultClickListener) :
  ViewHolder(view) {

  val query1: TextView = view.find(id.query1)
  val query2: TextView = view.find(id.query2)
  val query3: TextView = view.find(id.query3)

  var queryOneQuery: String? = null
  var queryTwoQuery: String? = null
  var queryThreeQuery: String? = null

  init {
    val icon = VectorDrawableCompat.create(
      view.context.resources,
      R.drawable.ic_search_history_24dp, null
    )

    query1.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    query2.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)
    query3.setCompoundDrawablesWithIntrinsicBounds(icon, null, null, null)

    query1.setOnClickListener { listener.onQueryClicked(queryOneQuery!!) }
    query2.setOnClickListener { listener.onQueryClicked(queryTwoQuery!!) }
    query3.setOnClickListener { listener.onQueryClicked(queryThreeQuery!!) }
  }

  fun update(recents: List<String>) {
    val recentQueryCount = recents.size

    queryOneQuery = recents[0]
    query1.text = queryOneQuery

    if (recentQueryCount >= 2) {
      queryTwoQuery = recents[1]
      query2.text = queryTwoQuery
      query2.visibility = View.VISIBLE
    } else {
      query2.visibility = View.GONE
    }

    if (recentQueryCount >= 3) {
      queryThreeQuery = recents[2]
      query3.text = queryThreeQuery
      query3.visibility = View.VISIBLE
    } else {
      query3.visibility = View.GONE
    }
  }
}
