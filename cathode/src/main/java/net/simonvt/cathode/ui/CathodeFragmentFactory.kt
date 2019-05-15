package net.simonvt.cathode.ui

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import javax.inject.Inject
import javax.inject.Provider

class CathodeFragmentFactory @Inject constructor(
  private val creators: Map<Class<out Fragment>, @JvmSuppressWildcards Provider<Fragment>>
) : FragmentFactory() {

  override fun instantiate(classLoader: ClassLoader, className: String): Fragment {
    val fragmentClass = Class.forName(className)
    val foundEntry = creators.entries.find { fragmentClass.isAssignableFrom(it.key) }
    if (foundEntry != null) {
      return foundEntry.value.get()
    }

    return super.instantiate(classLoader, className)
  }
}
