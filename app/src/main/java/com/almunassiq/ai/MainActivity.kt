package com.almunassiq.ai

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SwitchCompat

class MainActivity : AppCompatActivity() {

    private lateinit var statusValueText: TextView
    private lateinit var permissionValueText: TextView
    private lateinit var serviceSwitch: SwitchCompat
    private lateinit var whatsappSwitch: SwitchCompat
    private lateinit var messengerSwitch: SwitchCompat
    private lateinit var chatAiSwitch: SwitchCompat
    private lateinit var apiKeyInput: EditText
    private lateinit var fallbackReplyInput: EditText

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("munassiq_prefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            )
            setPadding(36, 42, 36, 42)
        }

        val rootLayout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val titleText = TextView(this).apply {
            text = "المنسّق أيه آي"
            textSize = 26f
            setTypeface(typeface, Typeface.BOLD)
            setTextColor(Color.parseColor("#111827"))
        }

        val subtitleText = TextView(this).apply {
            text = "لوحة تحكم الرد التلقائي على رسائل الشات وواتساب وماسنجر وGemini"
            textSize = 14f
            setTextColor(Color.parseColor("#4B5563"))
            setPadding(0, 8, 0, 28)
        }

        statusValueText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 4, 0, 12)
        }

        serviceSwitch = SwitchCompat(this).apply {
            text = "تفعيل الرد التلقائي"
            textSize = 16f
            setPadding(0, 10, 0, 18)
            isChecked = prefs.getBoolean("service_enabled", true)
            setOnCheckedChangeListener { _, isChecked ->
                updateStatusLabel(isChecked)
            }
        }
        updateStatusLabel(serviceSwitch.isChecked)

        permissionValueText = TextView(this).apply {
            textSize = 14f
            setPadding(0, 8, 0, 10)
        }

        val permissionButton = Button(this).apply {
            text = "فتح إعدادات إذن الإشعارات"
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        val targetsLabel = TextView(this).apply {
            text = "التطبيقات المستهدفة"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 28, 0, 10)
        }

        whatsappSwitch = SwitchCompat(this).apply {
            text = "واتساب (WhatsApp)"
            textSize = 15f
            setPadding(0, 8, 0, 8)
            isChecked = prefs.getBoolean("whatsapp_enabled", true)
        }

        messengerSwitch = SwitchCompat(this).apply {
            text = "ماسنجر (Messenger)"
            textSize = 15f
            setPadding(0, 8, 0, 8)
            isChecked = prefs.getBoolean("messenger_enabled", true)
        }

        chatAiSwitch = SwitchCompat(this).apply {
            text = "تطبيقات الشات و Gemini"
            textSize = 15f
            setPadding(0, 8, 0, 16)
            isChecked = prefs.getBoolean("chat_ai_enabled", true)
        }

        val settingsLabel = TextView(this).apply {
            text = "إعدادات الرد و Gemini"
            textSize = 18f
            setTypeface(typeface, Typeface.BOLD)
            setPadding(0, 16, 0, 10)
        }

        apiKeyInput = EditText(this).apply {
            hint = "مفتاح Gemini API (اختياري حالياً)"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("gemini_api_key", "") ?: "")
            setPadding(20, 20, 20, 20)
        }

        fallbackReplyInput = EditText(this).apply {
            hint = "الرد الافتراضي عند عدم توفر مفتاح أو اتصال"
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_MULTI_LINE
            minLines = 2
            setText(
                prefs.getString(
                    "fallback_reply",
                    "تم استلام رسالتك، سنرد عليك قريباً."
                ) ?: ""
            )
            setPadding(20, 20, 20, 20)
        }

        val saveButton = Button(this).apply {
            text = "حفظ الإعدادات"
            gravity = Gravity.CENTER
            setPadding(0, 24, 0, 24)
            setOnClickListener {
                prefs.edit()
                    .putBoolean("service_enabled", serviceSwitch.isChecked)
                    .putBoolean("whatsapp_enabled", whatsappSwitch.isChecked)
                    .putBoolean("messenger_enabled", messengerSwitch.isChecked)
                    .putBoolean("chat_ai_enabled", chatAiSwitch.isChecked)
                    .putString("gemini_api_key", apiKeyInput.text.toString().trim())
                    .putString("fallback_reply", fallbackReplyInput.text.toString().trim())
                    .apply()
                refreshPermissionStatus()
                Toast.makeText(this@MainActivity, "تم حفظ الإعدادات", Toast.LENGTH_SHORT).show()
            }
        }

        val spacer = View(this).apply {
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                20
            )
        }

        rootLayout.addView(titleText)
        rootLayout.addView(subtitleText)
        rootLayout.addView(serviceSwitch)
        rootLayout.addView(statusValueText)
        rootLayout.addView(permissionValueText)
        rootLayout.addView(permissionButton)
        rootLayout.addView(targetsLabel)
        rootLayout.addView(whatsappSwitch)
        rootLayout.addView(messengerSwitch)
        rootLayout.addView(chatAiSwitch)
        rootLayout.addView(settingsLabel)
        rootLayout.addView(apiKeyInput)
        rootLayout.addView(spacer)
        rootLayout.addView(fallbackReplyInput)
        rootLayout.addView(
            View(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    24
                )
            }
        )
        rootLayout.addView(saveButton)

        scrollView.addView(rootLayout)
        setContentView(scrollView)

        refreshPermissionStatus()
    }

    override fun onResume() {
        super.onResume()
        refreshPermissionStatus()
    }

    private fun updateStatusLabel(enabled: Boolean) {
        statusValueText.text = if (enabled) {
            "حالة الخدمة: مفعّلة"
        } else {
            "حالة الخدمة: متوقفة"
        }
    }

    private fun refreshPermissionStatus() {
        val granted = isNotificationListenerEnabled(this)
        permissionValueText.text = if (granted) {
            "إذن الإشعارات: ممنوح"
        } else {
            "إذن الإشعارات: غير مفعّل بعد"
        }
    }

    private fun isNotificationListenerEnabled(context: Context): Boolean {
        val enabledListeners = Settings.Secure.getString(
            context.contentResolver,
            "enabled_notification_listeners"
        ) ?: return false
        val componentName = ComponentName(
            context,
            "com.almunassiq.ai.NotificationReplyService"
        )
        return enabledListeners.split(":").any {
            ComponentName.unflattenFromString(it) == componentName
        }
    }
}
