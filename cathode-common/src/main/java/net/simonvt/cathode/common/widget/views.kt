package net.simonvt.cathode.common.widget

import android.app.Activity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment

inline fun <reified T : View> View.find(id: Int): T = findViewById(id)
inline fun <reified T : View> Activity.find(id: Int): T = findViewById(id)
inline fun <reified T : View> Fragment.find(id: Int): T = view?.findViewById(id) as T

inline fun <reified T : View> View.findOrNull(id: Int): T? = findViewById(id)
inline fun <reified T : View> Activity.findOrNull(id: Int): T? = findViewById(id)
inline fun <reified T : View> Fragment.findOrNull(id: Int): T? = view?.findViewById(id)

fun ViewGroup.inflate(layout: Int): View = LayoutInflater.from(context).inflate(layout, this, false)
