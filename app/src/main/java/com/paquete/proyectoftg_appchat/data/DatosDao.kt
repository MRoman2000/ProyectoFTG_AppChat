package com.paquete.proyectoftg_appchat.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paquete.proyectoftg_appchat.model.DataUser

@Dao
interface DatosDao {
    @Query("SELECT * FROM DataUser")
    fun getAllUsuarios(): LiveData<List<DataUser>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertar(datos: DataUser)

    @Query("SELECT * FROM DataUser")
    fun getElementos(): LiveData<List<DataUser>>

    @Update
    suspend fun update(datos: DataUser)

    @Query("SELECT * FROM DataUser WHERE uid = :uid")
    suspend fun getDataUser(uid: String): DataUser?
}