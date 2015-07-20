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
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.text.style.ImageSpan;
import android.util.AttributeSet;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AutoCompleteTextView;
import android.widget.Filterable;
import android.widget.LinearLayout;
import android.widget.ListAdapter;
import butterknife.Bind;
import butterknife.ButterKnife;
import net.simonvt.cathode.R;

public class SearchView extends LinearLayout {

  public interface SearchViewListener {

    void onTextChanged(String newText);

    void onSubmit(String query);

    void onSuggestionSelected(Object suggestion);
  }

  @Bind(R.id.search_input) AutoCompleteTextView inputView;

  @Bind(R.id.search_clear) View clearView;

  private SearchViewListener listener;

  private int maxWidth;

  public SearchView(Context context) {
    super(context);
    init(context);
  }

  public SearchView(Context context, AttributeSet attrs) {
    super(context, attrs);
    init(context);
  }

  public SearchView(Context context, AttributeSet attrs, int defStyle) {
    super(context, attrs, defStyle);
    init(context);
  }

  private void init(Context context) {
    maxWidth = getResources().getDimensionPixelSize(R.dimen.searchViewMaxWidth);
  }

  @Override protected void onFinishInflate() {
    super.onFinishInflate();
    ButterKnife.bind(this);

    inputView.setOnItemClickListener(suggestionListener);
    inputView.setOnKeyListener(inputKeyListener);
    inputView.addTextChangedListener(inputListener);
    inputView.requestFocus();

    SpannableStringBuilder ssb = new SpannableStringBuilder("   ");
    ssb.append(getResources().getString(R.string.action_search));
    Drawable searchIcon = getContext().getResources().getDrawable(R.drawable.ic_action_search);
    int textSize = (int) (inputView.getTextSize() * 1.25);
    searchIcon.setBounds(0, 0, textSize, textSize);
    ssb.setSpan(new ImageSpan(searchIcon), 1, 2, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
    inputView.setHint(ssb);

    post(showIme);

    clearView.setOnClickListener(clearListener);
    clearView.setVisibility(inputView.getText().toString().isEmpty() ? INVISIBLE : VISIBLE);
  }

  private Runnable showIme = new Runnable() {
    @Override public void run() {
      InputMethodManager imm =
          (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);
      if (imm != null) {
        imm.showSoftInput(inputView, 0);
      }
    }
  };

  @Override public void clearFocus() {
    super.clearFocus();
    inputView.clearFocus();

    removeCallbacks(showIme);
    InputMethodManager imm =
        (InputMethodManager) getContext().getSystemService(Context.INPUT_METHOD_SERVICE);

    if (imm != null) {
      imm.hideSoftInputFromWindow(getWindowToken(), 0);
    }
  }

  @Override public boolean requestFocus(int direction, Rect previouslyFocusedRect) {
    inputView.requestFocus();
    post(showIme);
    return true;
  }

  public void setListener(SearchViewListener listener) {
    this.listener = listener;
  }

  private OnClickListener clearListener = new OnClickListener() {
    @Override public void onClick(View v) {
      inputView.setText("");
    }
  };

  private AdapterView.OnItemClickListener suggestionListener =
      new AdapterView.OnItemClickListener() {
        @Override public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
          if (listener != null) {
            listener.onSuggestionSelected(parent.getItemAtPosition(position));
          }
        }
      };

  private OnKeyListener inputKeyListener = new OnKeyListener() {
    @Override public boolean onKey(View v, int keyCode, KeyEvent event) {
      if (event.getAction() == KeyEvent.ACTION_UP) {
        if (keyCode == KeyEvent.KEYCODE_ENTER) {
          if (listener != null) listener.onSubmit(inputView.getText().toString());
        }
      }
      return false;
    }
  };

  private TextWatcher inputListener = new TextWatcher() {
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
      if (listener != null) listener.onTextChanged(s.toString());
      clearView.setVisibility(TextUtils.isEmpty(inputView.getText()) ? INVISIBLE : VISIBLE);
    }

    @Override public void afterTextChanged(Editable s) {
    }
  };

  public <T extends ListAdapter & Filterable> void setAdapter(T adapter) {
    inputView.setAdapter(adapter);
  }

  public void setQuery(CharSequence query) {
    inputView.setText(query);
  }

  public CharSequence getQuery() {
    return inputView.getText();
  }

  private int getPreferredWidth() {
    return getContext().getResources()
        .getDimensionPixelSize(
            android.support.v7.appcompat.R.dimen.abc_search_view_preferred_width);
  }

  @Override protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {

    int widthMode = MeasureSpec.getMode(widthMeasureSpec);
    int width = MeasureSpec.getSize(widthMeasureSpec);

    switch (widthMode) {
      case MeasureSpec.AT_MOST:
        // If there is an upper limit, don't exceed maximum width (explicit or implicit)
        if (maxWidth > 0) {
          width = Math.min(maxWidth, width);
        } else {
          width = Math.min(getPreferredWidth(), width);
        }
        break;
      case MeasureSpec.EXACTLY:
        // If an exact width is specified, still don't exceed any specified maximum width
        if (maxWidth > 0) {
          width = Math.min(maxWidth, width);
        }
        break;
      case MeasureSpec.UNSPECIFIED:
        // Use maximum width, if specified, else preferred width
        width = maxWidth > 0 ? maxWidth : getPreferredWidth();
        break;
    }
    widthMode = MeasureSpec.EXACTLY;
    super.onMeasure(MeasureSpec.makeMeasureSpec(width, widthMode), heightMeasureSpec);
  }
}
