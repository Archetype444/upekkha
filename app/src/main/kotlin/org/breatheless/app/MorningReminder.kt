package org.breatheless.app

import android.app.*
import android.content.*
import android.os.Build
import com.wanderwildwood.nikka.MainActivity
import com.wanderwildwood.nikka.R
import java.time.ZonedDateTime

fun nextReminder(now: ZonedDateTime, hour: Int, minute: Int): ZonedDateTime {
    require(hour in 0..23 && minute in 0..59)
    val today = now.toLocalDate().atTime(hour, minute).atZone(now.zone)
    return if (today.isAfter(now)) today else now.toLocalDate().plusDays(1).atTime(hour, minute).atZone(now.zone)
}

class MorningReminder : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = Store.get(context)
        if (intent.action == ACTION && store.bool("morningReminder", false)) {
            val notifications = context.getSystemService(NotificationManager::class.java)
            notifications.createNotificationChannel(NotificationChannel("morning_cp", "Morning CP", NotificationManager.IMPORTANCE_DEFAULT))
            val open = Intent(context, MainActivity::class.java).putExtra("morning_cp", true)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
            val pending = PendingIntent.getActivity(context, 82, open, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            runCatching { notifications.notify(82, Notification.Builder(context, "morning_cp")
                .setSmallIcon(R.drawable.ic_notification).setContentTitle("Morning CP")
                .setContentText("Take your measurement when you are ready.").setContentIntent(pending).setAutoCancel(true).build()) }
        }
        schedule(context)
    }
    companion object {
        const val ACTION = "org.breatheless.app.MORNING_CP"
        fun exactAllowed(context: Context) = Build.VERSION.SDK_INT < 31 || context.getSystemService(AlarmManager::class.java).canScheduleExactAlarms()
        fun schedule(context: Context) {
            val alarms = context.getSystemService(AlarmManager::class.java)
            val pending = PendingIntent.getBroadcast(context, 82, Intent(context, MorningReminder::class.java).setAction(ACTION), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
            alarms.cancel(pending)
            val store = Store.get(context)
            if (!store.bool("morningReminder", false)) return
            val due = nextReminder(ZonedDateTime.now(), store.settings.optInt("reminderHour", 8), store.settings.optInt("reminderMinute", 0)).toInstant().toEpochMilli()
            try {
                if (exactAllowed(context)) alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)
                else alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending)
            } catch (_: SecurityException) { alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, due, pending) }
        }
    }
}
