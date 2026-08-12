package com.milad.pricealarm.network

import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * Thin client around Twelve Data's /price endpoint.
 * Free API key: https://twelvedata.com/apikey
 *
 * Supports batching multiple symbols in a single HTTP call, which helps
 * stay comfortably within the free-tier rate limit.
 */
object PriceApiClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    sealed class Result {
        data class Success(val prices: Map<String, Double>) : Result()
        data class Error(val message: String) : Result()
    }

    fun fetchPrices(symbols: Collection<String>, apiKey: String): Result {
        if (symbols.isEmpty()) return Result.Success(emptyMap())
        val joined = symbols.joinToString(",")
        val encoded = URLEncoder.encode(joined, "UTF-8")
        val url = "https://api.twelvedata.com/price?symbol=$encoded&apikey=$apiKey"

        return try {
            val request = Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful || body.isBlank()) {
                    return Result.Error("خطای شبکه: کد ${response.code}")
                }
                parseResponse(body, symbols)
            }
        } catch (e: Exception) {
            Result.Error("خطا در اتصال: ${e.message}")
        }
    }

    private fun parseResponse(body: String, requestedSymbols: Collection<String>): Result {
        val json = try {
            JSONObject(body)
        } catch (e: Exception) {
            return Result.Error("پاسخ نامعتبر از سرور")
        }

        // Single symbol response shape: {"price": "123.45"}
        if (json.has("price")) {
            val symbol = requestedSymbols.firstOrNull() ?: return Result.Error("نماد نامشخص")
            val price = json.optString("price").toDoubleOrNull()
                ?: return Result.Error("قیمت نامعتبر برای $symbol")
            return Result.Success(mapOf(symbol to price))
        }

        // Error response shape: {"code": 400, "message": "..."}
        if (json.has("code") && json.has("message")) {
            return Result.Error(json.optString("message"))
        }

        // Multi symbol response shape: {"EUR/USD": {"price": "1.08"}, "XAU/USD": {"price": "2400"}}
        val result = mutableMapOf<String, Double>()
        val keys = json.keys()
        while (keys.hasNext()) {
            val key = keys.next()
            val obj = json.optJSONObject(key) ?: continue
            val price = obj.optString("price").toDoubleOrNull() ?: continue
            result[key] = price
        }

        return if (result.isEmpty()) {
            Result.Error("داده‌ای دریافت نشد")
        } else {
            Result.Success(result)
        }
    }
}
