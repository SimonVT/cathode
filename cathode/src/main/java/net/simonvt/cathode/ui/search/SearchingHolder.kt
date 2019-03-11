package net.simonvt.cathode.ui.search

import android.graphics.drawable.Animatable
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import net.simonvt.cathode.R.id
import net.simonvt.cathode.common.widget.find

class SearchingHolder constructor(view: View) : ViewHolder(view) {

  val searching: AppCompatImageView = view.find(id.searching)
  val drawable = searching.drawable as Animatable
}
