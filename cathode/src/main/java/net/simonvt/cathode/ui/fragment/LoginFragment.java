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
package net.simonvt.cathode.ui.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import butterknife.InjectView;
import com.squareup.otto.Bus;
import com.squareup.otto.Produce;
import com.squareup.otto.Subscribe;
import javax.inject.Inject;
import net.simonvt.cathode.CathodeApp;
import net.simonvt.cathode.R;
import net.simonvt.cathode.api.UserCredentials;
import net.simonvt.cathode.api.body.CreateAccountBody;
import net.simonvt.cathode.api.entity.Response;
import net.simonvt.cathode.api.service.AccountService;
import net.simonvt.cathode.event.CreateUserFailedEvent;
import net.simonvt.cathode.event.LoginEvent;
import net.simonvt.cathode.event.LoginFailedEvent;
import net.simonvt.cathode.event.LoginSuccessEvent;
import net.simonvt.cathode.event.LoginTaskExecuting;
import net.simonvt.cathode.event.MessageEvent;
import net.simonvt.cathode.event.UserCreatedEvent;
import net.simonvt.cathode.remote.TraktTaskQueue;
import net.simonvt.cathode.remote.sync.SyncTask;
import net.simonvt.cathode.util.ApiUtils;
import retrofit.RetrofitError;

public class LoginFragment extends BaseFragment {

  private static final String STATE_CREATE_NEW_ENABLED =
      "net.simonvt.cathode.ui.fragment.LoginFragment.createNewEnabled";

  @Inject UserCredentials credentials;
  @Inject TraktTaskQueue queue;
  @Inject Bus bus;

  @InjectView(R.id.username) EditText usernameInput;
  @InjectView(R.id.password) EditText passwordInput;
  @InjectView(R.id.email) EditText emailInput;
  @InjectView(R.id.createNew) CheckBox createNew;
  @InjectView(R.id.login) Button login;

  private Context appContext;

  private boolean createNewEnabled;

  private boolean isTaskRunning = false;

  @Override public void onCreate(Bundle inState) {
    super.onCreate(inState);
    CathodeApp.inject(getActivity(), this);
    appContext = getActivity().getApplicationContext();
    bus.register(this);

    if (inState != null) createNewEnabled = inState.getBoolean(STATE_CREATE_NEW_ENABLED);
  }

  @Override public void onSaveInstanceState(Bundle outState) {
    super.onSaveInstanceState(outState);
    outState.putBoolean(STATE_CREATE_NEW_ENABLED, createNewEnabled);
  }

