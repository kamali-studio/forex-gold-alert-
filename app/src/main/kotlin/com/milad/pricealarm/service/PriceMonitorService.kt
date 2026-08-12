package com.milad.pricealarm.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.milad.pricealarm.data.AlertCondition
import com.milad.pricealarm.data.AppDatabase
import com.milad.pricealarm.data.Prefs
import com.milad.pricealarm.network.PriceApiClient
import com.milad.pricealarm.notification.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PriceMonitorService : Service() {

    private val scope = CoroutineScope(Dispatchers.Default + Job())
    private var loopJob: Job? = null
    private lateinit var prefs: Prefs
    private lateinit var db: AppDatabase

    override fun onCreate() {
        super.onCreate()
        prefs = Prefs(this)
        db = AppDatabase.getInstance(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NotificationHelper.SERVICE_NOTIFICATION_ID, NotificationHelper.buildServiceNotification(this, "در حال بررسی قیمت‌ها..."))
        prefs.isMonitoring = true
        startLoop()
        return START_STICKY
    }

    private fun startLoop() {
        loopJob?.cancel()
        loopJob = scope.launch {
            while (true) {
                try {
                    checkOnce()
                } catch (e: Exception) {
                    // swallow and keep looping; transient network errors shouldn't kill monitoring
                }
                val intervalMs = (prefs.pollIntervalSeconds.coerceAtLeast(15)) * 1000L
                delay(intervalMs)
            }
        }
    }

    private suspend fun checkOnce() {
        val apiKey = prefs.apiKey
        if (apiKey.isNullOrBlank()) {
            updateStatus("کلید API تنظیم نشده است")
            return
        }

        val activeAlerts = db.alertDao().getActiveAlerts()
        val watchSymbols = (prefs.watchlist + activeAlerts.map { it.symbol }).toSet()
        if (watchSymbols.isEmpty()) {
            updateStatus("نمادی برای پایش وجود ندارد")
            return
        }

        val result = withContext(Dispatchers.IO) {
            PriceApiClient.fetchPrices(watchSymbols, apiKey)
        }

        when (result) {
            is PriceApiClient.Result.Success -> {
                latestPrices.value = result.prices
                updateStatus("آخرین بروزرسانی: ${nowTime()}")
                evaluateAlerts(activeAlerts, result.prices)
            }
            is PriceApiClient.Result.Error -> {
                updateStatus("خطا: ${result.message}")
            }
        }
    }

    private suspend fun evaluateAlerts(alerts: List<com.milad.pricealarm.data.PriceAlert>, prices: Map<String, Double>) {
        for (alert in alerts) {
            val current = prices[alert.symbol] ?: continue
            val triggered = when (alert.condition) {
                AlertCondition.ABOVE -> current >= alert.targetPrice
                AlertCondition.BELOW -> current <= alert.targetPrice
            }
            if (triggered) {
                NotificationHelper.showAlertNotification(
                    this,
                    alert.id,
                    alert.symbol,
                    alert.condition,
                    alert.targetPrice,
                    current
                )
                db.alertDao().markTriggered(
                    alert.id,
                    System.currentTimeMillis(),
                    stillActive = alert.repeatAlert
                )
            }
        }
    }

    private fun updateStatus(text: String) {
        statusText.value = text
        val manager = getSystemService(NOTIFICATION_SERVICE) as android.app.NotificationManager
        manager.notify(
            NotificationHelper.SERVICE_NOTIFICATION_ID,
            NotificationHelper.buildServiceNotification(this, text)
        )
    }

    private fun nowTime(): String {
        val sdf = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.US)
        return sdf.format(java.util.Date())
    }

    override fun onDestroy() {
        super.onDestroy()
        prefs.isMonitoring = false
        loopJob?.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        val latestPrices: MutableStateFlow<Map<String, Double>> = MutableStateFlow(emptyMap())
        val statusText: MutableStateFlow<String> = MutableStateFlow("در حال آماده‌سازی...")

        fun observePrices(): StateFlow<Map<String, Double>> = latestPrices.asStateFlow()
        fun observeStatus(): StateFlow<String> = statusText.asStateFlow()
    }
}
