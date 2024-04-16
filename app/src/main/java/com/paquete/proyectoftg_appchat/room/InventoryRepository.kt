package com.paquete.proyectoftg_appchat.room

import com.paquete.proyectoftg_appchat.data.DatosDao
import com.paquete.proyectoftg_appchat.model.DataUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class UserRepository(private val datosDao: DatosDao) {

    suspend fun getElementos(): List<DataUser> {
        return withContext(Dispatchers.IO) {
            datosDao.getElementos()
        }
    }


    suspend fun insertar(usuarios: DataUser) {
        withContext(Dispatchers.IO) {
            datosDao.insertar(usuarios)
        }
    }

    suspend fun getDataUser(uid: String): DataUser? {
        return withContext(Dispatchers.IO) {
            datosDao.getDataUser(uid)
        }
    }
}