  @Override public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle inState) {
    return inflater.inflate(R.layout.fragment_login, container, false);
  }

  @Override public void onViewCreated(View view, Bundle inState) {
    super.onViewCreated(view, inState);
    login.setOnClickListener(onLoginListener);
    usernameInput.addTextChangedListener(textChanged);
    if (CathodeApp.accountExists(getActivity())) {
      usernameInput.setText(CathodeApp.getAccount(getActivity()).name);
    }
    passwordInput.addTextChangedListener(textChanged);
    passwordInput.setImeOptions(
        createNewEnabled ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
    emailInput.setEnabled(createNewEnabled);

    createNew.setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View view) {
        final boolean isEnabled = ((CheckBox) view).isChecked();
        createNewEnabled = isEnabled;
        emailInput.setEnabled(isEnabled);

        passwordInput.setImeOptions(
            isEnabled ? EditorInfo.IME_ACTION_NEXT : EditorInfo.IME_ACTION_DONE);
        login.setText(isEnabled ? R.string.create_account : R.string.login);
      }
    });

    login.setText(createNewEnabled ? R.string.create_account : R.string.login);
    updateUiEnabled();
  }

  @Override public String getTitle() {
    return getResources().getString(R.string.login);
  }

  @Override public void onDestroy() {
    bus.unregister(this);
    super.onDestroy();
  }

  private View.OnClickListener onLoginListener = new View.OnClickListener() {
    @Override public void onClick(View view) {
      String username = usernameInput.getText().toString();
      String password = passwordInput.getText().toString();
      String email = emailInput.getText().toString();
      final boolean createNewUser = createNew.isChecked();

      isTaskRunning = true;
      updateUiEnabled();
      if (createNewUser) {
        new CreateAccountAsync(getActivity(), username, password, email).execute();
      } else {
        new LoginAsync(getActivity(), username, password).execute();
      }
    }
  };

  private TextWatcher textChanged = new TextWatcher() {
    @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {
    }

    @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
    }

    @Override public void afterTextChanged(Editable s) {
      updateUiEnabled();
    }
  };

  @Subscribe public void onLoginTaskExecuting(LoginTaskExecuting event) {
    isTaskRunning = true;
    updateUiEnabled();
  }

  private void updateUiEnabled() {
    if (login != null) {
      login.setEnabled(!isTaskRunning && usernameInput.length() > 0 && passwordInput.length() > 0);
    }
  }

  @Subscribe public void onCreateUsedFailed(CreateUserFailedEvent event) {
    isTaskRunning = false;
    updateUiEnabled();
    if (event.getError() != null) {
      bus.post(new MessageEvent(event.getError()));
    } else {
      bus.post(new MessageEvent(R.string.create_user_failed));
    }
  }

  @Subscribe public void onUserCreated(UserCreatedEvent event) {
    isTaskRunning = false;
    updateUiEnabled();
    bus.post(new MessageEvent(R.string.login_success));

    final String username = event.getUsername();
    final String password = event.getPassword();

    credentials.setCredentials(username, ApiUtils.getSha(password));
    queue.add(new SyncTask());

    bus.post(new LoginEvent(username, password));

    CathodeApp.setupAccount(appContext, username, password);
  }

  @Subscribe public void onLoginFailed(LoginFailedEvent event) {
    isTaskRunning = false;
    updateUiEnabled();
    credentials.setCredentials(null, null);
    bus.post(new MessageEvent(R.string.wrong_password));
  }

  @Subscribe public void onLoginSuccess(LoginSuccessEvent event) {
    isTaskRunning = false;
    updateUiEnabled();
    bus.post(new MessageEvent(R.string.login_success));

    final String username = event.getUsername();
    final String password = event.getPassword();

    credentials.setCredentials(username, ApiUtils.getSha(password));
    queue.add(new SyncTask());

    bus.post(new LoginEvent(username, password));

    CathodeApp.setupAccount(appContext, username, password);
  }

  public static final class CreateAccountAsync extends AsyncTask<Void, Void, Response> {

    @Inject AccountService accountService;

    @Inject Bus bus;

    private String username;
    private String password;
    private String email;

    private CreateAccountAsync(Context context, String username, String password, String email) {
      CathodeApp.inject(context, this);
      this.username = username;
      this.password = password;
      this.email = email;
      bus.register(this);
    }

    @Produce public LoginTaskExecuting produceTaskExecutingEvent() {
      return new LoginTaskExecuting();
    }

    @Override protected Response doInBackground(Void... voids) {
      try {
        Response r = accountService.create(new CreateAccountBody(username, password, email));

        return r;
      } catch (RetrofitError e) {
        e.printStackTrace();
      }

      return null;
    }

    @Override protected void onPostExecute(Response response) {
      if (response != null && response.getError() == null) {
        bus.post(new UserCreatedEvent(username, password, email));
      } else {
        String error = null;
        if (response != null) {
          error = response.getError();
        }
        bus.post(new CreateUserFailedEvent(username, password, email, error));
      }
      bus.unregister(this);
    }
  }

  public static final class LoginAsync extends AsyncTask<Void, Void, Boolean> {

    @Inject AccountService accountService;

    @Inject UserCredentials credentials;

    @Inject Bus bus;

    private String username;
    private String password;

    private LoginAsync(Context context, String username, String password) {
      CathodeApp.inject(context, this);
      this.username = username;
      this.password = password;
      credentials.setCredentials(username, ApiUtils.getSha(password));
      bus.register(this);
    }

    @Produce public LoginTaskExecuting produceTaskExecutingEvent() {
      return new LoginTaskExecuting();
    }

    @Override protected Boolean doInBackground(Void... voids) {
      try {
        Response r = accountService.test();

        return r.getError() == null;
      } catch (RetrofitError e) {
        e.printStackTrace();
      }

      return false;
    }

    @Override protected void onPostExecute(Boolean success) {
      if (success) {
        bus.post(new LoginSuccessEvent(username, password));
      } else {
        bus.post(new LoginFailedEvent(username, password));
      }
      bus.unregister(this);
    }
  }
}
