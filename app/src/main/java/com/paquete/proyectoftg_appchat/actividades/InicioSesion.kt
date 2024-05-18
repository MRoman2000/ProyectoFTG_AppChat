package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.addTextChangedListener
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.databinding.ActivityInicioSesionBinding
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils

class InicioSesion : AppCompatActivity() {

    private lateinit var binding: ActivityInicioSesionBinding
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityInicioSesionBinding.inflate(layoutInflater)
        setContentView(binding.root)
        enableEdgeToEdge()

        auth = FirebaseAuth.getInstance()
        setupListeners()

    }

    private fun setupListeners() {

        binding.textViewRegistrarse.setOnClickListener {
            startActivity(Intent(this, Registro::class.java))
        }

        setSupportActionBar(binding.materialToolbar)
        binding.materialToolbar.setNavigationIcon(com.google.android.material.R.drawable.ic_arrow_back_black_24)
        binding.materialToolbar.setNavigationOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        binding.editTextEmail.addTextChangedListener {
            actualizarEstadoBoton()
            binding.inputEmail.error = null
        }
        binding.passwordEditText.addTextChangedListener {
            actualizarEstadoBoton()
            binding.inputPassword.error = null
        }


        binding.textViewPassword.setOnClickListener {
            val email = binding.editTextEmail.text.toString()
            if (email.isNotEmpty()) {
                resetPassword(email)
            } else {
                binding.editTextEmail.error = "Rellena el campo de correo "
            }

        }
        binding.botonIniciarSesion.setOnClickListener {
            iniciarSesion()
        }
    }

    private fun resetPassword(email: String) {
        // Verificar si el correo electrónico proporcionado está registrado en el sistema
        FirebaseUtils.allusers()?.whereEqualTo("email", email)?.get()?.addOnSuccessListener { documents ->
            if (!documents.isEmpty) {
                // El correo electrónico está registrado en el sistema, enviar correo de restablecimiento
                auth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        // Envío de correo electrónico de restablecimiento exitoso
                        Toast.makeText(applicationContext,
                            "Se ha enviado un correo electrónico para restablecer tu contraseña",
                            Toast.LENGTH_SHORT).show()
                    } else {
                        // Error al enviar el correo electrónico de restablecimiento
                        Toast.makeText(applicationContext,
                            "Error al enviar correo electrónico para restablecer contraseña: ${task.exception?.message}",
                            Toast.LENGTH_SHORT).show()
                    }
                }
            } else {
                // El correo electrónico no está registrado en el sistema
                Toast.makeText(applicationContext, "La dirección de correo electrónico no está registrada", Toast.LENGTH_SHORT).show()
            }
        }?.addOnFailureListener { e ->
            // Error al consultar la base de datos
            Toast.makeText(applicationContext, "Error al verificar el correo electrónico: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }


    private fun iniciarSesion() {
        val password = binding.passwordEditText.text.toString()
        val email = binding.editTextEmail.text.toString()

        // Verificar si los campos de correo electrónico y contraseña no están vacíos
        if (email.isNotEmpty() && password.isNotEmpty()) {
            // Eliminar errores anteriores
            binding.inputEmail.error = null
            binding.inputPassword.error = null
            binding.botonIniciarSesion.isEnabled = password.isNotEmpty() && email.isNotEmpty()
            // Iniciar sesión con correo electrónico y contraseña
            auth.signInWithEmailAndPassword(email, password).addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // El inicio de sesión fue exitoso, redirigir al usuario a la pantalla principal
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                    finish()
                    Toast.makeText(applicationContext, "Sesión iniciada correctamente", Toast.LENGTH_SHORT).show()
                } else {
                    // Mostrar errores si el inicio de sesión falla
                    binding.inputEmail.error = "El correo electrónico no coincide"
                    binding.inputPassword.error = "La contraseña no coincide"
                    binding.botonIniciarSesion.isEnabled = false
                }
            }
        } else {
            // Mostrar un mensaje de error si los campos están vacíos
            Toast.makeText(this, "Por favor, completa todos los campos.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun actualizarEstadoBoton() {
        val password = binding.passwordEditText.text.toString()
        val email = binding.editTextEmail.text.toString()
        // Habilitar el botón si ambos campos están rellenos
        binding.botonIniciarSesion.isEnabled = password.isNotEmpty() && email.isNotEmpty()
    }

}
