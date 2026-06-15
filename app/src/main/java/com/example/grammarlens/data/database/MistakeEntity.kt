package com.example.grammarlens.data.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "mistakes")
data class MistakeEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val originalText: String,
    val correctedText: String,
    val mistakeTypes: List<String>, // Stored as JSON string
    val isCorrect: Boolean = false,
    val timestamp: Long = System.currentTimeMillis()
)
