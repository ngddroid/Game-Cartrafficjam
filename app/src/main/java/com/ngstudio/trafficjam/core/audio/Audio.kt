package com.ngstudio.trafficjam.core.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import android.os.Build
import android.os.SystemClock
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import com.ngstudio.trafficjam.R

enum class Sfx(val res: Int, val volume: Float = 1f) {
    Click(R.raw.ui_click, 0.8f),
    Open(R.raw.ui_open, 0.7f),
    Close(R.raw.ui_close, 0.7f),
    Tap(R.raw.tap, 0.7f),
    Bump(R.raw.bump),
    EngineGo(R.raw.engine_go, 0.75f),
    Park(R.raw.park, 0.7f),
    Pop1(R.raw.board_pop1, 0.55f),
    Pop2(R.raw.board_pop2, 0.55f),
    Pop3(R.raw.board_pop3, 0.55f),
    Full(R.raw.vehicle_full, 0.8f),
    Horn(R.raw.depart_horn, 0.65f),
    Coin(R.raw.coin, 0.8f),
    CoinBurst(R.raw.coin_burst),
    Star1(R.raw.star1),
    Star2(R.raw.star2),
    Star3(R.raw.star3),
    Win(R.raw.win_fanfare),
    Fail(R.raw.fail),
    Unlock(R.raw.unlock),
    Whoosh(R.raw.whoosh, 0.7f),
    Booster(R.raw.booster),
    Crane(R.raw.crane),
    Warning(R.raw.warning, 0.8f),
}

enum class Music(val res: Int) { Home(R.raw.music_home), Game(R.raw.music_game) }

enum class Haptic { Tap, Bump, Success, Fail }

/** Sound effects, background music and vibration behind one switchboard. */
class AudioManager(private val context: Context) {

    var soundOn = true
    var vibrationOn = true
    var musicOn = true
        set(value) {
            field = value
            if (value) resumeMusic() else pauseMusic()
        }

    private val pool = SoundPool.Builder()
        .setMaxStreams(10)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build()
        ).build()

    private val ids = HashMap<Sfx, Int>()
    private val lastPlayed = HashMap<Sfx, Long>()

    /**
     * SoundPool.play can block for tens of milliseconds while it steals a busy stream, which
     * stalled game frames when many trucks were scooping at once. Sounds start on this thread.
     */
    private val sfxHandler = android.os.Handler(
        android.os.HandlerThread("sfx", android.os.Process.THREAD_PRIORITY_AUDIO).apply { start() }.looper,
    )

    init {
        Sfx.entries.forEach { ids[it] = pool.load(context, it.res, 1) }
    }

    /** Plays a sound; repeats of the same sound within 30 ms are dropped to avoid phasing. */
    fun play(sfx: Sfx, rate: Float = 1f, volume: Float = 1f) {
        if (!soundOn) return
        val now = SystemClock.uptimeMillis()
        if (now - (lastPlayed[sfx] ?: 0) < 30) return
        lastPlayed[sfx] = now
        val v = (sfx.volume * volume).coerceIn(0f, 1f)
        val id = ids[sfx] ?: return
        val r = rate.coerceIn(0.5f, 2f)
        sfxHandler.post { pool.play(id, v, v, 1, 0, r) }
    }

    // --- Music ------------------------------------------------------------------------------

    private var player: MediaPlayer? = null
    private var current: Music? = null
    private var inForeground = true
    private var ducked = false

    fun playMusic(music: Music) {
        if (current == music && player != null) {
            resumeMusic()
            return
        }
        player?.release()
        current = music
        player = MediaPlayer.create(context, music.res)?.apply {
            isLooping = true
            setVolume(0.45f, 0.45f)
        }
        resumeMusic()
    }

    fun setForeground(fg: Boolean) {
        inForeground = fg
        if (fg) resumeMusic() else pauseMusic()
    }

    /** Silences music while a (simulated) ad plays. */
    fun duck(on: Boolean) {
        ducked = on
        if (on) pauseMusic() else resumeMusic()
    }

    private fun resumeMusic() {
        val p = player ?: return
        if (musicOn && inForeground && !ducked && !p.isPlaying) p.start()
    }

    private fun pauseMusic() {
        player?.takeIf { it.isPlaying }?.pause()
    }

    // --- Haptics ----------------------------------------------------------------------------

    private val vibrator: Vibrator? =
        if (Build.VERSION.SDK_INT >= 31) {
            (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }

    fun haptic(h: Haptic) {
        if (!vibrationOn) return
        val v = vibrator ?: return
        // A binder call to the vibrator service: keep it off the frame thread like the sounds.
        sfxHandler.post { vibrateNow(v, h) }
    }

    private fun vibrateNow(v: android.os.Vibrator, h: Haptic) {
        if (!v.hasVibrator()) return
        if (Build.VERSION.SDK_INT >= 26) {
            val effect = when (h) {
                Haptic.Tap -> VibrationEffect.createOneShot(12, 90)
                Haptic.Bump -> VibrationEffect.createOneShot(35, 200)
                Haptic.Success -> VibrationEffect.createWaveform(longArrayOf(0, 30, 60, 40), intArrayOf(0, 180, 0, 255), -1)
                Haptic.Fail -> VibrationEffect.createWaveform(longArrayOf(0, 80, 80, 120), intArrayOf(0, 200, 0, 160), -1)
            }
            v.vibrate(effect)
        } else {
            @Suppress("DEPRECATION")
            v.vibrate(if (h == Haptic.Tap) 12 else 40)
        }
    }
}
