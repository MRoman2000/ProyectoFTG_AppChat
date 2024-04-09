package com.paquete.proyectoftg_appchat.room

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser
import kotlinx.coroutines.launch

class ElementosViewModel(application: Application) : AndroidViewModel(application) {
    private val userRepository: UserRepository = UserRepository(application)
    val elementos: LiveData<List<DataUser>> = userRepository.getElementos()

    private val _elementoUsuarioActual = MutableLiveData<DataUser?>()
    val elementoUsuarioActual: LiveData<DataUser?> = _elementoUsuarioActual

    private val _usuarios = MutableLiveData<List<DataUser>>()
    val usuarios: LiveData<List<DataUser>> = _usuarios


    val contactoSeleccionado = MutableLiveData<Contactos>()



    fun seleccionarContacto(contacto: Contactos) {
        contactoSeleccionado.value = contacto
    }

    fun contactoSelecionado(): LiveData<Contactos> {
        return contactoSeleccionado
    }
    fun obtenerDatosUsuario(uidUsuario: String?): LiveData<DataUser?> {
        val datosUsuario = MutableLiveData<DataUser?>()
        viewModelScope.launch {
            val usuario = uidUsuario?.let { userRepository.getDataUser(it) }
            datosUsuario.postValue(usuario)
        }
        return datosUsuario
    }
    fun obtenerElementoUsuarioActual(uidUsuarioActual: String?) {
        elementos.observeForever { elementos ->
            val elementoUsuarioActual = elementos.firstOrNull { it.uid == uidUsuarioActual }
            _elementoUsuarioActual.value = elementoUsuarioActual
        }
    }

    fun insertar(elemento: DataUser) {
        viewModelScope.launch {
            userRepository.insertar(elemento)
        }
    }

}









