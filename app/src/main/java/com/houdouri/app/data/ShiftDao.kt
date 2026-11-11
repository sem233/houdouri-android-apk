package com.houdouri.app.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface ShiftDao {

    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    fun observeAllShifts(): Flow<List<ShiftEntity>>

    @Query("SELECT * FROM shifts ORDER BY startTime DESC")
    suspend fun getAllShiftsOnce(): List<ShiftEntity>

    @Query("SELECT * FROM shifts WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getOpenShift(): ShiftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShift(shift: ShiftEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(shifts: List<ShiftEntity>)

    @Update
    suspend fun updateShift(shift: ShiftEntity)

    @Delete
    suspend fun deleteShift(shift: ShiftEntity)

    @Query("DELETE FROM shifts")
    suspend fun clearAllShifts()
}
