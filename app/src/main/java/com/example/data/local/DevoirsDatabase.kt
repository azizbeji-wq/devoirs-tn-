package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [DevoirEntity::class, StarredIdeaEntity::class],
    version = 1,
    exportSchema = false
)
abstract class DevoirsDatabase : RoomDatabase() {
    abstract fun devoirDao(): DevoirDao
    abstract fun starredIdeaDao(): StarredIdeaDao

    companion object {
        @Volatile
        private var INSTANCE: DevoirsDatabase? = null

        fun getInstance(context: Context): DevoirsDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    DevoirsDatabase::class.java,
                    "devoirs_tn_database"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
