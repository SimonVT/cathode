package net.simonvt.cathode.api.entity

data class NotFound(
  val movies: List<Movie>? = null,
  val shows: List<Show>? = null,
  val seasons: List<Season>? = null,
  val episodes: List<Episode>? = null
)
