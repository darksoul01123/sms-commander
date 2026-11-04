package com.smstrigger.app

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Telephony
import android.telephony.SmsMessage
import android.util.Log

class SmsReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "SmsReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) {
            return
        }

        val prefs = PreferencesHelper(context)
        if (!prefs.isEnabled) {
            Log.d(TAG, "SMS received but SMS Commander is disabled by user.")
            return
        }

        // Acquire a temporary partial wakelock so CPU doesn't sleep mid-execution
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
        val wakeLock = powerManager?.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "SMSCommander:SmsWakeLock"
        )
        wakeLock?.acquire(3000)

        try {
            val messages = extractSmsMessages(intent)
            if (messages.isEmpty()) {
                Log.w(TAG, "No SMS messages extracted from intent.")
                return
            }

            // Combine multipart SMS segments from same sender
            val sender = messages.first().originatingAddress ?: "Unknown"
            val fullBody = messages.joinToString(separator = "") { it.messageBody ?: "" }

            Log.i(TAG, "Incoming SMS from $sender: \"$fullBody\"")

            val triggerCode = prefs.triggerCode
            val isMatched = checkMatch(fullBody, triggerCode, prefs.matchMode)

            if (isMatched) {
                Log.i(TAG, "Trigger matched! Keyword: \"$triggerCode\". Executing action...")
                ActionExecutor.executeLock(context, sender, fullBody)
            } else {
                Log.d(TAG, "Message does not match trigger code: \"$triggerCode\"")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing incoming SMS", e)
        } finally {
            if (wakeLock?.isHeld == true) {
                wakeLock.release()
            }
        }
    }

    private fun checkMatch(messageBody: String, triggerCode: String, matchMode: String): Boolean {
        val cleanMsg = messageBody.trim().lowercase()
        val cleanTrigger = triggerCode.trim().lowercase()

        if (cleanTrigger.isEmpty()) return false

        return when (matchMode) {
            PreferencesHelper.MATCH_CONTAINS -> cleanMsg.contains(cleanTrigger)
            PreferencesHelper.MATCH_EXACT -> cleanMsg == cleanTrigger
            else -> cleanMsg == cleanTrigger
        }
    }

    private fun extractSmsMessages(intent: Intent): List<SmsMessage> {
        val messages = mutableListOf<SmsMessage>()
        try {
            // Modern API: Telephony.Sms.Intents.getMessagesFromIntent
            val msgs = Telephony.Sms.Intents.getMessagesFromIntent(intent)
            if (!msgs.isNullOrEmpty()) {
                messages.addAll(msgs)
                return messages
            }
        } catch (e: Exception) {
            Log.w(TAG, "Standard getMessagesFromIntent failed, falling back to raw pdus", e)
        }

        // Fallback for older formats / edge cases
        try {
            val bundle = intent.extras ?: return messages
            val pdus = bundle.get("pdus") as? Array<*> ?: return messages
            val format = bundle.getString("format")

            for (pdu in pdus) {
                val bytes = pdu as? ByteArray ?: continue
                val msg = if (format != null) {
                    SmsMessage.createFromPdu(bytes, format)
                } else {
                    @Suppress("DEPRECATION")
                    SmsMessage.createFromPdu(bytes)
                }
                if (msg != null) {
                    messages.add(msg)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse pdus", e)
        }

        return messages
    }
}
