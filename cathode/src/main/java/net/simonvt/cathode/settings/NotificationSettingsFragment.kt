package net.simonvt.cathode.settings

import android.os.Bundle
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import net.simonvt.cathode.R
import net.simonvt.cathode.common.ui.FragmentsUtils
import net.simonvt.cathode.notification.NotificationService
import net.simonvt.cathode.settings.NotificationTime.HOURS_1
import net.simonvt.cathode.settings.NotificationTimeDialog.NotificationTimeSelectedListener

class NotificationSettingsFragment : PreferenceFragmentCompat(),
  NotificationTimeSelectedListener {

  override fun onCreatePreferences(inState: Bundle?, rootKey: String?) {
    addPreferencesFromResource(R.xml.settings_notifications)

    findPreference<Preference>(Settings.NOTIFICACTIONS_ENABLED)!!.onPreferenceChangeListener =
      Preference.OnPreferenceChangeListener { preference, newValue ->
        NotificationService.start(requireContext())
        true
      }

    findPreference<Preference>(Settings.NOTIFICACTION_TIME)!!.onPreferenceClickListener =
      Preference.OnPreferenceClickListener {
        val notificationTime = NotificationTime.fromValue(
          Settings.get(requireContext()).getLong(
            Settings.NOTIFICACTION_TIME,
            HOURS_1.notificationTime
          )
        )
        val dialog = FragmentsUtils.instantiate(
          requireFragmentManager(),
          NotificationTimeDialog::class.java,
          NotificationTimeDialog.getArgs(notificationTime)
        )
        dialog.setTargetFragment(this@NotificationSettingsFragment, 0)
        dialog.show(requireFragmentManager(), DIALOG_NOTIFICATION_TIME)
        true
      }
  }

  override fun onNotificationTimeSelected(value: NotificationTime) {
    Settings.get(requireContext())
      .edit()
      .putLong(Settings.NOTIFICACTION_TIME, value.notificationTime)
      .apply()
    NotificationService.start(requireContext())
  }

  companion object {

    private const val DIALOG_NOTIFICATION_TIME =
      "net.simonvt.cathode.settings.NotificationSettingsFragment.notificationTIme"
  }
}
