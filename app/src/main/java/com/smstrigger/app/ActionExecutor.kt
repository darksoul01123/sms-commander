package com.smstrigger.app

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat

object ActionExecutor {

    private const val TAG = "ActionExecutor"
    private const val CHANNEL_ID = "sms_commander_channel"

    fun executeLock(context: Context, sender: String, messageText: String): Boolean {
        val prefs = PreferencesHelper(context)
        Log.i(TAG, "Executing Lock from $sender with message: $messageText")

        // 1. Give subtle haptic feedback
        vibrate(context)

        // 2. Perform lock
        val locked = LockAccessibilityService.lockScreen()

        // 3. Record log
        val status = if (locked) "Screen Locked" else "FAILED: Accessibility Service Not Enabled"
        prefs.addLog(sender, messageText, status)

        if (!locked) {
            notifyAccessibilityMissing(context)
        }

        return locked
    }

    private fun vibrate(context: Context) {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val vibratorManager =
                    context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
                vibratorManager?.defaultVibrator?.vibrate(
                    VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.vibrate(
                        VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE)
                    )
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(150)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error vibrating", e)
        }
    }

    private fun notifyAccessibilityMissing(context: Context) {
        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "SMS Commander Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alerts when an SMS action cannot be executed"
            }
            notificationManager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("SMS Trigger Failed")
            .setContentText("Trigger received, but 'SMS Commander Screen Lock' Accessibility Service is disabled in settings.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(101, notification)
    }
}
