package net.simonvt.cathode.ui

import androidx.lifecycle.ViewModel

interface ViewModelFactory {
  fun create(): ViewModel
}
