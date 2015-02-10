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

package net.simonvt.cathode.widget;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Parcel;
import android.os.Parcelable;
import android.util.AttributeSet;
import android.view.ViewPropertyAnimator;
import android.widget.TextView;
import java.util.LinkedList;
import net.simonvt.cathode.R;

public class Crouton extends TextView {

  private static final String STATE_MESSAGES = "net.simonvt.messagebar.Crouton.messages";
  private static final String STATE_CURRENT_MESSAGE =
      "net.simonvt.messagebar.Crouton.currentMessage";
  public static final String STATE_SUPER = "net.simonvt.cathode.widget.Crouton.superState";

  private static final int ANIMATION_DURATION = 600;

  private static final int HIDE_DELAY = 20000;

  private LinkedList<Message> messages = new LinkedList<>();

  private Message currentMessage;

  private boolean mShowing;

  private Handler handler = new Handler();

  public Crouton(Context context) {
    super(context);
    init(context);
  }

  public Crouton(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public Crouton(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init(context);
  }

  public Crouton(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
    super(context, attrs, defStyleAttr, defStyleRes);
    init(context);
  }

  private void init(Context context) {
    if (isInEditMode()) {
      setBackgroundColor(getResources().getColor(android.R.color.holo_red_dark));
      setText(R.string.error_unknown_retrying);
    }
  }

  public void show(String message) {
    show(message, 0xFF00FF00);
  }

  public void show(String message, int backgroundColor) {
    Message m = new Message(message, backgroundColor);
    if (mShowing) {
      messages.add(m);
    } else {
      show(m);
    }
  }

  private void show(Message message) {
    show(message, false);
  }

  private void show(Message message, boolean immediately) {
    mShowing = true;

    setTranslationY(getHeight());
    setBackgroundColor(message.backgroundColor);

    setText(message.message);
    if (immediately) {
      setTranslationY(0);
    } else {
      animator = animate();
      animator.translationY(0);
    }

    handler.postDelayed(hideRunnable, HIDE_DELAY);
  }

  public void clear() {
    messages.clear();
    hideRunnable.run();
  }

  private boolean hasMessages() {
    return messages.size() > 0;
  }

  @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) {
    super.onSizeChanged(w, h, oldw, oldh);

    if (isInEditMode()) {
      return;
    }

    if (animator != null) {
      animator.cancel();
    }

    if (!hasMessages()) {
      setTranslationY(getHeight());
    } else {
      setTranslationY(0);
    }
  }

  ViewPropertyAnimator animator;

  private final Runnable hideRunnable = new Runnable() {
    @Override
    public void run() {
      animator = animate();
      animator.translationY(getHeight()).withEndAction(new Runnable() {
        @Override public void run() {
          animator = null;

          if (hasMessages()) {
            Message message = messages.poll();
            show(message);
          }
        }
      });
    }
  };

  @Override public void onRestoreInstanceState(Parcelable state) {
    Bundle viewState = (Bundle) state;

    Parcelable superState = viewState.getParcelable(STATE_SUPER);
    super.onRestoreInstanceState(superState);

    Message currentMessage = viewState.getParcelable(STATE_CURRENT_MESSAGE);
    if (currentMessage != null) {
      show(currentMessage, true);
      Parcelable[] messages = viewState.getParcelableArray(STATE_MESSAGES);
      for (Parcelable p : messages) {
        this.messages.add((Message) p);
      }
    }
  }

  public Bundle onSaveInstanceState() {
    Bundle b = new Bundle();

    Parcelable superState = super.onSaveInstanceState();
    b.putParcelable(STATE_SUPER, superState);
    b.putParcelable(STATE_CURRENT_MESSAGE, currentMessage);

    final int count = messages.size();
    final Message[] messages = new Message[count];
    int i = 0;
    for (Message message : this.messages) {
      messages[i++] = message;
    }

    b.putParcelableArray(STATE_MESSAGES, messages);

    return b;
  }

  private static class Message implements Parcelable {

    final String message;

    final int backgroundColor;

    public Message(String message, int backgroundColor) {
      this.message = message;
      this.backgroundColor = backgroundColor;
    }

    public Message(Parcel p) {
      message = p.readString();
      backgroundColor = p.readInt();
    }

    public void writeToParcel(Parcel out, int flags) {
      out.writeString(message);
      out.writeInt(backgroundColor);
    }

    public int describeContents() {
      return 0;
    }

    public static final Parcelable.Creator<Message> CREATOR = new Parcelable.Creator<Message>() {
      public Message createFromParcel(Parcel in) {
        return new Message(in);
      }

      public Message[] newArray(int size) {
        return new Message[size];
      }
    };
  }
}
