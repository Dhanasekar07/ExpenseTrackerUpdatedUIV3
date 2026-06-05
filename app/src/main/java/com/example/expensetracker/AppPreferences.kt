package com.example.expensetracker

import android.content.Context
import android.content.SharedPreferences

object AppPreferences {

    private const val PREF_NAME    = "expense_tracker_prefs"
    private const val KEY_USERNAME = "pref_username"
    private const val KEY_COUNTRY  = "pref_country"
    private const val KEY_CURRENCY = "pref_currency_symbol"
    private const val KEY_NIGHT    = "pref_night_mode"
    private const val KEY_MIN      = "pref_min_amount"
    private const val KEY_MAX      = "pref_max_amount"
    private const val KEY_ONBOARD  = "pref_onboarded"

    private fun prefs(ctx: Context): SharedPreferences =
        ctx.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)

    // ── Onboarding ──────────────────────────────────────────────────────────
    fun isOnboarded(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_ONBOARD, false)

    fun setOnboarded(ctx: Context) =
        prefs(ctx).edit().putBoolean(KEY_ONBOARD, true).apply()

    // ── Username ─────────────────────────────────────────────────────────────
    fun getUsername(ctx: Context): String =
        prefs(ctx).getString(KEY_USERNAME, "") ?: ""

    fun setUsername(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_USERNAME, v).apply()

    // ── Country & Currency ───────────────────────────────────────────────────
    fun getCountry(ctx: Context): String =
        prefs(ctx).getString(KEY_COUNTRY, "India") ?: "India"

    fun setCountry(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_COUNTRY, v).apply()

    fun getCurrencySymbol(ctx: Context): String =
        prefs(ctx).getString(KEY_CURRENCY, "₹") ?: "₹"

    fun setCurrencySymbol(ctx: Context, v: String) =
        prefs(ctx).edit().putString(KEY_CURRENCY, v).apply()

    // ── Night Mode ───────────────────────────────────────────────────────────
    fun isNightMode(ctx: Context): Boolean =
        prefs(ctx).getBoolean(KEY_NIGHT, false)

    fun setNightMode(ctx: Context, v: Boolean) =
        prefs(ctx).edit().putBoolean(KEY_NIGHT, v).apply()

    // ── Thresholds ───────────────────────────────────────────────────────────
    fun getMinAmount(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_MIN, 0f)

    fun setMinAmount(ctx: Context, v: Float) =
        prefs(ctx).edit().putFloat(KEY_MIN, v).apply()

    fun getMaxAmount(ctx: Context): Float =
        prefs(ctx).getFloat(KEY_MAX, -1f)

    fun setMaxAmount(ctx: Context, v: Float) =
        prefs(ctx).edit().putFloat(KEY_MAX, v).apply()
}
