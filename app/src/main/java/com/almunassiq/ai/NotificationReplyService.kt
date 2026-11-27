package com.almunassiq.ai

import android.app.Notification
import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

class NotificationReplyService : NotificationListenerService() {

    private val repliedKeys = mutableSetOf<String>()

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        val notificationItem = sbn ?: return
        if (notificationItem.isOngoing) return

        val prefs = getSharedPreferences("munassiq_prefs", Context.MODE_PRIVATE)
        val serviceEnabled = prefs.getBoolean("service_enabled", true)
        if (!serviceEnabled) return

        val pkg = notificationItem.packageName ?: return
        if (pkg == packageName) return
        if (!isTargetAppEnabled(pkg, prefs)) return

        val notification = notificationItem.notification ?: return
        val extras = notification.extras ?: Bundle()
        val messageText = extractMessageText(extras)
        if (messageText.isBlank()) return

        val dedupKey = "${notificationItem.key}|${messageText.hashCode()}"
        synchronized(repliedKeys) {
            if (!repliedKeys.add(dedupKey)) return
            if (repliedKeys.size > 300) {
                repliedKeys.clear()
                repliedKeys.add(dedupKey)
            }
        }

        val replyText = prefs.getString(
            "fallback_reply",
            "تم استلام رسالتك، سنرد عليك قريباً."
        )?.trim().orEmpty()
        if (replyText.isEmpty()) return

        sendReplyAction(notification, replyText)
    }

    private fun isTargetAppEnabled(
        pkg: String,
        prefs: android.content.SharedPreferences
    ): Boolean {
        val whatsappEnabled = prefs.getBoolean("whatsapp_enabled", true)
        val messengerEnabled = prefs.getBoolean("messenger_enabled", true)
        val chatAiEnabled = prefs.getBoolean("chat_ai_enabled", true)

        val isWhatsapp = pkg == "com.whatsapp" || pkg == "com.whatsapp.w4b"
        val isMessenger = pkg == "com.facebook.orca" || pkg == "com.facebook.mlite"
        val isChatAi = pkg.contains("gemini", ignoreCase = true) ||
            pkg.contains("chatgpt", ignoreCase = true) ||
            pkg.contains("openai", ignoreCase = true) ||
            pkg.contains("copilot", ignoreCase = true) ||
            pkg.contains("claude", ignoreCase = true) ||
            pkg.contains("telegram", ignoreCase = true)

        return (isWhatsapp && whatsappEnabled) ||
            (isMessenger && messengerEnabled) ||
            (isChatAi && chatAiEnabled)
    }

    private fun extractMessageText(extras: Bundle): String {
        val textLines = extras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES)
        if (textLines != null && textLines.isNotEmpty()) {
            val lastLine = textLines.lastOrNull()?.toString()?.trim()
            if (!lastLine.isNullOrEmpty()) return lastLine
        }

        val bigText = extras.getCharSequence(Notification.EXTRA_BIG_TEXT)?.toString()?.trim()
        if (!bigText.isNullOrEmpty()) return bigText

        val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim()
        if (!text.isNullOrEmpty()) return text

        return ""
    }

    private fun sendReplyAction(notification: Notification, replyText: String) {
        val actions = notification.actions ?: return
        for (action in actions) {
            val remoteInputs = action.remoteInputs ?: continue
            if (remoteInputs.isEmpty()) continue

            val replyBundle = Bundle()
            for (input in remoteInputs) {
                replyBundle.putCharSequence(input.resultKey, replyText)
            }

            val fillInIntent = Intent().apply {
                addFlags(Intent.FLAG_RECEIVER_FOREGROUND)
            }
            RemoteInput.addResultsToIntent(remoteInputs, fillInIntent, replyBundle)

            try {
                action.actionIntent?.send(this, 0, fillInIntent)
                return
            } catch (_: Exception) {
            }
        }
    }
}
