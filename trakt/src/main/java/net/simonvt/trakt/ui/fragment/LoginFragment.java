package net.simonvt.trakt.ui.fragment;

import butterknife.InjectView;
import retrofit.RetrofitError;

import com.squareup.otto.Bus;

import net.simonvt.trakt.R;
import net.simonvt.trakt.TraktApp;
import net.simonvt.trakt.api.ResponseParser;
import net.simonvt.trakt.api.UserCredentials;
import net.simonvt.trakt.api.body.CreateAccountBody;
import net.simonvt.trakt.api.entity.TraktResponse;
import net.simonvt.trakt.api.service.AccountService;
import net.simonvt.trakt.event.LoginEvent;
import net.simonvt.trakt.event.MessageEvent;
import net.simonvt.trakt.remote.TraktTaskQueue;
import net.simonvt.trakt.remote.sync.SyncTask;
import net.simonvt.trakt.settings.Settings;
import net.simonvt.trakt.util.ApiUtils;
import net.simonvt.trakt.util.LogWrapper;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;

import javax.inject.Inject;

public class LoginFragment extends BaseFragment {

    private static final String TAG = "LoginFragment";

    @Inject AccountService mAccountService;
    @Inject UserCredentials mCredentials;
    @Inject TraktTaskQueue mQueue;
    @Inject Bus mBus;

    @InjectView(R.id.username) EditText mUsernameInput;
    @InjectView(R.id.password) EditText mPasswordInput;
    @InjectView(R.id.email) EditText mEmailInput;
    @InjectView(R.id.createNew) CheckBox mCreateNew;
    @InjectView(R.id.login) Button mLogin;

    private String mUsername;
    private String mPassword;
    private String mEmail;

    private Context mAppContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TraktApp.inject(getActivity(), this);
        mAppContext = getActivity().getApplicationContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_login, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mLogin.setOnClickListener(mOnLoginListener);
        mUsernameInput.addTextChangedListener(mTextChanged);
        mPasswordInput.addTextChangedListener(mTextChanged);
        mCreateNew.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mEmailInput.setEnabled(((CheckBox) view).isChecked());
            }
        });

        mLogin.setEnabled(mUsernameInput.length() > 0 && mPasswordInput.length() > 0);
    }

    @Override
    public String getTitle() {
        return getResources().getString(R.string.login);
    }

    private View.OnClickListener mOnLoginListener = new View.OnClickListener() {
        @Override
        public void onClick(View view) {
            mUsername = mUsernameInput.getText().toString();
            mPassword = mPasswordInput.getText().toString();
            mEmail = mEmailInput.getText().toString();
            final boolean createNewUser = mCreateNew.isChecked();

            mLogin.setEnabled(false);
            if (createNewUser) {
                new CreateAccountAsync().execute();
            } else {
                new LoginAsync(mUsername, mPassword).execute();
            }
        }
    };

    private TextWatcher mTextChanged = new TextWatcher() {
        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            mLogin.setEnabled(mUsernameInput.length() > 0 && mPasswordInput.length() > 0);
        }
    };

    private final class CreateAccountAsync extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            TraktResponse r = mAccountService.create(new CreateAccountBody(mUsername, mPassword, mEmail));
            LogWrapper.d(TAG,
                    "Error: " + r.getError() + " - Status: " + r.getStatus() + " - Message: " + r.getMessage());

            return r.getError() == null;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mBus.post(new MessageEvent(R.string.login_success));

                final String username = mUsername;
                final String password = ApiUtils.getSha(mPassword);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mAppContext);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Settings.USERNAME, username);
                editor.putString(Settings.PASSWORD, password);
                editor.apply();

                mCredentials.setCredentials(username, password);
                mQueue.add(new SyncTask());

                mBus.post(new LoginEvent(mUsername, mPassword));

                TraktApp.setupAccount(mAppContext);

            } else {
                mLogin.setEnabled(true);
                mBus.post(new MessageEvent(R.string.create_user_failed));
            }
        }
    }

    private final class LoginAsync extends AsyncTask<Void, Void, Boolean> {

        private LoginAsync(String username, String password) {
            mCredentials.setCredentials(username, ApiUtils.getSha(password));
        }

        @Override
        protected Boolean doInBackground(Void... voids) {
            try {
                TraktResponse r = mAccountService.test();
                LogWrapper.d(TAG,
                        "Error: " + r.getError() + " - Status: " + r.getStatus() + " - Message: " + r.getMessage());

                return r.getError() == null;

            } catch (RetrofitError e) {
                ResponseParser parser = new ResponseParser();
                TraktApp.inject(mAppContext, parser);
                TraktResponse r = parser.tryParse(e);
                if (r != null) {
                    LogWrapper.d(TAG,
                            "Error: " + r.getError() + " - Status: " + r.getStatus() + " - Message: " + r.getMessage());
                }
            }

            return false;
        }

        @Override
        protected void onPostExecute(Boolean success) {
            if (success) {
                mBus.post(new MessageEvent(R.string.login_success));

                final String username = mUsername;
                final String password = ApiUtils.getSha(mPassword);

                SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(mAppContext);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(Settings.USERNAME, username);
                editor.putString(Settings.PASSWORD, password);
                editor.apply();

                mCredentials.setCredentials(username, password);
                mQueue.add(new SyncTask());

                mBus.post(new LoginEvent(mUsername, mPassword));

                TraktApp.setupAccount(mAppContext);

            } else {
                mLogin.setEnabled(true);
                mCredentials.setCredentials(null, null);
                mBus.post(new MessageEvent(R.string.wrong_password));
            }
        }
    }
}
