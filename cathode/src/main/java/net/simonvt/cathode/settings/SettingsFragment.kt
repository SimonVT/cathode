package net.simonvt.cathode.settings

import android.Manifest.permission
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build.VERSION_CODES
import android.os.Bundle
import androidx.annotation.RequiresApi
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreference
import net.simonvt.android.colorpicker.ColorPickerDialog
import net.simonvt.android.colorpicker.ColorPickerSwatch.OnColorSelectedListener
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.common.ui.adapter.Adapters
import net.simonvt.cathode.common.util.Intents
import net.simonvt.cathode.common.util.VersionCodes
import net.simonvt.cathode.provider.DatabaseContract.EpisodeColumns
import net.simonvt.cathode.provider.ProviderSchematic.Episodes
import net.simonvt.cathode.settings.ShowOffsetDialog.ShowOffsetSelectedListener
import net.simonvt.cathode.settings.UpcomingTime.WEEKS_1
import net.simonvt.cathode.settings.UpcomingTimeDialog.UpcomingTimeSelectedListener
import net.simonvt.cathode.settings.hidden.HiddenItems
import net.simonvt.cathode.settings.login.LoginActivity
import timber.log.Timber
import javax.inject.Inject

class SettingsFragment @Inject constructor(private val upcomingTimePreference: UpcomingTimePreference) :
  PreferenceFragmentCompat(), UpcomingTimeSelectedListener, OnColorSelectedListener,
  ShowOffsetSelectedListener {

  private lateinit var syncCalendar: SwitchPreference

  private var isTablet: Boolean = false

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    isTablet = resources.getBoolean(R.bool.isTablet)
  }

  override fun onCreatePreferences(inState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.settings_general)

    syncCalendar = findPreference<Preference>(Settings.CALENDAR_SYNC) as SwitchPreference
    syncCalendar.onPreferenceChangeListener =
      Preference.OnPreferenceChangeListener { preference, newValue ->
        val checked = newValue as Boolean
        syncCalendar.isChecked = checked

        if (VersionCodes.isAtLeastM()) {
          if (checked && !Permissions.hasCalendarPermission(activity)) {
            requestPermission()
          }
        } else {
          Accounts.requestCalendarSync(activity)
        }

        true
      }

    findPreference<Preference>("calendarColor")!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        val calendarColor =
          Settings.get(activity).getInt(Settings.CALENDAR_COLOR, Settings.CALENDAR_COLOR_DEFAULT)
        val size = if (isTablet) ColorPickerDialog.SIZE_LARGE else ColorPickerDialog.SIZE_SMALL
        val dialog = ColorPickerDialog.newInstance(
          R.string.preference_calendar_color,
          Settings.CALENDAR_COLORS,
          calendarColor,
          5,
          size
        )
        dialog.setTargetFragment(this@SettingsFragment, 0)
        dialog.show(requireFragmentManager(), DIALOG_COLOR_PICKER)

        if (Settings.get(activity).getBoolean(Settings.CALENDAR_SYNC, false)) {
          Accounts.requestCalendarSync(activity)
        }

        true
      }

    findPreference<Preference>("notifications")!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        startActivity(
          Intent(
            activity,
            NotificationSettingsActivity::class.java
          )
        )
        true
      }

    findPreference<Preference>("hiddenItems")!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        startActivity(Intent(activity, HiddenItems::class.java))
        true
      }

    findPreference<Preference>("upcomingTime")!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        val upcomingTime = UpcomingTime.fromValue(
          Settings.get(activity).getLong(
            Settings.UPCOMING_TIME,
            WEEKS_1.cacheTime
          )
        )
        val dialog = FragmentsUtils.instantiate(
          requireFragmentManager(),
          UpcomingTimeDialog::class.java,
          UpcomingTimeDialog.getArgs(upcomingTime)
        )
        dialog.setTargetFragment(this@SettingsFragment, 0)
        dialog.show(requireFragmentManager(), DIALOG_UPCOMING_TIME)
        true
      }

    findPreference<Preference>(Settings.SHOWS_AVOID_SPOILERS)!!.onPreferenceChangeListener =
      Preference.OnPreferenceChangeListener { preference, newValue ->
        Adapters.notifyAdapters()

        if (Settings.get(activity).getBoolean(Settings.CALENDAR_SYNC, false)) {
          Accounts.requestCalendarSync(activity)
        }
        true
      }

    findPreference<Preference>(Settings.SHOWS_OFFSET)!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        val offset = FirstAiredOffsetPreference.getInstance().offsetHours
        val dialog = FragmentsUtils.instantiate(
          requireFragmentManager(),
          ShowOffsetDialog::class.java,
          ShowOffsetDialog.getArgs(offset)
        )
        dialog.setTargetFragment(this@SettingsFragment, 0)
        dialog.show(requireFragmentManager(), DIALOG_SHOW_OFFSET)
        true
      }

    val traktLink = findPreference<Preference>("traktLink")
    val traktUnlink = findPreference<Preference>("traktUnlink")

    traktLink!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      val i = Intent(activity, LoginActivity::class.java)
      i.putExtra(LoginActivity.EXTRA_TASK, LoginActivity.TASK_LINK)
      startActivity(i)
      true
    }

    traktUnlink!!.onPreferenceClickListener = Preference.OnPreferenceClickListener {
      FragmentsUtils.instantiate(requireFragmentManager(), LogoutDialog::class.java)
        .show(requireFragmentManager(), DIALOG_LOGOUT)
      true
    }

    if (TraktLinkSettings.isLinked(activity)) {
      preferenceScreen.removePreference(traktLink)
    } else {
      preferenceScreen.removePreference(traktUnlink)
    }

    findPreference<Preference>("about")!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        AboutDialog().show(requireFragmentManager(), DIALOG_ABOUT)
        true
      }

    findPreference<Preference>("privacy")!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        Intents.openUrl(requireActivity(), getString(R.string.privacy_policy_url))
        true
      }
  }

  @RequiresApi(VERSION_CODES.M)
  private fun requestPermission() {
    requestPermissions(
      arrayOf(permission.READ_CALENDAR, permission.WRITE_CALENDAR),
      PERMISSION_REQUEST_CALENDAR
    )
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == PERMISSION_REQUEST_CALENDAR) {
      if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Timber.d("Calendar permission granted")
        Accounts.requestCalendarSync(activity)
      } else {
        Timber.d("Calendar permission not granted")
      }
    }
  }

  override fun onUpcomingTimeSelected(value: UpcomingTime) {
    upcomingTimePreference.set(value)
  }

  override fun onColorSelected(color: Int) {
    val calendarColor =
      Settings.get(activity).getInt(Settings.CALENDAR_COLOR, Settings.CALENDAR_COLOR_DEFAULT)
    if (color != calendarColor) {
      Settings.get(activity)
        .edit()
        .putInt(Settings.CALENDAR_COLOR, color)
        .putBoolean(Settings.CALENDAR_COLOR_NEEDS_UPDATE, true)
        .apply()

      if (Permissions.hasCalendarPermission(activity)) {
        Accounts.requestCalendarSync(activity)
      }
    }
  }

  override fun onShowOffsetSelected(offset: Int) {
    val showOffset = FirstAiredOffsetPreference.getInstance().offsetHours
    if (offset != showOffset) {
      FirstAiredOffsetPreference.getInstance().set(offset)
      val context = requireContext()

      Thread(Runnable {
        val values = ContentValues()
        values.put(EpisodeColumns.LAST_MODIFIED, System.currentTimeMillis())
        context.contentResolver.update(Episodes.EPISODES, values, null, null)

        Accounts.requestCalendarSync(context)
      }).start()
    }
  }

  companion object {

    private const val DIALOG_UPCOMING_TIME =
      "net.simonvt.cathode.settings.SettingsFragment.upcomingTime"
    private const val DIALOG_SHOW_OFFSET =
      "net.simonvt.cathode.settings.SettingsFragment.showOffset"
    private const val DIALOG_COLOR_PICKER =
      "net.simonvt.cathode.settings.SettingsFragment.ColorPicker"
    private const val DIALOG_LOGOUT = "net.simonvt.cathode.settings.SettingsFragment.logoutDialog"
    private const val DIALOG_ABOUT = "net.simonvt.cathode.settings.SettingsFragment.aboutDialog"

    private const val PERMISSION_REQUEST_CALENDAR = 11
  }
}
