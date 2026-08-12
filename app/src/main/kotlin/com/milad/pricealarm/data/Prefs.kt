package com.milad.pricealarm.data

import android.content.Context

class Prefs(context: Context) {
    private val sp = context.getSharedPreferences("price_alarm_prefs", Context.MODE_PRIVATE)

    var apiKey: String?
        get() = sp.getString("api_key", null)
        set(value) = sp.edit().putString("api_key", value).apply()

    var isMonitoring: Boolean
        get() = sp.getBoolean("is_monitoring", false)
        set(value) = sp.edit().putBoolean("is_monitoring", value).apply()

    var pollIntervalSeconds: Int
        get() = sp.getInt("poll_interval", 30)
        set(value) = sp.edit().putInt("poll_interval", value).apply()

    var watchlist: Set<String>
        get() = sp.getStringSet("watchlist", defaultWatchlist) ?: defaultWatchlist
        set(value) = sp.edit().putStringSet("watchlist", value).apply()

    companion object {
        val defaultWatchlist = setOf(
            "EUR/USD", "GBP/USD", "USD/JPY", "XAU/USD", "BTC/USD"
        )
    }
}
