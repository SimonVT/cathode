package net.simonvt.cathode.settings;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import net.simonvt.cathode.R;
import net.simonvt.cathode.settings.login.LoginActivity;

public class LinkPromptBottomSheet extends BottomSheetDialogFragment {

  public interface LinkPromptDismissListener {

    void onDismissLinkPrompt();
  }

  private LinkPromptDismissListener dismissListener;

  @Override public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setCancelable(false);
    dismissListener = (LinkPromptDismissListener) requireActivity();
  }

  @Override public void onResume() {
    super.onResume();
    if (TraktLinkSettings.isLinkPrompted(requireContext())) {
      dismiss();
      dismissListener.onDismissLinkPrompt();
    }
  }

  @Nullable @Override
  public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState) {
    return inflater.inflate(R.layout.prompt_link, container, false);
  }

  @Override public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    view.findViewById(R.id.prompt_link_no).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Settings.get(requireContext())
            .edit()
            .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
            .apply();
        dismiss();
        dismissListener.onDismissLinkPrompt();
      }
    });
    view.findViewById(R.id.prompt_link_yes).setOnClickListener(new View.OnClickListener() {
      @Override public void onClick(View v) {
        Intent i = new Intent(requireContext(), LoginActivity.class);
        startActivity(i);
      }
    });
  }
}
