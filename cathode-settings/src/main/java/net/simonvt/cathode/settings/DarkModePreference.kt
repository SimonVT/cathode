package net.simonvt.cathode.settings

import android.content.Context
import net.simonvt.cathode.settings.DarkModePreference.DarkMode.SYSTEM
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DarkModePreference @Inject constructor(val context: Context) {

  enum class DarkMode(val value: String) {
    SYSTEM("system"),
    ALWAYS("always")
  }

  val value: DarkMode get() = darkMode(Settings.get(context).getString(DARK_MODE, null))

  private fun darkMode(value: String?): DarkMode {
    return DarkMode.values().firstOrNull { value == it.value } ?: SYSTEM
  }

  companion object {
    const val DARK_MODE = "darkMode"
  }
}
