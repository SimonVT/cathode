package net.simonvt.cathode.settings

import android.Manifest.permission
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.simonvt.cathode.databinding.PromptSetupBinding
import timber.log.Timber

class SetupPromptBottomSheet : BottomSheetDialogFragment() {

  private var _binding: PromptSetupBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    isCancelable = false
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = PromptSetupBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    binding.promptSetupCalendarSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
      Timber.d("Calendar toggled: %b", isChecked)
      if (isChecked) {
        if (!Permissions.hasCalendarPermission(requireContext())) {
          requestPermissions(
            arrayOf(
              permission.READ_CALENDAR,
              permission.WRITE_CALENDAR
            ), PERMISSION_REQUEST_CALENDAR
          )
        } else {
          syncCalendar()
        }
      } else {
        dontSyncCalendar()
      }
    }
    binding.promptSetupNotificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
      Timber.d("Notification toggled: %b", isChecked)
      Settings.get(requireContext()).edit()
        .putBoolean(Settings.CALENDAR_SYNC, isChecked).apply()
    }
    binding.promptSetupNotificationSwitch.setOnCheckedChangeListener { buttonView, isChecked ->
      Timber.d("Toggling notifications: %b", isChecked)
      Settings.get(requireContext())
        .edit()
        .putBoolean(Settings.NOTIFICACTIONS_ENABLED, isChecked)
        .apply()
    }
    binding.promptSetupDone.setOnClickListener {
      Settings.get(requireContext()).edit()
        .putBoolean(Settings.SETUP_PROMPTED, true).apply()
      dismiss()
    }
  }

  override fun onDestroyView() {
    _binding = null
    super.onDestroyView()
  }

  fun syncCalendar() {
    Timber.d("syncCalendar")
    Settings.get(requireContext()).edit()
      .putBoolean(Settings.CALENDAR_SYNC, true).apply()
  }

  fun dontSyncCalendar() {
    Timber.d("dontSyncCalendar")
    Settings.get(requireContext()).edit()
      .putBoolean(Settings.CALENDAR_SYNC, false).apply()
    if (_binding != null && binding.promptSetupCalendarSwitch.isChecked) {
      binding.promptSetupCalendarSwitch.isChecked = false
    }
  }

  override fun onRequestPermissionsResult(
    requestCode: Int,
    permissions: Array<String>,
    grantResults: IntArray
  ) {
    if (requestCode == PERMISSION_REQUEST_CALENDAR) {
      if (grantResults.size > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Timber.d("Calendar permission granted")
        syncCalendar()
      } else {
        Timber.d("Calendar permission not granted")
        dontSyncCalendar()
      }
    }
  }

  companion object {
    private const val PERMISSION_REQUEST_CALENDAR = 11
  }
}
