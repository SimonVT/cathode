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

package net.simonvt.cathode.api.util

import android.content.Context
import android.os.StatFs
import java.io.File

object OkHttpUtils {

  private const val CACHE_DIR = "trakt-cache"

  private const val CACHE_SIZE_MIN = 5 * 1024 * 1024
  private const val CACHE_SIZE_MAX = 30 * 1024 * 1024

  fun getCacheDir(context: Context): File {
    val cache = File(context.applicationContext.cacheDir, CACHE_DIR)
    if (!cache.exists()) {
      cache.mkdirs()
    }
    return cache
  }

  fun getCacheSize(dir: File): Long {
    var size = CACHE_SIZE_MIN.toLong()

    try {
      val statFs = StatFs(dir.absolutePath)
      val available: Long
      available = statFs.blockCountLong * statFs.blockSizeLong

      size = available / 50
    } catch (e: IllegalArgumentException) {
      // Ignore
    }

    return Math.max(Math.min(size, CACHE_SIZE_MAX.toLong()), CACHE_SIZE_MIN.toLong())
  }
}
