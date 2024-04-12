package com.paquete.proyectoftg_appchat.actividades

import android.content.ContentValues.TAG
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.util.Log
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.databinding.ActivityMainBinding
import com.paquete.proyectoftg_appchat.fragmentos.ChatRoomFragment
import com.paquete.proyectoftg_appchat.fragmentos.ConfiguracionFragment
import com.paquete.proyectoftg_appchat.fragmentos.ContactosFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.UserStatusService
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var elementosViewModel: ElementosViewModel


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        val isDarkMode = isDarkModeEnabled()
        // Configura el color de la barra de navegación según el modo
        val navigationBarColor = if (isDarkMode) {
            ContextCompat.getColor(this, R.color.dark_primary_blue)
        } else {
            ContextCompat.getColor(this, R.color.my_green_secondary_fixed_dim)
        }
        window.navigationBarColor = navigationBarColor

        elementosViewModel = ViewModelProvider(this).get(ElementosViewModel::class.java)
        val chatFragment = ChatRoomFragment()
        val contactos = ContactosFragment()
        val configuracion = ConfiguracionFragment()


        lifecycleScope.launch {
            addNewItem()
        }


        // Verifica si la actividad se está recreando debido a un cambio de configuración
        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit()
            binding.bottomNavigation.selectedItemId = R.id.Chat
        }



        binding.bottomNavigation.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.Chat -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, chatFragment).commit()
                }

                R.id.Contactos -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, contactos).commit()
                }

                R.id.Configuracion -> {
                    supportFragmentManager.beginTransaction().replace(R.id.main_frame_layout, configuracion).commit()
                }
            }
            true
        }
        startService(Intent(this, UserStatusService::class.java))

    }


    private fun isDarkModeEnabled(): Boolean {
        val currentNightMode = resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
        return currentNightMode == Configuration.UI_MODE_NIGHT_YES
    }


    private suspend fun addNewItem() {
        val firestore = FirebaseFirestore.getInstance()
        val usuarioActual = FirebaseAuth.getInstance().currentUser
        if (usuarioActual != null) {
            val querySnapshot = firestore.collection("usuarios").get().await()
            withContext(Dispatchers.IO) { // Cambiar al contexto del hilo de entrada/salida
                for (document in querySnapshot.documents) {
                    val fechaNacimiento = document.getString("fechaNacimiento") ?: ""
                    val email = document.getString("email") ?: ""
                    val nombreCompleto = document.getString("nombreCompleto") ?: ""
                    val nombreUsuario = document.getString("nombreUsuario") ?: ""
                    val telefono = document.getString("telefono") ?: ""
                    val uid = document.getString("uid") ?: ""
                    val url_image = document.getString("imageUrl") ?: ""
                    val elemento = DataUser(uid, email, fechaNacimiento, nombreCompleto, nombreUsuario, telefono, url_image, "")
                    elementosViewModel.insertar(elemento)
                }
            }
        } else {
            Log.e(TAG, "No hay usuario autenticado.")
        }
    }


    override fun onStart() {
        super.onStart()
        Utils.updateUserStatusOnline()
        stopService(Intent(this, UserStatusService::class.java))
    }

    override fun onStop() {
        super.onStop()
        Utils.updateUserStatusOffline()
        stopService(Intent(this, UserStatusService::class.java))
    }


    override fun onDestroy() {
        super.onDestroy()
        stopService(Intent(this, UserStatusService::class.java))
    }
}


