package com.puzzel.lab.cartrafficjam.core.ads

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/**
 * Rewarded-ad seam. The UI only ever calls [showRewarded]; swapping [FakeAdManager] for an
 * AdMob-backed implementation later needs no screen changes.
 */
interface AdManager {
    /** Shows a rewarded ad for [placement]; [onReward] runs only if the ad was watched. */
    fun showRewarded(placement: String, onReward: () -> Unit, onClosed: () -> Unit = {})
}

/** Stand-in that shows a short full-screen countdown (rendered by AdOverlay) and then rewards. */
class FakeAdManager : AdManager {

    class Request(val placement: String, val onReward: () -> Unit, val onClosed: () -> Unit)

    var active by mutableStateOf<Request?>(null)
        private set

    override fun showRewarded(placement: String, onReward: () -> Unit, onClosed: () -> Unit) {
        if (active != null) return
        active = Request(placement, onReward, onClosed)
    }

    fun finish(rewarded: Boolean) {
        val r = active ?: return
        active = null
        if (rewarded) r.onReward()
        r.onClosed()
    }
}
