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

import android.Manifest;
import android.annotation.TargetApi;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import net.simonvt.cathode.util.VersionCodes;

public final class Permissions {

  private Permissions() {
  }

  public static boolean hasCalendarPermission(Context context) {
    return hasPermissions(context, new String[] {
        Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR
    });
  }

  @TargetApi(Build.VERSION_CODES.M)
  public static boolean hasPermissions(Context context, String[] permissions) {
    if (VersionCodes.isAtLeastM()) {
      for (String permission : permissions) {
        if (context.checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
          return false;
        }
      }
    }

    return true;
  }
}
