package com.ngstudio.trafficjam

import com.ngstudio.trafficjam.core.analytics.Analytics
import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.ngstudio.trafficjam.core.ads.FakeAdManager
import com.ngstudio.trafficjam.core.audio.AudioManager
import com.ngstudio.trafficjam.core.data.ProgressRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/** Process-wide singletons. Small enough that a DI framework would be overkill. */
class AppContainer(app: Application) {
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    val progress = ProgressRepository(app, scope)
    val audio = AudioManager(app)
    val ads = FakeAdManager()
}

class App : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        instance = this
        Analytics.init(this)
        com.ngstudio.trafficjam.core.notify.Notifications.createChannels(this)
        // Re-booking on every start keeps the 9 AM / 7 PM reminders alive (no settings toggle by design).
        com.ngstudio.trafficjam.core.notify.Reminders.scheduleAll(this)
        com.ngstudio.trafficjam.core.notify.PushService.subscribe()
        container = AppContainer(this)
        val c = container
        c.scope.launch {
            c.progress.load()
            c.progress.state.collectLatest { s ->
                if (s.loaded) Analytics.progress(s.nextLevel, s.modeLevels)
                c.audio.soundOn = s.sound
                c.audio.vibrationOn = s.vibration
                c.audio.musicOn = s.music
            }
        }
        ProcessLifecycleOwner.get().lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onStart(owner: LifecycleOwner) = c.audio.setForeground(true)
            override fun onStop(owner: LifecycleOwner) = c.audio.setForeground(false)
        })
    }

    companion object {
        lateinit var instance: App
            private set
        val container get() = instance.container
    }
}
