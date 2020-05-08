package net.simonvt.cathode.settings

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import net.simonvt.cathode.databinding.PromptLinkBinding
import net.simonvt.cathode.settings.login.LoginActivity

class LinkPromptBottomSheet : BottomSheetDialogFragment() {

  interface LinkPromptDismissListener {
    fun onDismissLinkPrompt()
  }

  private lateinit var dismissListener: LinkPromptDismissListener

  private var _binding: PromptLinkBinding? = null
  private val binding get() = _binding!!

  override fun onCreate(inState: Bundle?) {
    super.onCreate(inState)
    isCancelable = false
    dismissListener = requireActivity() as LinkPromptDismissListener
  }

  override fun onResume() {
    super.onResume()
    if (TraktLinkSettings.isLinkPrompted(requireContext())) {
      dismiss()
      dismissListener.onDismissLinkPrompt()
    }
  }

  override fun onCreateView(
    inflater: LayoutInflater,
    container: ViewGroup?,
    inState: Bundle?
  ): View? {
    _binding = PromptLinkBinding.inflate(inflater, container, false)
    return binding.root
  }

  override fun onViewCreated(view: View, inState: Bundle?) {
    super.onViewCreated(view, inState)
    binding.promptLinkNo.setOnClickListener {
      Settings.get(requireContext())
        .edit()
        .putBoolean(TraktLinkSettings.TRAKT_LINK_PROMPTED, true)
        .apply()
      dismiss()
      dismissListener.onDismissLinkPrompt()
    }
    binding.promptLinkYes
      .setOnClickListener {
        val i = Intent(requireContext(), LoginActivity::class.java)
        startActivity(i)
      }
  }
}
