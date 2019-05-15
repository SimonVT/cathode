package net.simonvt.cathode.ui

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import dagger.android.AndroidInjection
import net.simonvt.cathode.settings.DarkModePreference
import net.simonvt.cathode.settings.DarkModePreference.DarkMode.ALWAYS
import javax.inject.Inject

abstract class CathodeActivity : AppCompatActivity() {

  @Inject
  lateinit var darkModePreference: DarkModePreference

  @Inject
  lateinit var fragmentFactory: CathodeFragmentFactory

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    AndroidInjection.inject(this)
    updateNightMode()
    supportFragmentManager.fragmentFactory = fragmentFactory
  }

  override fun onStart() {
    super.onStart()
    updateNightMode()
  }

  private fun updateNightMode() {
    when (darkModePreference.value) {
      ALWAYS -> delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_YES)
      else -> delegate.setLocalNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }
  }
}
