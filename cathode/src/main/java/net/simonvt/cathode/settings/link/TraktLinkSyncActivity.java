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

package net.simonvt.cathode.settings.link;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import dagger.android.AndroidInjection;
import java.util.List;
import javax.inject.Inject;
import net.simonvt.cathode.R;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobManager;
import net.simonvt.cathode.ui.BaseActivity;
import net.simonvt.cathode.ui.HomeActivity;

public class TraktLinkSyncActivity extends BaseActivity {

  @Inject JobManager jobManager;

  @Inject TraktLinkSyncViewModelFactory viewModelFactory;

  private TraktLinkSyncViewModel viewModel;
  private List<Job> syncJobs;

  @Override protected void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    AndroidInjection.inject(this);
    setContentView(R.layout.link_sync_progressbar);

    viewModel = ViewModelProviders.of(this, viewModelFactory).get(TraktLinkSyncViewModel.class);
    viewModel.getLocalState().observe(this, new Observer<List<Job>>() {
      @Override public void onChanged(List<Job> jobs) {
        syncJobs = jobs;
        updateView();
      }
    });
  }

  private void updateView() {
    if (syncJobs == null) {
      setContentView(R.layout.link_sync_progressbar);
    } else {
      setContentView(R.layout.activity_trakt_link_sync);

      findViewById(R.id.sync).setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          new SyncThread(TraktLinkSyncActivity.this, jobManager, syncJobs).start();

          Intent home = new Intent(TraktLinkSyncActivity.this, HomeActivity.class);
          startActivity(home);
          finish();
        }
      });

      findViewById(R.id.forget).setOnClickListener(new View.OnClickListener() {
        @Override public void onClick(View v) {
          finish();
        }
      });
    }
  }
}
