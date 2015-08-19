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

package net.simonvt.cathode.settings;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import net.simonvt.cathode.BuildConfig;
import net.simonvt.cathode.R;
import net.simonvt.cathode.service.AccountAuthenticator;
import net.simonvt.cathode.util.DateUtils;

public final class Accounts {

  private Accounts() {
  }

  public static boolean accountExists(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));

    return accounts.length > 0;
  }

  public static Account getAccount(Context context) {
    AccountManager accountManager = AccountManager.get(context);
    Account[] accounts = accountManager.getAccountsByType(context.getString(R.string.accountType));
    return accounts.length > 0 ? accounts[0] : null;
  }

  public static void setupAccount(Context context, String username) {
    AccountManager manager = AccountManager.get(context);

    Account account = new Account(username, context.getString(R.string.accountType));

    manager.addAccountExplicitly(account, null, null);

    ContentResolver.setIsSyncable(account, BuildConfig.PROVIDER_AUTHORITY, 1);
    ContentResolver.setSyncAutomatically(account, BuildConfig.PROVIDER_AUTHORITY, true);
    ContentResolver.addPeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY, new Bundle(),
        12 * DateUtils.HOUR_IN_SECONDS);

    ContentResolver.setIsSyncable(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, 1);
    ContentResolver.setSyncAutomatically(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, true);
    ContentResolver.addPeriodicSync(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, new Bundle(),
        12 * DateUtils.HOUR_IN_SECONDS);
  }

  public static void removeAccount(Context context) {
    AccountManager am = AccountManager.get(context);
    Account[] accounts = am.getAccountsByType(context.getString(R.string.accountType));
    for (Account account : accounts) {
      ContentResolver.removePeriodicSync(account, BuildConfig.PROVIDER_AUTHORITY, new Bundle());
      ContentResolver.removePeriodicSync(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR,
          new Bundle());
      AccountAuthenticator.allowRemove();
      am.removeAccount(account, null, null);
    }
  }

  public static void requestCalendarSync(Context context) {
    Account account = getAccount(context);
    ContentResolver.requestSync(account, BuildConfig.AUTHORITY_DUMMY_CALENDAR, new Bundle());
  }
}
