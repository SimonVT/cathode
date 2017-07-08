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
package net.simonvt.cathode.widget;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.support.v7.widget.AppCompatTextView;
import android.util.AttributeSet;
import java.lang.ref.WeakReference;
import net.simonvt.cathode.common.util.DateUtils;

public class TimeStamp extends AppCompatTextView {

  private static final int MSG_UPDATE = 1;

  private long timeInMillis;

  private boolean extended;

  private TimeStampHandler handler = new TimeStampHandler();

  public TimeStamp(Context context) {
    super(context);
  }

  public TimeStamp(Context context, AttributeSet attrs) {
    super(context, attrs);
  }

  public TimeStamp(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
  }

  public void setTimeInMillis(long timeInMillis) {
    this.timeInMillis = timeInMillis;

    updateTimestamp();
  }

  public void setExtended(boolean extended) {
    this.extended = extended;
  }

  private void updateTimestamp() {
    final String timeStamp = DateUtils.millisToString(getContext(), timeInMillis, extended);
    setText(timeStamp);

    long nextUpdate = DateUtils.timeUntilUpdate(timeInMillis);
    nextUpdate = Math.min(nextUpdate, 1 * DateUtils.HOUR_IN_MILLIS);
    handler.removeMessages(MSG_UPDATE);

    Message m = handler.obtainMessage(MSG_UPDATE);
    m.what = MSG_UPDATE;
    m.obj = new WeakReference<>(this);
    handler.sendMessageDelayed(m, nextUpdate);
  }

  @Override protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    if (timeInMillis > 0) {
      updateTimestamp();
    }
  }

  @Override protected void onDetachedFromWindow() {
    handler.removeMessages(MSG_UPDATE);
    super.onDetachedFromWindow();
  }

  private static class TimeStampHandler extends Handler {

    TimeStampHandler() {
      super(Looper.getMainLooper());
    }

    @Override public void handleMessage(Message msg) {
      switch (msg.what) {
        case MSG_UPDATE:
          WeakReference<TimeStamp> ref = (WeakReference<TimeStamp>) msg.obj;
          TimeStamp ts = ref.get();
          if (ts != null) {
            ts.updateTimestamp();
          }
          break;
      }
    }
  }
}
