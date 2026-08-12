package com.milad.pricealarm.notification

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.RingtoneManager
import androidx.core.app.NotificationCompat
import com.milad.pricealarm.MainActivity
import com.milad.pricealarm.data.AlertCondition

object NotificationHelper {
    const val SERVICE_CHANNEL_ID = "monitor_service_channel"
    const val ALERT_CHANNEL_ID = "price_alert_channel"
    const val SERVICE_NOTIFICATION_ID = 1001

    fun createChannels(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val serviceChannel = NotificationChannel(
            SERVICE_CHANNEL_ID,
            "مانیتورینگ پس‌زمینه",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "نمایش وضعیت بررسی قیمت در پس‌زمینه"
            setShowBadge(false)
        }

        val alertSoundUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)

        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_ALARM)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        val alertChannel = NotificationChannel(
            ALERT_CHANNEL_ID,
            "آلارم قیمت",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "اعلان زمانی که قیمت به هدف تعیین‌شده می‌رسد"
            enableVibration(true)
            vibrationPattern = longArrayOf(0, 500, 250, 500, 250, 500)
            setSound(alertSoundUri, audioAttributes)
        }

        manager.createNotificationChannel(serviceChannel)
        manager.createNotificationChannel(alertChannel)
    }

    fun buildServiceNotification(context: Context, statusText: String): Notification {
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, 0, openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        return NotificationCompat.Builder(context, SERVICE_CHANNEL_ID)
            .setContentTitle("دیده‌بان قیمت فعال است")
            .setContentText(statusText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    fun showAlertNotification(
        context: Context,
        alertId: Long,
        symbol: String,
        condition: AlertCondition,
        targetPrice: Double,
        currentPrice: Double
    ) {
        val directionText = if (condition == AlertCondition.ABOVE) "بالاتر رفت از" else "پایین‌تر آمد از"
        val openAppIntent = Intent(context, MainActivity::class.java)
        val pendingIntent = PendingIntent.getActivity(
            context, alertId.toInt(), openAppIntent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, ALERT_CHANNEL_ID)
            .setContentTitle("🔔 $symbol $directionText ${formatPrice(targetPrice)}")
            .setContentText("قیمت فعلی: ${formatPrice(currentPrice)}")
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(alertId.toInt(), notification)
    }

    private fun formatPrice(value: Double): String {
        return if (value >= 100) String.format("%.2f", value) else String.format("%.5f", value)
    }
}
