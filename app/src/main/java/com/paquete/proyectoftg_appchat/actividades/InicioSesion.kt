package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.widget.EditText
import androidx.appcompat.app.AppCompatActivity
import com.paquete.proyectoftg_appchat.databinding.ActivityInicioSesionBinding
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils

class InicioSesion : AppCompatActivity() {

    private lateinit var binding: ActivityInicioSesionBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityInicioSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setupListeners()
    }




    private fun setupListeners() {
        binding.textViewRegistrarse.setOnClickListener {
            startActivity(Intent(this, NumeroVertificacion::class.java))
        }

        binding.botonBack.setOnClickListener {
            finish()
        }

        binding.botonIniciarSesion.setOnClickListener {
            iniciarSesion()
        }

        binding.editTextNombreUsuario.afterTextChanged {
            validarCampos()
        }

        binding.editTextTelefono.afterTextChanged {
            validarCampos()
        }
    }

    private fun iniciarSesion() {
        val nombreUsuario = binding.editTextNombreUsuario.text.toString()
        if (validarCampos()) {
            val nombrePais = binding.countryCode.selectedCountryNameCode
            binding.countryCode.registerCarrierNumberEditText(binding.editTextTelefono)

            val formattedPhoneNumber = binding.editTextTelefono.text.toString().trim()
            val numeroTelefonoFull = binding.countryCode.formattedFullNumber

            FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("nombreUsuario", nombreUsuario)
                .whereEqualTo("telefono", numeroTelefonoFull).get().addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) {
                        val intent = Intent(this@InicioSesion, NumeroVertificacion::class.java)
                        intent.putExtra("phoneNumber", formattedPhoneNumber)
                        intent.putExtra("nombrePais", nombrePais)
                        intent.putExtra("isLogin", true)
                        startActivity(intent)
                    } else {
                        binding.editTextNombreUsuario.error = "Usuario no coincide o no está registrado"
                        binding.editTextTelefono.error = "Teléfono no coincide o no está registrado"
                    }
                }.addOnFailureListener { e ->
                    // Manejar errores de Firebase
                    Log.e("InicioSesion", "Error al iniciar sesión: $e")
                    // Mostrar un mensaje al usuario o registrar el error de alguna manera
                }
        }
    }

    private fun validarCampos(): Boolean {
        val nombreUsuario = binding.editTextNombreUsuario.text.toString()
        val numeroTelefono = binding.editTextTelefono.text.toString()

        val camposRellenos = nombreUsuario.isNotEmpty() && numeroTelefono.isNotEmpty()

        binding.editTextNombreUsuario.error = if (nombreUsuario.isEmpty()) "Este campo es requerido" else null
        binding.editTextTelefono.error = if (numeroTelefono.isEmpty()) "Este campo es requerido" else null
        binding.botonIniciarSesion.isEnabled = camposRellenos

        return camposRellenos
    }

    private fun EditText.afterTextChanged(afterTextChanged: (String) -> Unit) {
        this.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(editable: Editable?) {
                afterTextChanged.invoke(editable.toString())
            }

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) {}
        })
    }

}