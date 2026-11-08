package org.breatheless.app

import android.content.Context
import android.media.AudioAttributes

object SoundRoute {
    val names = listOf("Alarm", "Media", "Notification", "Ringtone", "System")
    fun usage(channel: String) = when (channel) {
        "Media" -> AudioAttributes.USAGE_MEDIA
        "Notification" -> AudioAttributes.USAGE_NOTIFICATION
        "Ringtone" -> AudioAttributes.USAGE_NOTIFICATION_RINGTONE
        "System" -> AudioAttributes.USAGE_ASSISTANCE_SONIFICATION
        else -> AudioAttributes.USAGE_ALARM
    }
    fun attributes(context: Context): AudioAttributes = AudioAttributes.Builder()
        .setUsage(usage(Store.get(context).str("fxChannel", "Alarm")))
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()
}
