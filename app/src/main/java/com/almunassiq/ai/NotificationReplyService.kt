package com.almunassiq.ai

import android.app.RemoteInput
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class NotificationReplyService : NotificationListenerService() {

    private val serviceJob = SupervisorJob()
    private val serviceScope = CoroutineScope(Dispatchers.Main + serviceJob)

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: return
        if (packageName == this.packageName) return

        val extras = sbn.notification.extras ?: return
        val sender = extras.getString("android.title")?.trim().orEmpty()
        val messageText = extras.getCharSequence("android.text")?.toString()?.trim().orEmpty()

        if (messageText.isEmpty()) return

        val replyAction = findReplyAction(sbn) ?: return
        val remoteInput = replyAction.remoteInputs?.firstOrNull() ?: return

        val prefs = getSharedPreferences("almunassiq_prefs", Context.MODE_PRIVATE)
        val autoReplyEnabled = prefs.getBoolean("auto_reply_enabled", false)
        if (!autoReplyEnabled) return

        val apiKey = prefs.getString("gemini_api_key", "")?.trim().orEmpty()

        serviceScope.launch {
            val replyText = if (apiKey.isNotEmpty()) {
                try {
                    val smartReply = withContext(Dispatchers.IO) {
                        GeminiReplyClient.generateReply(
                            apiKey = apiKey,
                            sender = sender,
                            incomingMessage = messageText,
                            appPackage = packageName
                        )
                    }
                    smartReply.trim().ifEmpty { buildFallbackReply(sender, messageText) }
                } catch (_: Exception) {
                    buildFallbackReply(sender, messageText)
                }
            } else {
                buildFallbackReply(sender, messageText)
            }

            sendDirectReply(replyAction, remoteInput, replyText)
        }
    }

    private fun findReplyAction(sbn: StatusBarNotification): android.app.Notification.Action? {
        val actions = sbn.notification.actions ?: return null
        return actions.firstOrNull { action ->
            val hasRemoteInput = !action.remoteInputs.isNullOrEmpty()
            val title = action.title?.toString()?.lowercase().orEmpty()
            hasRemoteInput && (
                title.contains("reply") ||
                    title.contains("رد") ||
                    title.contains("إرسال") ||
                    title.contains("ارسال") ||
                    action.semanticAction == android.app.Notification.Action.SEMANTIC_ACTION_REPLY
                )
        } ?: actions.firstOrNull { !it.remoteInputs.isNullOrEmpty() }
    }

    private fun sendDirectReply(
        action: android.app.Notification.Action,
        remoteInput: RemoteInput,
        replyText: String
    ) {
        try {
            val intent = Intent()
            val bundle = Bundle().apply {
                putCharSequence(remoteInput.resultKey, replyText)
            }
            RemoteInput.addResultsToIntent(arrayOf(remoteInput), intent, bundle)
            action.actionIntent.send(this, 0, intent)
        } catch (_: Exception) {
        }
    }

    private fun buildFallbackReply(sender: String, incomingMessage: String): String {
        val who = if (sender.isNotEmpty()) " يا $sender" else ""
        return "أهلاً$who، تم استلام رسالتك بخصوص: \"$incomingMessage\" وسأرد عليك قريباً."
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }
}
