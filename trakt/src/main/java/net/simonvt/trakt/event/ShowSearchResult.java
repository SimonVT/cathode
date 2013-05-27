package net.simonvt.trakt.event;

import java.util.List;

public class ShowSearchResult {

    private List<Long> mShowIds;

    public ShowSearchResult(List<Long> showIds) {
        mShowIds = showIds;
    }

    public List<Long> getShowIds() {
        return mShowIds;
    }
}
