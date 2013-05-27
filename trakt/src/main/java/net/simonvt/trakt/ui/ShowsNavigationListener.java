package net.simonvt.trakt.ui;

public interface ShowsNavigationListener {

    void onDisplayShow(long showId, LibraryType type);

    void onDisplaySeasons(long showId, LibraryType type);

    void onDisplaySeason(long showId, long seasonId, LibraryType type);

    void onDisplayEpisode(long episodeId, LibraryType type);

    void onSearchShow(String query);
}
