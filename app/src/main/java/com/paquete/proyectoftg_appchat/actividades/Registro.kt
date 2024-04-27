package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.datepicker.CalendarConstraints
import com.google.android.material.datepicker.MaterialDatePicker
import com.paquete.proyectoftg_appchat.databinding.ActivityRegistroBinding
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

class Registro : AppCompatActivity() {

    private lateinit var binding: ActivityRegistroBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityRegistroBinding.inflate(layoutInflater)

        setContentView(binding.root)
        setupListeners()

    }

    private fun setupListeners() {
        with(binding) {
            editTextFechaNacimiento.setOnClickListener {
                mostrarSelectorFecha()
            }

            botonBack.setOnClickListener {
                finish()
            }

            botonRegistrarse.setOnClickListener {
                registrarUsuario()
            }

            editTextNombreUsuario.afterTextChanged {
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
        val email = binding.editTextEmail.text.toString()
        val numeroTelefono = intent.getStringExtra("numeroTelefono")
        val fechaNacimiento = binding.editTextFechaNacimiento.text.toString()
        val currentUser = FirebaseUtils.getCurrentUserId()

        // Verificar la fecha de nacimiento
        val formatter = SimpleDateFormat("dd-MM-yyyy", Locale.getDefault())
        val fechaNacimientoDate = formatter.parse(fechaNacimiento)
        val calendarNacimiento = Calendar.getInstance().apply { time = fechaNacimientoDate }
        val calendarHoy = Calendar.getInstance()
        calendarNacimiento.add(Calendar.YEAR, 12) // Agregar 12 años a la fecha de nacimiento

        if (calendarNacimiento.before(calendarHoy)) {
            // El usuario tiene al menos 12 años, permitir el registro
            crearUsuario(nombreUsuario, nombreCompleto, email, numeroTelefono.toString(), fechaNacimiento, currentUser.toString())
        } else {
            // El usuario no tiene la edad mínima requerida
            Toast.makeText(this, "Debes tener al menos 12 años para registrarte.", Toast.LENGTH_SHORT).show()
        }
    }


    private fun crearUsuario(nombreUsuario: String,
        nombreCompleto: String,
        email: String,
        numeroTelefono: String,
        fechaNacimiento: String,
        currentUser: String) {
        if (nombreUsuario.isNotEmpty() && nombreUsuario.isNotEmpty() && email.isNotEmpty() && fechaNacimiento.isNotEmpty()) {
            FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("nombreUsuario", nombreUsuario).get()
                .addOnSuccessListener { nombreUsuarioSnapshot ->
                    if (!nombreUsuarioSnapshot.isEmpty) {
                        binding.inputNombreUsuario.error
                        binding.editTextNombreUsuario.error = "Este nombre de usuario ya está en uso"
                    } else {
                        FirebaseUtils.getFirestoreInstance().collection("usuarios").whereEqualTo("email", email).get()
                            .addOnSuccessListener { emailSnapshot ->
                                if (!emailSnapshot.isEmpty) {
                                    binding.editTextEmail.error = "Este correo electrónico ya está en uso"
                                } else {
                                    val usuario = hashMapOf("nombreCompleto" to nombreCompleto,
                                        "nombreUsuario" to nombreUsuario,
                                        "email" to email,
                                        "telefono" to numeroTelefono,
                                        "uid" to currentUser,
                                        "imageUrl" to "")

                                    FirebaseUtils.getFirestoreInstance().collection("usuarios").document(currentUser).set(usuario)
                                        .addOnSuccessListener {
                                            //    Toast.makeText(applicationContext, "Documento agregado con ID: $usuario}", Toast.LENGTH_SHORT)
                                            //        .show()
                                            startActivity(Intent(this@Registro, MainActivity::class.java))
                                        }.addOnFailureListener { e ->
                                            Toast.makeText(applicationContext, "Error al agregar documento: $e", Toast.LENGTH_SHORT).show()
                                        }
                                }
                            }.addOnFailureListener { e ->
                                Toast.makeText(applicationContext, "Error al verificar correo electrónico: $e", Toast.LENGTH_SHORT).show()
                            }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(applicationContext, "Error al verificar nombre de usuario: $e", Toast.LENGTH_SHORT).show()
                }
        }
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



