/*
 * Copyright (C) 2016 Simon Vig Therkildsen
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

package net.simonvt.cathode.api.util;

import android.content.Context;
import android.os.Build;
import android.os.StatFs;
import java.io.File;

public class OkHttpUtils {

  private static final String CACHE_DIR = "trakt-cache";

  private static final int CACHE_SIZE_MIN = 5 * 1024 * 1024;
  private static final int CACHE_SIZE_MAX = 30 * 1024 * 1024;

  private OkHttpUtils() {
  }

  public static File getCacheDir(Context context) {
    File cache = new File(context.getApplicationContext().getCacheDir(), CACHE_DIR);
    if (!cache.exists()) {
      cache.mkdirs();
    }
    return cache;
  }

  public static long getCacheSize(File dir) {
    long size = CACHE_SIZE_MIN;

    try {
      StatFs statFs = new StatFs(dir.getAbsolutePath());
      long available;
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
        available = statFs.getBlockCountLong() * statFs.getBlockSizeLong();
      } else {
        available = ((long) statFs.getBlockCount()) * statFs.getBlockSize();
      }

      size = available / 50;
    } catch (IllegalArgumentException e) {
    }

    return Math.max(Math.min(size, CACHE_SIZE_MAX), CACHE_SIZE_MIN);
  }
}
