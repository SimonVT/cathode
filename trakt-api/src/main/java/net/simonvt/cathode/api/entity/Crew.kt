package net.simonvt.cathode.api.entity

import com.squareup.moshi.Json

data class Crew(
  var production: List<Credit>? = null,
  var art: List<Credit>? = null,
  var crew: List<Credit>? = null,
  @Json(name = "costume & make-up")
  var costume_and_make_up: List<Credit>? = null,
  var directing: List<Credit>? = null,
  var writing: List<Credit>? = null,
  var sound: List<Credit>? = null,
  var camera: List<Credit>? = null
)
