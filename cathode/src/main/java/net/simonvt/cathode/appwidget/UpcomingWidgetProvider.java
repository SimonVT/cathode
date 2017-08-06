/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.widget.RemoteViews;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.StartPage;
import net.simonvt.cathode.ui.EpisodeDetailsActivity;
import net.simonvt.cathode.ui.HomeActivity;

public class UpcomingWidgetProvider extends AppWidgetProvider {

  @Override
  public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
    for (int appWidgetId : appWidgetIds) {

      Intent remoteViewsIntent = new Intent(context, UpcomingWidgetService.class);
      remoteViewsIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
      remoteViewsIntent.setData(Uri.parse(remoteViewsIntent.toUri(Intent.URI_INTENT_SCHEME)));

      RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget_upcoming);

      rv.setRemoteAdapter(android.R.id.list, remoteViewsIntent);
      rv.setEmptyView(android.R.id.list, android.R.id.empty);

      Intent homeIntent = new Intent(context, HomeActivity.class);
      homeIntent.setAction(HomeActivity.ACTION_SHOW_START_PAGE);
      homeIntent.putExtra(HomeActivity.EXTRA_START_PAGE, StartPage.SHOWS_UPCOMING);

      PendingIntent homePI =
          PendingIntent.getActivity(context, 0, homeIntent, PendingIntent.FLAG_UPDATE_CURRENT);
      rv.setOnClickPendingIntent(R.id.header, homePI);

      Intent rowClickTemplate = new Intent(context, EpisodeDetailsActivity.class);
      rowClickTemplate.setData(Uri.parse(rowClickTemplate.toUri(Intent.URI_INTENT_SCHEME)));
      PendingIntent piTemplate = PendingIntent.getActivity(context, 0, rowClickTemplate,
          PendingIntent.FLAG_UPDATE_CURRENT);
      rv.setPendingIntentTemplate(android.R.id.list, piTemplate);

      appWidgetManager.updateAppWidget(appWidgetId, rv);
    }
    super.onUpdate(context, appWidgetManager, appWidgetIds);
  }
}
