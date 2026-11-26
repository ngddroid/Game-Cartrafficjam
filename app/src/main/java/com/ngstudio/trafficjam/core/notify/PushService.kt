package com.ngstudio.trafficjam.core.notify

import android.util.Log
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.ngstudio.trafficjam.BuildConfig

/**
 * Firebase Cloud Messaging. When the game is in the background, Firebase shows "notification"
 * messages itself (with the icon and channel set in the manifest). This service covers the rest:
 * notification messages that arrive while the game is open, and data-only messages that carry
 * "title" and "body" keys.
 */
class PushService : FirebaseMessagingService() {

    override fun onMessageReceived(message: RemoteMessage) {
        val n = message.notification
        val title = n?.title ?: message.data["title"] ?: return
        val body = n?.body ?: message.data["body"] ?: return
        val channel = n?.channelId ?: message.data["channel"] ?: Notifications.CHANNEL_NEWS
        Notifications.post(this, (message.messageId ?: title).hashCode(), channel, title, body, "push")
    }

    override fun onNewToken(token: String) {
        // The token is only needed to send a test push to one device from the Firebase console.
        if (BuildConfig.DEBUG) Log.d(TAG, "FCM token: $token")
    }

    companion object {
        private const val TAG = "Push"

        /** Every install listens on the "all" topic, so a console campaign can reach everyone. */
        const val TOPIC_ALL = "all"

        fun subscribe() {
            runCatching {
                val fm = FirebaseMessaging.getInstance()
                fm.subscribeToTopic(TOPIC_ALL)
                if (BuildConfig.DEBUG) fm.token.addOnSuccessListener { Log.d(TAG, "FCM token: $it") }
            }
        }
    }
}
