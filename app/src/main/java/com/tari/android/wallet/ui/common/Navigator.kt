package com.tari.android.wallet.ui.common

import android.os.Bundle
import androidx.annotation.IdRes
import androidx.navigation.NavController
import androidx.navigation.NavDirections
import androidx.navigation.NavOptions
import com.tari.android.wallet.R

class Navigator(private val navController: NavController) {

    fun navigate(@IdRes id: Int, bundle: Bundle? = null) = navController.navigate(id, bundle, getNavOptions())

    fun navigate(action: NavDirections) = navController.navigate(action, getNavOptions())

    private fun getNavOptions(): NavOptions = NavOptions.Builder()
        .setEnterAnim(R.anim.enter_from_right)
        .setExitAnim(R.anim.exit_to_left)
        .setPopEnterAnim(R.anim.enter_from_left)
        .setPopExitAnim(R.anim.exit_to_right)
        .build()
}