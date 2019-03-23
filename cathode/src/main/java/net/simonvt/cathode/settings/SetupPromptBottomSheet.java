package net.simonvt.cathode.settings;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.SwitchCompat;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.Unbinder;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import net.simonvt.cathode.R;
import timber.log.Timber;

public class SetupPromptBottomSheet extends BottomSheetDialogFragment {

  private static final int PERMISSION_REQUEST_CALENDAR = 11;

  Unbinder unbinder;

  @BindView(R.id.prompt_setup_calendar_switch) SwitchCompat calendarSwitch;
  @BindView(R.id.prompt_setup_notification_switch) SwitchCompat notificationSwitch;

  @Override public void onCreate(@Nullable Bundle inState) {
    super.onCreate(inState);
    setCancelable(false);
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle inState) {
    return inflater.inflate(R.layout.prompt_setup, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle inState) {
    super.onViewCreated(view, inState);
    unbinder = ButterKnife.bind(this, view);
    calendarSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Timber.d("Calendar toggled: %b", isChecked);
        if (isChecked) {
          if (!Permissions.hasCalendarPermission(requireContext())) {
            requestPermissions(new String[] {
                Manifest.permission.READ_CALENDAR, Manifest.permission.WRITE_CALENDAR,
            }, PERMISSION_REQUEST_CALENDAR);
          } else {
            syncCalendar();
          }
        } else {
          dontSyncCalendar();
        }
      }
    });
    notificationSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
      @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Timber.d("Notification toggled: %b", isChecked);
        Settings.get(requireContext()).edit().putBoolean(Settings.CALENDAR_SYNC, isChecked).apply();
      }
    });
    ((SwitchCompat) view.findViewById(
        R.id.prompt_setup_notification_switch)).setOnCheckedChangeListener(
        new CompoundButton.OnCheckedChangeListener() {
          @Override public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            Timber.d("Toggling notifications: %b", isChecked);
            Settings.get(requireContext())
                .edit()
                .putBoolean(Settings.NOTIFICACTIONS_ENABLED, isChecked)
                .apply();
          }
        });
    view.findViewById(R.id.prompt_setup_done).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Settings.get(requireContext()).edit().putBoolean(Settings.SETUP_PROMPTED, true).apply();
        dismiss();
      }
    });
  }

  @Override public void onDestroyView() {
    super.onDestroyView();
    unbinder.unbind();
    unbinder = null;
  }

  void syncCalendar() {
    Timber.d("syncCalendar");
    Settings.get(requireContext()).edit().putBoolean(Settings.CALENDAR_SYNC, true).apply();
  }

  void dontSyncCalendar() {
    Timber.d("dontSyncCalendar");
    Settings.get(requireContext()).edit().putBoolean(Settings.CALENDAR_SYNC, false).apply();
    if (calendarSwitch != null && calendarSwitch.isChecked()) {
      calendarSwitch.setChecked(false);
    }
  }

  @Override public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
      @NonNull int[] grantResults) {
    if (requestCode == PERMISSION_REQUEST_CALENDAR) {
      if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
        Timber.d("Calendar permission granted");
        syncCalendar();
      } else {
        Timber.d("Calendar permission not granted");
        dontSyncCalendar();
      }
    }
  }
}
