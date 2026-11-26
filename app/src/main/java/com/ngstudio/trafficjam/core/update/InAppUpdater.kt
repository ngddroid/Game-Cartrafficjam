package com.ngstudio.trafficjam.core.update

import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.ngstudio.trafficjam.core.analytics.Analytics

/**
 * Google Play in-app updates. A new version downloads in the background while the player keeps
 * playing (flexible flow); once it is ready the game shows a "Restart" prompt. Releases marked
 * high priority (4 or 5) in the Play Console use the full-screen immediate flow instead.
 * Builds not installed from Google Play simply never see an update.
 *
 * Create it in Activity.onCreate (it registers an activity result launcher), call [check] once
 * per launch and [onResume] from onResume.
 */
class InAppUpdater(activity: ComponentActivity) {

    private val manager = AppUpdateManagerFactory.create(activity)
    private val launcher = activity.registerForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { r ->
        Analytics.updateFlowResult(r.resultCode == android.app.Activity.RESULT_OK)
    }

    /** True once a flexible update has downloaded and is waiting for a restart. */
    var readyToInstall by mutableStateOf(false)
        private set

    private val listener = InstallStateUpdatedListener { state ->
        if (state.installStatus() == InstallStatus.DOWNLOADED) readyToInstall = true
    }

    init {
        manager.registerListener(listener)
    }

    fun check() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            if (info.updateAvailability() != UpdateAvailability.UPDATE_AVAILABLE) return@addOnSuccessListener
            val type = when {
                info.updatePriority() >= 4 && info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                info.isUpdateTypeAllowed(AppUpdateType.FLEXIBLE) -> AppUpdateType.FLEXIBLE
                info.isUpdateTypeAllowed(AppUpdateType.IMMEDIATE) -> AppUpdateType.IMMEDIATE
                else -> return@addOnSuccessListener
            }
            Analytics.updateOffered(info.availableVersionCode(), if (type == AppUpdateType.IMMEDIATE) "immediate" else "flexible")
            start(info, type)
        }
    }

    fun onResume() {
        manager.appUpdateInfo.addOnSuccessListener { info ->
            when {
                info.installStatus() == InstallStatus.DOWNLOADED -> readyToInstall = true
                // An immediate update the player left half way must be resumed.
                info.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> start(info, AppUpdateType.IMMEDIATE)
            }
        }
    }

    /** Installs the downloaded update; the game restarts into the new version. */
    fun install() {
        readyToInstall = false
        manager.completeUpdate()
    }

    fun dispose() = manager.unregisterListener(listener)

    private fun start(info: AppUpdateInfo, type: Int) {
        runCatching {
            manager.startUpdateFlowForResult(info, launcher, AppUpdateOptions.newBuilder(type).build())
        }.onFailure { Analytics.nonFatal(it) }
    }
}
