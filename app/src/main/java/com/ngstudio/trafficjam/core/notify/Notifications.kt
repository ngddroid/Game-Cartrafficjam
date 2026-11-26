package com.ngstudio.trafficjam.core.notify

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.ngstudio.trafficjam.MainActivity
import com.ngstudio.trafficjam.R
import com.ngstudio.trafficjam.core.analytics.Analytics
import java.util.Calendar

/** Notification channels, the twice-daily reminders and the shared way to post a notification. */
object Notifications {

    const val CHANNEL_REMINDERS = "daily_reminders"
    const val CHANNEL_NEWS = "game_news"

    /** Intent extra on the launch intent, so opens can be counted per notification kind. */
    const val EXTRA_SOURCE = "notification_source"

    private const val ACCENT = 0xFFFF7A3D.toInt()

    fun createChannels(context: Context) {
        if (Build.VERSION.SDK_INT < 26) return
        val nm = context.getSystemService(NotificationManager::class.java) ?: return
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_REMINDERS, "Daily reminders", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Morning and evening reminders to play and collect your daily reward."
            },
        )
        nm.createNotificationChannel(
            NotificationChannel(CHANNEL_NEWS, "Game news", NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "New levels, events and updates from the game."
            },
        )
    }

    fun canPost(context: Context): Boolean =
        Build.VERSION.SDK_INT < 33 ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED

    /** Posts a notification that opens the game when tapped. */
    fun post(context: Context, id: Int, channel: String, title: String, body: String, source: String) {
        if (!canPost(context)) return
        val open = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(EXTRA_SOURCE, source)
        }
        val pi = PendingIntent.getActivity(context, id, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val n = NotificationCompat.Builder(context, channel)
            .setSmallIcon(R.drawable.ic_stat_notify)
            .setColor(ACCENT)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setAutoCancel(true)
            .setContentIntent(pi)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(context).notify(id, n)
        } catch (e: SecurityException) {
            Analytics.nonFatal(e)
        }
    }
}

/**
 * Two local reminders a day, at 9 AM and 7 PM. Each alarm posts its message and then books
 * itself for the next day; reboots, time changes and app updates book both again.
 * Inexact alarms (a 10 minute window) need no special permission and are kind to the battery.
 */
object Reminders {

    enum class Slot(val hour: Int, val requestCode: Int) { MORNING(9, 9001), EVENING(19, 9019) }

    private const val ACTION_REMIND = "com.ngstudio.trafficjam.action.REMIND"
    private const val EXTRA_SLOT = "slot"
    private const val WINDOW_MS = 10 * 60 * 1000L

    fun scheduleAll(context: Context) {
        Slot.entries.forEach { schedule(context, it) }
    }

    fun schedule(context: Context, slot: Slot) {
        val am = context.getSystemService(AlarmManager::class.java) ?: return
        am.setWindow(AlarmManager.RTC_WAKEUP, nextTime(slot.hour), WINDOW_MS, pending(context, slot))
    }

    private fun pending(context: Context, slot: Slot): PendingIntent {
        val i = Intent(context, ReminderReceiver::class.java).setAction(ACTION_REMIND).putExtra(EXTRA_SLOT, slot.name)
        return PendingIntent.getBroadcast(context, slot.requestCode, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
    }

    /** The next [hour]:00 that is still ahead of now. */
    private fun nextTime(hour: Int): Long {
        val c = Calendar.getInstance()
        val now = c.timeInMillis
        c.set(Calendar.HOUR_OF_DAY, hour)
        c.set(Calendar.MINUTE, 0)
        c.set(Calendar.SECOND, 0)
        c.set(Calendar.MILLISECOND, 0)
        if (c.timeInMillis <= now + 60_000L) c.add(Calendar.DAY_OF_YEAR, 1)
        return c.timeInMillis
    }

    /** Debug builds only: posts the reminder for [slotName] now, today's message included. */
    fun debugShow(context: Context, slotName: String) {
        val slot = Slot.entries.firstOrNull { it.name == slotName } ?: return
        val (title, body) = ReminderMessages.pick(slot, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
        Notifications.post(context, slot.requestCode, Notifications.CHANNEL_REMINDERS, title, body, "reminder_${slot.name.lowercase()}")
    }

    internal fun handle(context: Context, intent: Intent) {
        when (intent.action) {
            ACTION_REMIND -> {
                val slot = intent.getStringExtra(EXTRA_SLOT)?.let { n -> Slot.entries.firstOrNull { it.name == n } } ?: return
                // Book tomorrow first, so a failure below can never stop the reminders.
                schedule(context, slot)
                // No point nudging someone who is playing right now.
                val playing = ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)
                if (playing) return
                val (title, body) = ReminderMessages.pick(slot, Calendar.getInstance().get(Calendar.DAY_OF_YEAR))
                Notifications.post(context, slot.requestCode, Notifications.CHANNEL_REMINDERS, title, body, "reminder_${slot.name.lowercase()}")
            }
            else -> scheduleAll(context) // boot, time or time zone change, app update
        }
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) = Reminders.handle(context, intent)
}

/** Different words every day: the morning and evening lists rotate by day of the year. */
internal object ReminderMessages {

    private val morning = listOf(
        "🚦 Rush hour is here!" to "The lot is jammed again. Clear the cars before your coffee gets cold!",
        "🎁 Your daily reward is ready" to "Grab today's free coins and keep your streak going.",
        "🚗 Good morning, driver!" to "A fresh puzzle is waiting on your next level. Can you solve it first try?",
        "🚚 Sand Sweep is calling" to "Free the trucks and scoop the whole sand picture.",
        "🧠 Quick brain warm-up" to "Untangle one traffic jam to start your day sharp.",
        "🚌 Passengers are waiting" to "The Crowd Loop buses won't fill themselves. Park them up!",
        "⭐ Stars up for grabs" to "Clear today's level with 3 stars and climb the map.",
        "🎨 A new picture to reveal" to "Peel the Pixel Crowd layer by layer and see what's hiding.",
        "☕ Coffee + one level" to "The perfect five-minute puzzle to wake your brain up.",
        "🛣️ Fresh roads today" to "Your next level is ready and the cars are honking. Let's go!",
    )

    private val evening = listOf(
        "🌆 Unwind with a quick jam" to "A few traffic puzzles make the perfect evening break.",
        "🔥 Keep your streak alive" to "Play one level tonight and keep your progress rolling.",
        "🚧 Gridlock alert!" to "The cars are stuck again. Only you can get them moving.",
        "🏁 One more level?" to "Every level you clear brings the next world closer.",
        "🚚 The trucks are idling" to "The Sand Sweep belt is ready to roll. Fill them up!",
        "💪 Evening challenge" to "Hard levels pay out more coins. Are you up for one?",
        "🎨 Fresh paint for your ride" to "Visit the Garage and pick a new look for your cars.",
        "😌 Relax and park" to "Slide, tap and clear the lot. A calm puzzle to end the day.",
        "🚍 The Crowd Loop is packed" to "Send the buses round and get everyone home.",
        "🌙 Before you sleep…" to "One quick jam to clear, then sweet dreams, driver!",
    )

    fun pick(slot: Reminders.Slot, dayOfYear: Int): Pair<String, String> {
        val list = if (slot == Reminders.Slot.MORNING) morning else evening
        return list[Math.floorMod(dayOfYear, list.size)]
    }
}
