package net.simonvt.cathode.common.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager

@Suppress("UNCHECKED_CAST")
fun <T : Fragment> FragmentManager.instantiate(fragmentClass: Class<T>, args: Bundle? = null): T {
  val fragment = fragmentFactory.instantiate(javaClass.classLoader!!, fragmentClass.name)
  fragment.arguments = args
  return fragment as T
}
