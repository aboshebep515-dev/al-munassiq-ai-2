package com.almunassiq.ai

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.Switch
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val prefs = getSharedPreferences("almunassiq_prefs", Context.MODE_PRIVATE)

        val scrollView = ScrollView(this).apply {
            setBackgroundColor(Color.parseColor("#121212"))
        }

        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(48, 64, 48, 64)
        }

        val titleView = TextView(this).apply {
            text = "المنسّق أيه آي"
            textSize = 26f
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 24)
        }

        val subtitleView = TextView(this).apply {
            text = "إعدادات الرد التلقائي الذكي عبر Gemini"
            textSize = 15f
            setTextColor(Color.LTGRAY)
            gravity = Gravity.CENTER
            setPadding(0, 0, 0, 40)
        }

        val apiKeyLabel = TextView(this).apply {
            text = "مفتاح Gemini API"
            textSize = 15f
            setTextColor(Color.WHITE)
            setPadding(0, 0, 0, 12)
        }

        val apiKeyInput = EditText(this).apply {
            hint = "الصق مفتاح API هنا"
            setHintTextColor(Color.GRAY)
            setTextColor(Color.WHITE)
            setBackgroundColor(Color.parseColor("#1E1E1E"))
            setPadding(32, 28, 32, 28)
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
            setText(prefs.getString("gemini_api_key", ""))
        }

        val autoReplySwitch = Switch(this).apply {
            text = "تفعيل الرد التلقائي"
            textSize = 16f
            setTextColor(Color.WHITE)
            setPadding(0, 36, 0, 36)
            isChecked = prefs.getBoolean("auto_reply_enabled", false)
        }

        val saveButton = Button(this).apply {
            text = "حفظ الإعدادات"
            textSize = 16f
            setPadding(0, 28, 0, 28)
            setOnClickListener {
                val key = apiKeyInput.text?.toString()?.trim().orEmpty()
                val enabled = autoReplySwitch.isChecked
                prefs.edit()
                    .putString("gemini_api_key", key)
                    .putBoolean("auto_reply_enabled", enabled)
                    .apply()
                Toast.makeText(this@MainActivity, "تم حفظ الإعدادات بنجاح", Toast.LENGTH_SHORT).show()
            }
        }

        val permissionButton = Button(this).apply {
            text = "فتح إذن الوصول للإشعارات"
            textSize = 15f
            setPadding(0, 28, 0, 28)
            val params = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = 24
            }
            layoutParams = params
            setOnClickListener {
                startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
            }
        }

        layout.addView(titleView)
        layout.addView(subtitleView)
        layout.addView(apiKeyLabel)
        layout.addView(apiKeyInput)
        layout.addView(autoReplySwitch)
        layout.addView(saveButton)
        layout.addView(permissionButton)

        scrollView.addView(layout)
        setContentView(scrollView)
    }
}
