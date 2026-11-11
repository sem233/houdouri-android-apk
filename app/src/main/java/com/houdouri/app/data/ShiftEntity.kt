package com.houdouri.app.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "shifts")
data class ShiftEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,
    val startTime: Long,          // Epoch milliseconds
    val endTime: Long? = null,    // null = شيفت مفتوح حالياً
    val timezoneId: String = "Africa/Cairo",
    val note: String? = null
)
