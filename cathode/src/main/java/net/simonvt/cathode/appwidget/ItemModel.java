/*
 * Copyright (C) 2017 Simon Vig Therkildsen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.simonvt.cathode.appwidget;

import android.content.Context;
import android.text.format.DateUtils;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import net.simonvt.cathode.common.util.DateStringUtils;
import net.simonvt.cathode.entity.NextEpisode;
import net.simonvt.cathode.entity.Show;
import net.simonvt.cathode.entity.ShowWithEpisode;
import net.simonvt.cathode.provider.util.DataHelper;

public class ItemModel {

  private List<WidgetItem> widgetItems;

  private ItemModel(List<WidgetItem> widgetItems) {
    this.widgetItems = widgetItems;
  }

  public int getItemCount() {
    return widgetItems == null ? 0 : widgetItems.size();
  }

  public int getItemType(int position) {
    return widgetItems.get(position).getItemType();
  }

  public DayInfo getDay(int position) {
    return (DayInfo) widgetItems.get(position);
  }

  public ItemInfo getItem(int position) {
    return (ItemInfo) widgetItems.get(position);
  }

  public long getNextUpdateTime() {
    // Update at least every six hours
    long updateTime = System.currentTimeMillis() + 6 * DateUtils.HOUR_IN_MILLIS;

    if (getItemCount() > 0) {
      ItemInfo info = getItem(1);
      final long airTime = info.getFirstAired() + DateUtils.HOUR_IN_MILLIS;
      if (airTime < updateTime) {
        updateTime = airTime;
      }
    }

    // Update at most once an hour
    updateTime = Math.max(updateTime, DateUtils.HOUR_IN_MILLIS);

    return updateTime;
  }

  public static ItemModel fromItems(Context context, List<ShowWithEpisode> showsWithEpisodes) {
    List<WidgetItem> items = new ArrayList<>();

    Calendar calendar = Calendar.getInstance();
    calendar.set(Calendar.HOUR_OF_DAY, 0);
    calendar.set(Calendar.MINUTE, 0);

    DayInfo lastDay = null;

    for (ShowWithEpisode showWithEpisode : showsWithEpisodes) {
      Show show = showWithEpisode.getShow();
      NextEpisode episode = showWithEpisode.getEpisode();

      final long firstAired = episode.getFirstAired();
      String airTime = DateStringUtils.getTimeString(context, firstAired);
      final String episodeTitle =
          DataHelper.getEpisodeTitle(context, episode.getTitle(), episode.getSeason(),
              episode.getEpisode(), episode.getWatched());

      ItemInfo item =
          new ItemInfo(show.getId(), show.getTitle(), show.getOverview(), episode.getId(),
              episodeTitle, episode.getSeason(), episode.getEpisode(), firstAired, airTime);

      calendar.setTimeInMillis(firstAired);
      calendar.set(Calendar.HOUR_OF_DAY, 0);
      calendar.set(Calendar.MINUTE, 0);

      final long itemDay = calendar.getTimeInMillis();

      if (lastDay == null || itemDay != lastDay.dayStart) {
        lastDay = new DayInfo(itemDay, DateStringUtils.getDateString(itemDay));
        items.add(lastDay);
      }

      items.add(item);
    }

    return new ItemModel(items);
  }
}
