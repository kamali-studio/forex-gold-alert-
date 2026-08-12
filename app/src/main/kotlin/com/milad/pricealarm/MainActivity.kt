package com.milad.pricealarm

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.core.content.ContextCompat
import com.milad.pricealarm.data.AppDatabase
import com.milad.pricealarm.data.Prefs
import com.milad.pricealarm.service.PriceMonitorService
import com.milad.pricealarm.ui.AlertsScreen
import com.milad.pricealarm.ui.PricesScreen
import com.milad.pricealarm.ui.SetupScreen
import com.milad.pricealarm.ui.theme.PriceAlarmTheme

class MainActivity : ComponentActivity() {

    private lateinit var prefs: Prefs
    private lateinit var db: AppDatabase

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { /* result ignored; user can retry via system settings */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        db = AppDatabase.getInstance(this)

        requestNotificationPermissionIfNeeded()

        setContent {
            PriceAlarmTheme {
                var hasApiKey by remember { mutableStateOf(!prefs.apiKey.isNullOrBlank()) }
                var monitoring by remember { mutableStateOf(prefs.isMonitoring) }
                var selectedTab by remember { mutableIntStateOf(0) }

                if (!hasApiKey) {
                    SetupScreen(prefs = prefs, onDone = { hasApiKey = true })
                } else {
                    Scaffold(
                        bottomBar = {
                            NavigationBar {
                                NavigationBarItem(
                                    selected = selectedTab == 0,
                                    onClick = { selectedTab = 0 },
                                    icon = { Icon(Icons.Default.ShowChart, contentDescription = null) },
                                    label = { Text("قیمت‌ها") }
                                )
                                NavigationBarItem(
                                    selected = selectedTab == 1,
                                    onClick = { selectedTab = 1 },
                                    icon = { Icon(Icons.Default.Notifications, contentDescription = null) },
                                    label = { Text("آلارم‌ها") }
                                )
                            }
                        }
                    ) { padding ->
                        Surface(modifier = androidx.compose.ui.Modifier.padding(padding)) {
                            when (selectedTab) {
                                0 -> PricesScreen(prefs = prefs)
                                1 -> AlertsScreen(
                                    prefs = prefs,
                                    db = db,
                                    monitoringEnabled = monitoring,
                                    onToggleMonitoring = { enabled ->
                                        monitoring = enabled
                                        if (enabled) startMonitoringService() else stopMonitoringService()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun requestNotificationPermissionIfNeeded() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                this, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    private fun startMonitoringService() {
        val intent = Intent(this, PriceMonitorService::class.java)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            startForegroundService(intent)
        } else {
            startService(intent)
        }
    }

    private fun stopMonitoringService() {
        prefs.isMonitoring = false
        stopService(Intent(this, PriceMonitorService::class.java))
    }
}
