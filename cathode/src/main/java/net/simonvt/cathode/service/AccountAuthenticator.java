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
package net.simonvt.cathode.service;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.os.Bundle;
import net.simonvt.cathode.R;

public class AccountAuthenticator extends AbstractAccountAuthenticator {

  public static boolean allowRemove;

  private Context context;

  public AccountAuthenticator(Context context) {
    super(context);
    this.context = context;
  }

  @Override
  public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
    return null;
  }

  @Override public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
      String authTokenType, String[] requiredFeatures, Bundle options)
      throws NetworkErrorException {
    AccountManager manager = AccountManager.get(context);

    final Account account =
        new Account(context.getString(R.string.accountName), context.getPackageName());
    manager.addAccountExplicitly(account, null, null);

    return null;
  }

  @Override public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account,
      Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account,
      String authTokenType, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override public String getAuthTokenLabel(String authTokenType) {
    return null;
  }

  @Override public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account,
      String authTokenType, Bundle options) throws NetworkErrorException {
    return null;
  }

  @Override public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account,
      String[] features) throws NetworkErrorException {
    return null;
  }

  @Override
  public Bundle getAccountRemovalAllowed(AccountAuthenticatorResponse response, Account account)
      throws NetworkErrorException {
    final Bundle result = new Bundle();
    result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, allowRemove);
    return result;
  }
}
