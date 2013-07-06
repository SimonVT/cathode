package net.simonvt.trakt.ui;

public interface ShowsNavigationListener {

    void onDisplayShow(long showId, String title, LibraryType type);

    void onDisplaySeason(long showId, long seasonId, String showTitle, int seasonNumber, LibraryType type);

    void onDisplayEpisode(long episodeId, String showTitle);

    void onStartShowSearch();
}
