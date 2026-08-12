package com.milad.pricealarm.data

import androidx.room.Entity
import androidx.room.PrimaryKey

enum class AlertCondition {
    ABOVE, BELOW
}

@Entity(tableName = "alerts")
data class PriceAlert(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val symbol: String,
    val targetPrice: Double,
    val condition: AlertCondition,
    val isActive: Boolean = true,
    val repeatAlert: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastTriggeredAt: Long? = null
)
