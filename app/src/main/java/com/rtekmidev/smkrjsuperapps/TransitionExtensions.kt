package com.rtekmidev.smkrjsuperapps

import android.app.Activity

/**
 * Extension functions to provide smooth, seamless screen transitions across the app.
 * Entering: Incoming fades in smoothly while outgoing holds in place (no black flash).
 * Exiting: Outgoing dissolves smoothly while incoming is already visible.
 */
fun Activity.applyEnterTransition() {
    overridePendingTransition(R.anim.fade_in_smooth, R.anim.stay_smooth)
}

fun Activity.applyExitTransition() {
    overridePendingTransition(R.anim.stay_smooth, R.anim.fade_out_smooth)
}
