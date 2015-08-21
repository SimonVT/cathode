/*
 * Copyright (C) 2015 Simon Vig Therkildsen
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

package net.simonvt.cathode.module;

import android.content.Context;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.jobqueue.Job;
import net.simonvt.cathode.jobqueue.JobInjector;

public class JobInjectorImpl implements JobInjector {

  private static volatile JobInjectorImpl instance;

  public static JobInjectorImpl getInstance(Context context) {
    if (instance == null) {
      synchronized (JobInjectorImpl.class) {
        if (instance == null) {
          instance = new JobInjectorImpl(context);
        }
      }
    }

    return instance;
  }

  private Context context;

  public JobInjectorImpl(Context context) {
    this.context = context;
  }

  @Override public void injectInto(Job job) {
    CathodeApp.inject(context, job);
  }
}
