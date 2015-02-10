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

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarActivity;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.Spinner;
import butterknife.ButterKnife;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.HttpStatusCode;
import net.simonvt.cathode.IntPreference;
import net.simonvt.cathode.R;
import net.simonvt.cathode.event.AuthFailedEvent;
import net.simonvt.cathode.event.RequestFailedEvent;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.remote.InitialSyncJob;

public abstract class BaseActivity extends ActionBarActivity {

  private final DebugViews debugViews = new DebugViews();

  private final DebugInjects injects = new DebugInjects();

  private SharedPreferences settings;

  @Override protected void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(this);
    CathodeApp.inject(this, injects);

    super.setContentView(R.layout.debug_home);

    settings = PreferenceManager.getDefaultSharedPreferences(this);

    Context drawerContext = new ContextThemeWrapper(this, R.style.Theme_AppCompat);
    LayoutInflater.from(drawerContext)
        .inflate(R.layout.debug_drawer, (ViewGroup) ButterKnife.findById(this, R.id.debug_drawer));

    ButterKnife.inject(debugViews, this);

    debugViews.requestFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.bus.post(new RequestFailedEvent(R.string.error_unknown_retrying));
        debugViews.drawerLayout.closeDrawers();
      }
    });

    debugViews.authFailedEvent.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.bus.post(new AuthFailedEvent());
        debugViews.drawerLayout.closeDrawers();
      }
    });

    int[] statusCodes = new int[] {
        200, 401, 404, 502,
    };
    final IntAdapter httpStatusCodeAdapter = new IntAdapter(statusCodes);
    debugViews.httpStatusCode.setAdapter(httpStatusCodeAdapter);
    debugViews.httpStatusCode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override public void onItemSelected(AdapterView<?> parent, View view, int position,
          long id) {
        injects.httpStatusCodePreference.set(httpStatusCodeAdapter.getItem(position));
      }

      @Override public void onNothingSelected(AdapterView<?> parent) {
      }
    });
    debugViews.httpStatusCode.setSelection(
        httpStatusCodeAdapter.getPositionForValue(injects.httpStatusCodePreference.get()));

    debugViews.initialSync.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        injects.jobManager.addJob(new InitialSyncJob());
      }
    });
  }

  @Override public void setContentView(int layoutResID) {
    debugViews.container.removeAllViews();
    getLayoutInflater().inflate(layoutResID, debugViews.container);
  }

  public static class DebugInjects {

    @Inject Bus bus;

    @Inject @HttpStatusCode IntPreference httpStatusCodePreference;

    @Inject JobManager jobManager;
  }

  static class DebugViews {

    @InjectView(R.id.debug_drawerLayout) DrawerLayout drawerLayout;

    @InjectView(R.id.debug_content) ViewGroup container;

    @InjectView(R.id.debug_requestFailedEvent) View requestFailedEvent;

    @InjectView(R.id.debug_authFailedEvent) View authFailedEvent;

    @InjectView(R.id.debug_networkStatusCode) Spinner httpStatusCode;

    @InjectView(R.id.debug_drawer) ViewGroup drawerContent;

    @InjectView(R.id.debug_initialSync) View initialSync;
  }
}
