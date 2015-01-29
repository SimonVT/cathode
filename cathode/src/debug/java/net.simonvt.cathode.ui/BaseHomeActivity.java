/*
 * Copyright (C) 2013 Simon Vig Therkildsen
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
package net.simonvt.cathode.ui;

import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.View;
import android.view.ViewGroup;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.RequestFailedEvent;

public abstract class BaseHomeActivity extends ActionBarActivity {

  @Inject Bus bus;

  private final DebugViews debugViews = new DebugViews();

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(this);

    super.setContentView(R.layout.debug_home);

    ButterKnife.inject(debugViews, this);

    debugViews.requestFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        bus.post(new RequestFailedEvent(R.string.error_unknown));
        debugViews.drawer.closeDrawers();
      }
    });

    debugViews.authFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        bus.post(new AuthFailedEvent());
        debugViews.drawer.closeDrawers();
      }
    });
  }

  @Override public void setContentView(int layoutResID) {
    debugViews.container.removeAllViews();
    getLayoutInflater().inflate(layoutResID, debugViews.container);
  }

  static class DebugViews {

    @InjectView(R.id.debug_drawerLayout) DrawerLayout drawer;

    @InjectView(R.id.debug_content) ViewGroup container;

    @InjectView(R.id.requestFailedEvent) View requestFailedEvent;

    @InjectView(R.id.authFailedEvent) View authFailedEvent;
  }
}
