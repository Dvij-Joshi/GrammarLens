package com.example.grammarlens.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MistakeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMistake(mistake: MistakeEntity): Long

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC")
    fun getAllMistakes(): Flow<List<MistakeEntity>>

    @Query("SELECT * FROM mistakes ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentMistakes(limit: Int): Flow<List<MistakeEntity>>

    @Query("SELECT COUNT(*) FROM mistakes")
    fun getTotalMistakesCount(): Flow<Int>
}
