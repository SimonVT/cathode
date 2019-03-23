package net.simonvt.cathode.api.body

data class TraktId(val trakt: Long)

class ShowTraktId(val ids: TraktId) {
  val seasons = mutableListOf<SeasonNumber>()
}

class SeasonNumber(val number: Int) {
  val episodes = mutableListOf<EpisodeNumber>()
}

class EpisodeNumber(val number: Int)

class TraktIdItem private constructor(val ids: TraktId) {

  companion object {
    @JvmStatic
    fun withId(traktId: Long) = TraktIdItem(TraktId(traktId))
  }
}
