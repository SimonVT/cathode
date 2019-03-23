package net.simonvt.cathode.api.body

class IdsBody private constructor(
  val movies: List<TraktIdItem>,
  val shows: List<ShowTraktId>,
  val people: List<TraktIdItem>
) {

  class Builder {

    val movies = mutableListOf<TraktIdItem>()
    val shows = mutableListOf<ShowTraktId>()
    val people = mutableListOf<TraktIdItem>()

    fun movie(traktId: Long): Builder {
      val movie = movies.firstOrNull { it.ids.trakt == traktId }
      if (movie == null) {
        movies.add(TraktIdItem.withId(traktId))
      }
      return this
    }

    fun show(traktId: Long): Builder {
      val show = shows.firstOrNull { it.ids.trakt == traktId }
      if (show == null) {
        shows.add(ShowTraktId(TraktId(traktId)))
      }
      return this
    }

    fun season(showTraktId: Long, season: Int): Builder {
      var show: ShowTraktId? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = ShowTraktId(TraktId(showTraktId))
        shows.add(show)
      }

      val ratingSeason = show.seasons.firstOrNull { it.number == season }
      if (ratingSeason == null) {
        show.seasons.add(SeasonNumber(season))
      }

      return this
    }

    fun episode(showTraktId: Long, season: Int, episode: Int): Builder {
      var show: ShowTraktId? = shows.firstOrNull { it.ids.trakt == showTraktId }
      if (show == null) {
        show = ShowTraktId(TraktId(showTraktId))
        shows.add(show)
      }

      var idSeason = show.seasons.firstOrNull { it.number == season }
      if (idSeason == null) {
        idSeason = SeasonNumber(season)
        show.seasons.add(idSeason)
      }

      val idEpisode = idSeason.episodes.firstOrNull { it.number == episode }
      if (idEpisode == null) {
        idSeason.episodes.add(EpisodeNumber(episode))
      }

      return this
    }

    fun person(traktId: Long): Builder {
      val person = movies.firstOrNull { it.ids.trakt == traktId }
      if (person == null) {
        people.add(TraktIdItem.withId(traktId))
      }
      return this
    }

    fun build(): IdsBody {
      return IdsBody(movies, shows, people)
    }
  }
}
