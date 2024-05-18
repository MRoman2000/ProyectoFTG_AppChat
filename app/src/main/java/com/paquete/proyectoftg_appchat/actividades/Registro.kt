package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.databinding.ActivityRegistroBinding
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Registro : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding
    private lateinit var auth: FirebaseAuth
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)
        lateinit var auth: FirebaseAuth
        enableEdgeToEdge()
        setContentView(binding.root)
        setupListeners()

    }

    private fun setupListeners() {
        with(binding) {
            editTextFechaNacimiento.setOnClickListener {
                mostrarSelectorFecha()
            }
            setSupportActionBar(binding.materialToolbarRegistro)

            binding.materialToolbarRegistro.setNavigationIcon(com.google.android.material.R.drawable.ic_arrow_back_black_24)
            binding.materialToolbarRegistro.setNavigationOnClickListener {
                onBackPressed()
            }

            botonRegistrarse.setOnClickListener {
                registrarUsuario()
            }

            editTextNombreUsuario.afterTextChanged {
                validarCampos()
            }
            editTextPassword.afterTextChanged {
                validarCampos()
            }

            editTextNombreCompleto.afterTextChanged {
                validarCampos()
            }
            editTextEmail.afterTextChanged {
                validarCampos()
            }
            editTextFechaNacimiento.afterTextChanged {
                validarCampos()
            }

        }
    }

    private fun registrarUsuario() {
        val nombreUsuario = binding.editTextNombreUsuario.text.toString()
        val nombreCompleto = binding.editTextNombreCompleto.text.toString()
        val password = binding.editTextPassword.text.toString()
        val email = binding.editTextEmail.text.toString()
        val confirmPassword = binding.editTextConfirmPassword.text.toString()
        val fechaNacimiento = binding.editTextFechaNacimiento.text.toString()
        // Verificar la fecha de nacimiento
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val fechaNacimientoDate = formatter.parse(fechaNacimiento)
        val calendarNacimiento = Calendar.getInstance().apply { time = fechaNacimientoDate }
        val calendarHoy = Calendar.getInstance()
        calendarNacimiento.add(Calendar.YEAR, 12) // Agregar 12 años a la fecha de nacimiento
        auth = FirebaseAuth.getInstance()
        if (calendarNacimiento.before(calendarHoy)) {
            // El usuario tiene al menos 12 años, permitir el registro
            crearUsuario(nombreUsuario, nombreCompleto, email, fechaNacimiento, password, confirmPassword)
        } else {
            // El usuario no tiene la edad mínima requerida
            Toast.makeText(this, "Debes tener al menos 12 años para registrarte.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun crearUsuario(nombreUsuario: String,
        nombreCompleto: String,
        email: String,
        fechaNacimiento: String,
        password: String,
        confirmPassword: String) {
        // Verificar que todos los campos estén completos
        if (nombreUsuario.isEmpty() || nombreCompleto.isEmpty() || email.isEmpty() || fechaNacimiento.isEmpty() || password.isEmpty()) {
            // Mostrar un mensaje de error si algún campo está vacío
            Toast.makeText(applicationContext, "Todos los campos son obligatorios", Toast.LENGTH_SHORT).show()
            return
        }

        // Verificar si las contraseñas coinciden
        if (password != confirmPassword) {
            binding.inputPassword.error = "Las contraseñas no coinciden"
            binding.inputPasswordVerti.error = "Las contraseñas no coinciden"
            // Mostrar un mensaje de error si las contraseñas no coinciden
            return
        }

        // Verificar si la contraseña es lo suficientemente segura
        if (!isStrongPassword(password)) {
            // Mostrar un mensaje de error si la contraseña es débil
            Toast.makeText(applicationContext, "La contraseña es débil, debe tener al menos 6 dígitos", Toast.LENGTH_SHORT).show()
            return
        }
        // Verificar si el nombre de usuario ya está en uso en Firestore
        FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("nombreUsuario", nombreUsuario).get()
            .addOnCompleteListener { userTask ->
                if (userTask.isSuccessful) {
                    val documents = userTask.result
                    if (documents != null && !documents.isEmpty) {
                        // Mostrar un mensaje de error si el nombre de usuario ya está en uso
                        Toast.makeText(applicationContext, "Este nombre de usuario ya está en uso", Toast.LENGTH_SHORT).show()

                    } else {
                        // Crear el usuario con correo electrónico y contraseña en Firebase Authentication
                        auth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { createUserTask ->
                            if (createUserTask.isSuccessful) {
                                // El usuario se ha creado exitosamente
                                val currentUser = auth.currentUser
                                currentUser?.let { user ->
                                    // Crear el documento del usuario en Firestore
                                    val usuario = hashMapOf("nombreCompleto" to nombreCompleto,
                                        "nombreUsuario" to nombreUsuario,
                                        "email" to email,
                                        "telefono" to "",
                                        "uid" to user.uid,
                                        "imageUrl" to "")
                                    // Agregar el documento del usuario a Firestore
                                    FirebaseUtils.getFirestoreInstance().collection("usuarios").document(user.uid).set(usuario)
                                        .addOnSuccessListener {
                                            // Mostrar un mensaje de éxito
                                            Toast.makeText(applicationContext, "El usuario se creó correctamente", Toast.LENGTH_SHORT)
                                                .show()
                                            // Redirigir al usuario a la pantalla principal
                                            startActivity(Intent(this@Registro, MainActivity::class.java))
                                            finish()
                                        }.addOnFailureListener { e ->
                                            // Mostrar un mensaje de error si falla la creación del documento
                                            Toast.makeText(applicationContext, "Error al agregar documento: $e", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            } else {
                                binding.editTextEmail.error = "El correo electrónico ya está en uso"
                                binding.botonRegistrarse.isEnabled = false
                            }
                        }
                    }
                } else {
                    // Mostrar un mensaje de error si falla la verificación del nombre de usuario en Firestore
                    Toast.makeText(applicationContext,
                        "Error al verificar nombre de usuario: ${userTask.exception?.message}",
                        Toast.LENGTH_SHORT).show()
                }
            }
    }


    private fun isStrongPassword(password: String): Boolean {
        // Validar la fortaleza de la contraseña
        return password.length >= 6 // Ejemplo simple: la contraseña debe tener al menos 6 caracteres
    }

    private fun validarCampos() {
        with(binding) {
            val nombreUsuario = editTextNombreUsuario.text.toString()
            val nombreCompleto = editTextNombreCompleto.text.toString()
            val fechaNacimiento = editTextFechaNacimiento.text.toString()
            val email = editTextEmail.text.toString()
            editTextNombreUsuario.error = if (nombreUsuario.isEmpty()) "Este campo es requerido" else null
            editTextNombreCompleto.error = if (nombreCompleto.isEmpty()) "Este campo es requerido" else null
            editTextFechaNacimiento.error = if (fechaNacimiento.isEmpty()) "Este campo es requerido" else null
            editTextEmail.error = if (email.isEmpty()) "Este campo es requerido" else null

            botonRegistrarse.isEnabled =
                nombreUsuario.isNotEmpty() && nombreCompleto.isNotEmpty() && fechaNacimiento.isNotEmpty() && email.isNotEmpty()
        }
    }

    private fun mostrarSelectorFecha() {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"))
        calendar.set(1940, Calendar.JANUARY, 1)
        val minDate = calendar.timeInMillis
        val maxDate = Calendar.getInstance().timeInMillis
        val builder = MaterialDatePicker.Builder.datePicker()
        builder.setTitleText("Selecciona una fecha")
        builder.setCalendarConstraints(CalendarConstraints.Builder().setStart(minDate).setEnd(maxDate).build())
        val picker = builder.build()
        picker.addOnPositiveButtonClickListener { selection ->
            val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
            val formattedDate = formatter.format(Date(selection))
            binding.editTextFechaNacimiento.setText(formattedDate)
        }
        picker.show(supportFragmentManager, picker.toString())
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



