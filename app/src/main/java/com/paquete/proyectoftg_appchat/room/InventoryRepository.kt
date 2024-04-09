package com.paquete.proyectoftg_appchat.room

import android.app.Application
import androidx.lifecycle.LiveData
import com.paquete.proyectoftg_appchat.data.AppDatabase
import com.paquete.proyectoftg_appchat.data.DatosDao
import com.paquete.proyectoftg_appchat.model.DataUser
import java.util.concurrent.Executor
import java.util.concurrent.Executors

class UserRepository(application: Application) {
    private val executor: Executor = Executors.newSingleThreadExecutor()
    private val elementosDao: DatosDao = AppDatabase.getDatabase(application).datosDao()
    fun getElementos(): LiveData<List<DataUser>> {
        return elementosDao.getElementos()
    }


    fun insertar(elemento: DataUser) {
        executor.execute {
            elementosDao.insertar(elemento)
        }
    }
    suspend fun getDataUser(uid: String): DataUser? {
        return elementosDao.getDataUser(uid)
    }
}