package com.example.grammarlens.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertMistake(mistake: MistakeEntity)

    @Query("DELETE FROM mistakes WHERE id = :id")
    fun deleteMistake(id: Long)

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMistakes(limit: Int): Flow<List<MistakeEntity>>

    @Query("SELECT COUNT(*) FROM mistakes")
    fun getTotalChecksCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM mistakes WHERE isCorrect = 0")
    fun getTotalMistakesCount(): Flow<Int>

    @Query("SELECT DISTINCT date(timestamp / 1000, 'unixepoch', 'localtime') FROM mistakes ORDER BY timestamp DESC")
    fun getDistinctCheckDates(): Flow<List<String>>
}
