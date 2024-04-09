package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.content.res.ColorStateList
import android.graphics.Color
import android.os.Bundle
import android.os.CountDownTimer
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.android.gms.tasks.OnCompleteListener
import com.google.firebase.FirebaseException
import com.google.firebase.auth.AuthResult
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import com.google.firebase.auth.PhoneAuthProvider.ForceResendingToken
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.databinding.ActivityNumeroVertificacionBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Timer
import java.util.TimerTask
import java.util.concurrent.TimeUnit

class NumeroVertificacion : AppCompatActivity() {

    private lateinit var binding: ActivityNumeroVertificacionBinding
    private var storedVerificationId: String? = null
    private lateinit var auth: FirebaseAuth
    private var resendToken: ForceResendingToken? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNumeroVertificacionBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)
        auth = FirebaseAuth.getInstance()


        setupViews()


    }

    fun setupViews() {
        binding.botonComprobarCodigo.setOnClickListener { checkVerificationCode() }
        binding.botonEnviarCode.setOnClickListener { sendOTP() }
        binding.botonBack.setOnClickListener { finish() }

        val phoneNumber = intent.getStringExtra("phoneNumber")
        val nombrePais = intent.getStringExtra("nombrePais")

        phoneNumber?.let {
            if (nombrePais != null && it.isNotEmpty() && nombrePais.isNotEmpty()) {
                binding.editTextTelefono.setText(phoneNumber)
                binding.countryCode.setCountryForNameCode(nombrePais)
            } else {
                Log.d("NumeroVertificacion", "phoneNumber o nombre del país es nulo o vacío")
            }
        }
    }

    private fun checkVerificationCode() {
        val entradaOTP = binding.editTextCodigoVertificacion.text.toString()
        val codigoVertificacion = storedVerificationId

        if (entradaOTP.isNotEmpty() && codigoVertificacion != null) {
            if (entradaOTP.length == 6) {
                val phoneAuthCredential = PhoneAuthProvider.getCredential(codigoVertificacion, entradaOTP)
                val isLogin = intent.getBooleanExtra("isLogin", false)
                signInWithPhoneAuthCredential(phoneAuthCredential, isLogin)
            } else {
                binding.editTextCodigoVertificacion.error = "Por favor ingresa los 6 números"
            }
        } else {
            Toast.makeText(applicationContext, "Por favor ingresa el código de verificación", Toast.LENGTH_SHORT).show()
        }
    }


    private fun startResendTimer() {
        object : CountDownTimer(20000, 1000) {
            override fun onTick(millisUntilFinished: Long) {
                val segundosRestantes = millisUntilFinished / 1000
                binding.resentOTP.text = "Reenviar código de verificación en $segundosRestantes segundos"
            }

            override fun onFinish() {
                binding.resentOTP.isEnabled = true
                binding.resentOTP.text = "Reenviar código de verificación"
            }
        }.start()
    }

    fun sendOTP() {
        val isLogin = intent.getBooleanExtra("isLogin", false)
        binding.countryCode.registerCarrierNumberEditText(binding.editTextTelefono)

        if (binding.countryCode.isValidFullNumber) {

            val numeroCompleto = binding.countryCode.formattedFullNumber
            val db = FirebaseFirestore.getInstance()
            if (!isLogin) { // Solo comprobar si no es un inicio de sesión
                db.collection("usuarios").whereEqualTo("telefono", numeroCompleto).get().addOnSuccessListener { querySnapshot ->
                    if (!querySnapshot.isEmpty) { // El número de teléfono ya está registrado
                        binding.editTextTelefono.error = "Este número ya está registrado"
                        return@addOnSuccessListener // Salir de la función sin iniciar la verificación
                    } else { // Continuar con el proceso de verificación
                        CoroutineScope(Dispatchers.Main).launch {
                            withContext(Dispatchers.IO) {
                                startPhoneNumberVerification(numeroCompleto, isLogin)
                            }
                        }
                    }
                }.addOnFailureListener { e ->
                    Toast.makeText(applicationContext, "Error al verificar número de teléfono: $e", Toast.LENGTH_SHORT).show()
                }
            } else { // Continuar con el proceso de verificación directamente para iniciar sesión
                startPhoneNumberVerification(numeroCompleto, isLogin)
            }
        } else {
            binding.editTextTelefono.error = "El número no puede estar vacío"
        }
    }

    private fun startPhoneNumberVerification(phoneNumber: String, isLogin: Boolean) {
        val opcion = PhoneAuthOptions.newBuilder(auth).setPhoneNumber(phoneNumber).setTimeout(20L, TimeUnit.SECONDS).setActivity(this)
            .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                    signInWithPhoneAuthCredential(credential, isLogin)
                }

                override fun onVerificationFailed(exception: FirebaseException) {
                    Log.e("VerificationFailed", "Error OTP verificación fallada", exception)
                    Toast.makeText(applicationContext, "Error OTP verificación fallada", Toast.LENGTH_SHORT).show()
                    progressBar(false)
                }

                override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                    storedVerificationId = verificationId
                    resendToken = token
                    Toast.makeText(applicationContext, "Código de verificación enviado", Toast.LENGTH_SHORT).show()
                    startResendTimer()
                    binding.inputCodigoVertificacion.visibility = View.VISIBLE
                    binding.botonComprobarCodigo.visibility = View.VISIBLE
                    progressBar(true) // Iniciar un temporizador para ocultar el botón de enviar durante 20 segundos
                    Timer().schedule(object : TimerTask() {
                        override fun run() {
                            runOnUiThread {
                                progressBar(false) // Oculta el botón de enviar después de 20 segundos
                            }
                        }
                    }, 20000)
                }
            }).build()

        // Iniciar la verificación del número de teléfono
        PhoneAuthProvider.verifyPhoneNumber(opcion)
    }

    private fun signInWithPhoneAuthCredential(credential: PhoneAuthCredential, isLogin: Boolean) {
        auth.signInWithCredential(credential).addOnCompleteListener(OnCompleteListener<AuthResult?> { task ->
            if (task.isSuccessful) {
                if (isLogin) {
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)
                } else {
                    val numeroTelefono = binding.countryCode.formattedFullNumber
                    val intent = Intent(this, Registro::class.java)
                    intent.putExtra("numeroTelefono", numeroTelefono)
                    startActivity(intent)
                }
            } else {
                binding.inputCodigoVertificacion.error = "El código de verificación es incorrecto"
                binding.inputCodigoVertificacion.setErrorTextColor(ColorStateList.valueOf(Color.RED))
            }
        })
    }


    fun progressBar(inProgress: Boolean) {
        if (inProgress) {
            binding.progressBar.visibility = View.VISIBLE
            binding.botonEnviarCode.isEnabled = false
            binding.resentOTP.visibility = View.VISIBLE
        } else {
            binding.progressBar.visibility = View.GONE
            binding.botonEnviarCode.isEnabled = true
            binding.resentOTP.visibility = View.GONE
        }
    }
}