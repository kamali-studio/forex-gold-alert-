package com.milad.pricealarm.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface AlertDao {
    @Query("SELECT * FROM alerts ORDER BY createdAt DESC")
    fun observeAll(): Flow<List<PriceAlert>>

    @Query("SELECT * FROM alerts WHERE isActive = 1")
    suspend fun getActiveAlerts(): List<PriceAlert>

    @Insert
    suspend fun insert(alert: PriceAlert): Long

    @Update
    suspend fun update(alert: PriceAlert)

    @Delete
    suspend fun delete(alert: PriceAlert)

    @Query("UPDATE alerts SET isActive = :active WHERE id = :id")
    suspend fun setActive(id: Long, active: Boolean)

    @Query("UPDATE alerts SET lastTriggeredAt = :time, isActive = :stillActive WHERE id = :id")
    suspend fun markTriggered(id: Long, time: Long, stillActive: Boolean)
}
