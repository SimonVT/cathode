package net.simonvt.cathode.common.ui;

import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

public final class FragmentsUtils {

  private FragmentsUtils() {
  }

  public static <T extends Fragment> T instantiate(@NonNull FragmentManager fragmentManager,
      @NonNull Class<T> fragmentClass) {
    return FragmentsKt.instantiate(fragmentManager, fragmentClass, null);
  }

  public static <T extends Fragment> T instantiate(@NonNull FragmentManager fragmentManager,
      @NonNull Class<T> fragmentClass, @Nullable Bundle args) {
    return FragmentsKt.instantiate(fragmentManager, fragmentClass, args);
  }
}
