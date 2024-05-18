package com.paquete.proyectoftg_appchat.data

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser

@Dao
interface DatosDao {
    @Query("SELECT * FROM DataUser")
    fun getAllUsuarios(): LiveData<List<DataUser>>

    @Update
    suspend fun actualizarContacto(contacto: Contactos)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarContacto(datos: Contactos)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertar(datos: DataUser)

    @Query("SELECT * FROM DataUser")
    fun getElementos(): List<DataUser>

    @Query("SELECT * FROM  Contactos")
    fun obtenerContactos(): List<Contactos>


    @Query("DELETE FROM Contactos WHERE uid = :uid")
    fun eliminarContactoPorUID(uid: String)

    @Query("UPDATE DataUser SET imageUrl = :imageUrl WHERE uid = :uid")
    suspend fun updateUserImageUrl(uid: String, imageUrl: String)


    @Query("UPDATE DataUser SET email = :email WHERE uid = :uid")
    suspend fun updateEmail(uid: String, email: String)

    @Query("UPDATE DataUser SET nombreUsuario = :nombreUsuario WHERE uid = :uid")
    suspend fun updateNombreUsuario(uid: String, nombreUsuario: String)

    @Query("UPDATE DataUser SET nombreCompleto = :nombreCompleto WHERE uid = :uid")
    suspend fun updateNombreCompleto(uid: String, nombreCompleto: String)

    @Query("SELECT * FROM  datauser WHERE nombreUsuario LIKE '%' || :searchText || '%'")
    fun buscarContactos(searchText: String): List<DataUser>


    @Query("SELECT * FROM DataUser WHERE uid = :uid")
    suspend fun getDataUser(uid: String): DataUser?

    @Query("SELECT * FROM DataUser WHERE uid = :uid")
    suspend fun getContactos(uid: String): Contactos?

    @Query("DELETE FROM contactos")
    suspend fun eliminarTablaContactos()

}