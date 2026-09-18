package com.example.myno.jz.ui.common

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.example.myno.jz.R

/**
 * 统一应用内 Fragment 导航，避免各页面重复编写 transaction 模板。
 */
fun FragmentManager.openScreen(fragment: Fragment, addToBackStack: Boolean = true) {
    beginTransaction()
        .replace(R.id.fragmentContainer, fragment)
        .apply {
            if (addToBackStack) addToBackStack(null)
        }
        .commit()
}
