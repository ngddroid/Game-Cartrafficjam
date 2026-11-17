package com.puzzel.lab.cartrafficjam

import android.app.Application
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.puzzel.lab.cartrafficjam.core.ads.FakeAdManager
import com.puzzel.lab.cartrafficjam.core.audio.AudioManager
import com.puzzel.lab.cartrafficjam.core.data.ProgressRepository
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
        container = AppContainer(this)
        val c = container
        c.scope.launch {
            c.progress.load()
            c.progress.state.collectLatest { s ->
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
