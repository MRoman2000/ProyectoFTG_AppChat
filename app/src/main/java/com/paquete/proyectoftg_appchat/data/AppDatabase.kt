package com.paquete.proyectoftg_appchat.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.paquete.proyectoftg_appchat.model.DataUser

@Database(entities = [DataUser::class], version = 3)
abstract class AppDatabase : RoomDatabase() {
    abstract fun datosDao(): DatosDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance =
                    Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "DataUser").fallbackToDestructiveMigration()
                        .build()
                INSTANCE = instance
                instance
            }
        }
    }
}

