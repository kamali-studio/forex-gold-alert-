@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.milad.pricealarm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.milad.pricealarm.data.AlertCondition
import com.milad.pricealarm.data.AppDatabase
import com.milad.pricealarm.data.Prefs
import com.milad.pricealarm.data.PriceAlert
import kotlinx.coroutines.launch

@Composable
fun AlertsScreen(
    prefs: Prefs,
    db: AppDatabase,
    monitoringEnabled: Boolean,
    onToggleMonitoring: (Boolean) -> Unit
) {
    val scope = rememberCoroutineScope()
    val alerts by db.alertDao().observeAll().collectAsState(initial = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("آلارم‌ها", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))

        ElevatedCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("مانیتورینگ پس‌زمینه", fontWeight = FontWeight.SemiBold)
                    Text(
                        "وقتی روشنه، حتی با بسته بودن اپ قیمت‌ها چک میشن و آلارم صدا میده.",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Switch(checked = monitoringEnabled, onCheckedChange = onToggleMonitoring)
            }
        }

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("لیست آلارم‌ها", style = MaterialTheme.typography.titleMedium)
            IconButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "افزودن آلارم")
            }
        }

        Spacer(Modifier.height(8.dp))

        if (alerts.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("هنوز آلارمی نساختی. با دکمه + یکی اضافه کن.")
            }
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(alerts, key = { it.id }) { alert ->
                    AlertRow(
                        alert = alert,
                        onToggleActive = { active ->
                            scope.launch { db.alertDao().setActive(alert.id, active) }
                        },
                        onDelete = {
                            scope.launch { db.alertDao().delete(alert) }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddAlertDialog(
            defaultSymbols = prefs.watchlist.toList().sorted(),
            onDismiss = { showAddDialog = false },
            onConfirm = { symbol, condition, target, repeat ->
                scope.launch {
                    db.alertDao().insert(
                        PriceAlert(
                            symbol = symbol,
                            targetPrice = target,
                            condition = condition,
                            repeatAlert = repeat
                        )
                    )
                }
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun AlertRow(alert: PriceAlert, onToggleActive: (Boolean) -> Unit, onDelete: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(alert.symbol, fontWeight = FontWeight.SemiBold, style = MaterialTheme.typography.titleMedium)
                val dirText = if (alert.condition == AlertCondition.ABOVE) "بالاتر از" else "پایین‌تر از"
                Text("$dirText ${alert.targetPrice}", style = MaterialTheme.typography.bodyMedium)
                if (alert.repeatAlert) {
                    Text("تکرارشونده", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
            Switch(checked = alert.isActive, onCheckedChange = onToggleActive)
            IconButton(onClick = onDelete) {
                Icon(Icons.Default.Delete, contentDescription = "حذف")
            }
        }
    }
}

@Composable
private fun AddAlertDialog(
    defaultSymbols: List<String>,
    onDismiss: () -> Unit,
    onConfirm: (symbol: String, condition: AlertCondition, target: Double, repeat: Boolean) -> Unit
) {
    var symbol by remember { mutableStateOf(defaultSymbols.firstOrNull() ?: "XAU/USD") }
    var symbolExpanded by remember { mutableStateOf(false) }
    var condition by remember { mutableStateOf(AlertCondition.ABOVE) }
    var priceText by remember { mutableStateOf("") }
    var repeat by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("آلارم جدید") },
        text = {
            Column {
                ExposedDropdownMenuBox(expanded = symbolExpanded, onExpandedChange = { symbolExpanded = it }) {
                    OutlinedTextField(
                        value = symbol,
                        onValueChange = { symbol = it.uppercase() },
                        label = { Text("نماد") },
                        singleLine = true,
                        modifier = Modifier.menuAnchor().fillMaxWidth()
                    )
                    ExposedDropdownMenu(expanded = symbolExpanded, onDismissRequest = { symbolExpanded = false }) {
                        defaultSymbols.forEach { s ->
                            DropdownMenuItem(text = { Text(s) }, onClick = {
                                symbol = s
                                symbolExpanded = false
                            })
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    FilterChip(
                        selected = condition == AlertCondition.ABOVE,
                        onClick = { condition = AlertCondition.ABOVE },
                        label = { Text("بالاتر از") }
                    )
                    Spacer(Modifier.width(8.dp))
                    FilterChip(
                        selected = condition == AlertCondition.BELOW,
                        onClick = { condition = AlertCondition.BELOW },
                        label = { Text("پایین‌تر از") }
                    )
                }

                Spacer(Modifier.height(12.dp))

                OutlinedTextField(
                    value = priceText,
                    onValueChange = { priceText = it },
                    label = { Text("قیمت هدف") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(checked = repeat, onCheckedChange = { repeat = it })
                    Text("بعد از فعال شدن، دوباره فعال بمونه (تکرارشونده)")
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val target = priceText.toDoubleOrNull()
                    if (symbol.isNotBlank() && target != null) {
                        onConfirm(symbol.trim(), condition, target, repeat)
                    }
                },
                enabled = symbol.isNotBlank() && priceText.toDoubleOrNull() != null
            ) { Text("ذخیره") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("انصراف") }
        }
    )
}
