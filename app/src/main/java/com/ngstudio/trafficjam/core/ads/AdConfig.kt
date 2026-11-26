package com.ngstudio.trafficjam.core.ads

import com.ngstudio.trafficjam.BuildConfig

/**
 * Single switch for whether rewarded-ad options are offered anywhere in the game.
 *
 * No real ad network is wired up yet, so ads are kept out of release builds completely: every
 * watch-ad button is hidden and each feature falls back to a non-ad flow. The placeholder ad
 * still runs in debug builds, so the reward plumbing can be tested. When a real network (AdMob)
 * is integrated, return true here (or gate on the network being ready) to turn ads back on.
 */
object AdConfig {
    inline val enabled: Boolean get() = BuildConfig.DEBUG
}
