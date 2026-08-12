package com.milad.pricealarm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.milad.pricealarm.data.Prefs
import com.milad.pricealarm.network.PriceApiClient
import com.milad.pricealarm.service.PriceMonitorService
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun PricesScreen(prefs: Prefs) {
    var watchlist by remember { mutableStateOf(prefs.watchlist.toList().sorted()) }
    var prices by remember { mutableStateOf<Map<String, Double>>(emptyMap()) }
    var status by remember { mutableStateOf("در حال دریافت...") }
    var showAddSymbol by remember { mutableStateOf(false) }
    var newSymbol by remember { mutableStateOf("") }

    // If the background service is running, reuse its data. Otherwise poll locally
    // only while this screen is visible (lightweight, foreground-only).
    LaunchedEffect(watchlist) {
        while (true) {
            val monitoring = prefs.isMonitoring
            if (monitoring) {
                prices = PriceMonitorService.observePrices().value
                status = PriceMonitorService.observeStatus().value
            } else {
                val key = prefs.apiKey
                if (!key.isNullOrBlank() && watchlist.isNotEmpty()) {
                    val result = withContext(Dispatchers.IO) {
                        PriceApiClient.fetchPrices(watchlist, key)
                    }
                    when (result) {
                        is PriceApiClient.Result.Success -> {
                            prices = result.prices
                            status = "بروزرسانی دستی"
                        }
                        is PriceApiClient.Result.Error -> status = result.message
                    }
                }
            }
            delay(30_000)
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("قیمت‌های لحظه‌ای", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            IconButton(onClick = { showAddSymbol = true }) {
                Icon(Icons.Default.Add, contentDescription = "افزودن نماد")
            }
        }
        Text(status, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(12.dp))

        if (watchlist.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز نمادی اضافه نکردی. با دکمه + شروع کن.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(watchlist) { symbol ->
                    PriceRow(
                        symbol = symbol,
                        price = prices[symbol],
                        onRemove = {
                            val updated = watchlist.filterNot { it == symbol }
                            watchlist = updated
                            prefs.watchlist = updated.toSet()
                        }
                    )
                }
            }
        }
    }

    if (showAddSymbol) {
        AlertDialog(
            onDismissRequest = { showAddSymbol = false },
            title = { Text("افزودن نماد") },
            text = {
                Column {
                    Text(
                        "نماد رو به فرمت Twelve Data وارد کن. مثال‌ها: EUR/USD ، GBP/USD ، XAU/USD (طلا) ، BTC/USD",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = newSymbol,
                        onValueChange = { newSymbol = it.uppercase() },
                        singleLine = true,
                        label = { Text("نماد") },
                        placeholder = { Text("XAU/USD") }
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val s = newSymbol.trim()
                    if (s.isNotEmpty()) {
                        val updated = (watchlist + s).distinct().sorted()
                        watchlist = updated
                        prefs.watchlist = updated.toSet()
                    }
                    newSymbol = ""
                    showAddSymbol = false
                }) { Text("افزودن") }
            },
            dismissButton = {
                TextButton(onClick = { showAddSymbol = false }) { Text("انصراف") }
            }
        )
    }
}

@Composable
private fun PriceRow(symbol: String, price: Double?, onRemove: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(symbol, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = price?.let { formatPrice(it) } ?: "—",
                    style = MaterialTheme.typography.titleMedium
                )
                IconButton(onClick = onRemove) {
                    Icon(Icons.Default.Close, contentDescription = "حذف", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

private fun formatPrice(value: Double): String {
    return if (value >= 100) String.format("%.2f", value) else String.format("%.5f", value)
}
