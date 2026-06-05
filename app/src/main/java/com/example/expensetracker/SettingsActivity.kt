package com.example.expensetracker

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        val btnBack     = findViewById<android.widget.ImageView>(R.id.btnBack)
        val etUsername  = findViewById<EditText>(R.id.etUsername)
        val switchNight = findViewById<Switch>(R.id.switchNightMode)
        val etMin       = findViewById<EditText>(R.id.etMinAmount)
        val etMax       = findViewById<EditText>(R.id.etMaxAmount)

        // Load saved values
        etUsername.setText(AppPreferences.getUsername(this))
        switchNight.isChecked = AppPreferences.isNightMode(this)
        etMin.setText(AppPreferences.getMinAmount(this).toInt().toString())
        val maxAmt = AppPreferences.getMaxAmount(this)
        if (maxAmt > 0) etMax.setText(maxAmt.toInt().toString())

        btnBack.setOnClickListener { finish() }

        etUsername.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) {
                val name = etUsername.text.toString().trim()
                if (name.isNotEmpty()) AppPreferences.setUsername(this, name)
            }
        }

        switchNight.setOnCheckedChangeListener { _, isChecked ->
            AppPreferences.setNightMode(this, isChecked)
            AppCompatDelegate.setDefaultNightMode(
                if (isChecked) AppCompatDelegate.MODE_NIGHT_YES
                else           AppCompatDelegate.MODE_NIGHT_NO
            )
        }

        etMin.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) AppPreferences.setMinAmount(this, etMin.text.toString().toFloatOrNull() ?: 0f)
        }
        etMax.setOnFocusChangeListener { _, hasFocus ->
            if (!hasFocus) AppPreferences.setMaxAmount(this, etMax.text.toString().toFloatOrNull() ?: -1f)
        }

        // Bottom nav - settings active
        setupBottomNav()
    }

    private fun setupBottomNav() {
        // Highlight Settings
        listOf(
            Triple(R.id.navHomePill, R.id.navHomeIcon, R.id.navHomeLabel),
            Triple(R.id.navTransactionsPill, R.id.navTransactionsIcon, R.id.navTransactionsLabel),
            Triple(R.id.navCategoryPill, R.id.navCategoryIcon, R.id.navCategoryLabel),
            Triple(R.id.navSettingsPill, R.id.navSettingsIcon, R.id.navSettingsLabel)
        ).forEach { (pillId, iconId, labelId) ->
            findViewById<LinearLayout>(pillId).background = null
            findViewById<android.widget.ImageView>(iconId).alpha = 0.5f
            findViewById<TextView>(labelId).setTextColor(Color.parseColor("#9CA3AF"))
        }
        findViewById<LinearLayout>(R.id.navSettingsPill).setBackgroundResource(R.drawable.bg_nav_active_pill)
        findViewById<android.widget.ImageView>(R.id.navSettingsIcon).apply {
            alpha = 1f; setColorFilter(Color.parseColor("#2D6A4F"))
        }
        findViewById<TextView>(R.id.navSettingsLabel).setTextColor(Color.parseColor("#2D6A4F"))

        // Nav clicks
        findViewById<LinearLayout>(R.id.navHome).setOnClickListener {
            startActivity(Intent(this, MainActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navTransactions).setOnClickListener {
            startActivity(Intent(this, TransactionsActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navCategory).setOnClickListener {
            startActivity(Intent(this, ManageCategoriesActivity::class.java)); finish()
        }
        findViewById<LinearLayout>(R.id.navSettings).setOnClickListener { /* already here */ }
    }
}
