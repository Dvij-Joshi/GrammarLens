package com.example.grammarlens.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters

@Database(entities = [MistakeEntity::class], version = 2, exportSchema = false)
@TypeConverters(Converters::class)
abstract class GrammarDatabase : RoomDatabase() {

    abstract fun mistakeDao(): MistakeDao

    companion object {
        @Volatile
        private var INSTANCE: GrammarDatabase? = null

        fun getDatabase(context: Context): GrammarDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GrammarDatabase::class.java,
                    "grammar_lens_db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
