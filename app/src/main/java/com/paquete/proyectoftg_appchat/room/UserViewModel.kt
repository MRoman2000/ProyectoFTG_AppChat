package com.paquete.proyectoftg_appchat.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.liveData
import androidx.lifecycle.viewModelScope
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ElementosViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepository(application)
    val elementos: LiveData<List<DataUser>> = userRepository.getElementos()

    val contactoSeleccionado1 = MutableLiveData<DataUser>()
    val elementoUsuarioActual: LiveData<DataUser> = contactoSeleccionado1

    private val _usuarios = MutableLiveData<List<DataUser>>()
    val usuarios: LiveData<List<DataUser>> = _usuarios


    val contactoSeleccionado = MutableLiveData<Contactos>()


    fun seleccionarContacto1(contacto: DataUser) {
        contactoSeleccionado1.value = contacto
    }

    fun contactoSelecionado1(): LiveData<DataUser> {
        return contactoSeleccionado1
    }

    fun seleccionarContacto(contacto: Contactos) {
        contactoSeleccionado.value = contacto
    }

    fun contactoSelecionado(): LiveData<Contactos> {
        return contactoSeleccionado
    }


    fun obtenerDatosUsuario(uidUsuario: String?): LiveData<DataUser?> {
        return liveData(viewModelScope.coroutineContext) {
            val usuario = uidUsuario?.let { userRepository.getDataUser(it) }
            emit(usuario)
        }
    }

    fun obtenerDatosYElementoUsuarioActual(uidUsuarioActual: String?): LiveData<DataUser?> {
        return liveData(viewModelScope.coroutineContext) {
            val usuario = withContext(Dispatchers.IO) {
                uidUsuarioActual?.let { userRepository.getDataUser(it) }
            }
            emit(usuario)
        }
    }



    fun insertar(elemento: DataUser) {
        viewModelScope.launch {
            userRepository.insertar(elemento)
        }
    }
}









