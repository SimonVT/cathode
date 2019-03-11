package net.simonvt.cathode.ui.search

import android.view.View
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R.id
import net.simonvt.cathode.api.enumeration.ItemType
import net.simonvt.cathode.common.widget.CircularProgressIndicator
import net.simonvt.cathode.common.widget.RemoteImageView
import net.simonvt.cathode.common.widget.find
import net.simonvt.cathode.images.ImageType
import net.simonvt.cathode.images.ImageUri
import net.simonvt.cathode.search.SearchHandler.SearchItem
import net.simonvt.cathode.ui.search.SearchAdapter.OnResultClickListener

class ResultViewHolder(view: View, listener: OnResultClickListener) : ViewHolder(view) {

  val poster: RemoteImageView = view.find(id.poster)
  val title: TextView = view.find(id.title)
  val overview: TextView = view.find(id.overview)
  val rating: CircularProgressIndicator = view.find(id.rating)

  var item: SearchItem? = null

  init {
    view.setOnClickListener {
      val item = this.item
      if (item != null) {
        if (item.itemType == ItemType.SHOW) {
          listener.onShowClicked(item.itemId, item.title, item.overview)
        } else {
          listener.onMovieClicked(item.itemId, item.title, item.overview)
        }
      }
    }
  }

  fun update(item: SearchItem) {
    this.item = item

    val posterUri = when (item.itemType) {
      ItemType.SHOW -> ImageUri.create(ImageUri.ITEM_SHOW, ImageType.POSTER, item.itemId)
      else -> ImageUri.create(ImageUri.ITEM_MOVIE, ImageType.POSTER, item.itemId)
    }

    poster.setImage(posterUri)
    title.text = item.title
    overview.text = item.overview
    rating.setValue(item.rating)
  }
}
