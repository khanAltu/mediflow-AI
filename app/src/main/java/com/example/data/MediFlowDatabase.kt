package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [Tenant::class, User::class, Patient::class, Appointment::class, Payment::class],
    version = 1,
    exportSchema = false
)
abstract class MediFlowDatabase : RoomDatabase() {
    abstract fun dao(): MediFlowDao

    companion object {
        @Volatile
        private var INSTANCE: MediFlowDatabase? = null

        fun getDatabase(context: Context): MediFlowDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    MediFlowDatabase::class.java,
                    "mediflow_database"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
