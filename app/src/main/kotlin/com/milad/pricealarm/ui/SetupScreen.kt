package com.milad.pricealarm.ui

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.milad.pricealarm.data.Prefs

@Composable
fun SetupScreen(prefs: Prefs, onDone: () -> Unit) {
    var keyInput by remember { mutableStateOf(prefs.apiKey ?: "") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Key, contentDescription = null, modifier = Modifier.padding(bottom = 16.dp))
        Text("راه‌اندازی اولیه", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(12.dp))
        Text(
            "برای دریافت قیمت لحظه‌ای فارکس، طلا و کریپتو، به یک کلید رایگان از Twelve Data نیاز داری. " +
                "به سایت twelvedata.com برو، ثبت‌نام رایگان کن و کلید API رو از داشبورد کپی کن.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("کلید API") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = {
                prefs.apiKey = keyInput.trim()
                onDone()
            },
            enabled = keyInput.isNotBlank(),
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("ذخیره و ادامه")
        }
    }
}
