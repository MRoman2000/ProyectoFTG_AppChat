package com.paquete.proyectoftg_appchat.actividades

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.firebaseutil.FirebaseAuthHelper

@SuppressLint("CustomSplashScreen")
class SplashActivity : AppCompatActivity() {
    private val firebaseAuthHelper = FirebaseAuthHelper(FirebaseAuth.getInstance())
    private val PREFS_KEY_THEME = "theme_preference"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        applySavedTheme()
        Handler().postDelayed({
            verificarAutenticacion()
        }, 1000) // Espera 1 segundo antes de verificar la autenticación
    }

    private fun applySavedTheme() {
        val savedThemeMode = getThemeMode()
        AppCompatDelegate.setDefaultNightMode(savedThemeMode)
    }

    private fun getThemeMode(): Int {
        val sharedPreferences = getSharedPreferences("app_prefs", Context.MODE_PRIVATE)
        return sharedPreferences.getInt(PREFS_KEY_THEME, AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM)
    }

    private fun verificarAutenticacion() {
        val currentUser = firebaseAuthHelper.getCurrentUserId()
        if (currentUser != null) {
            val firebase = FirebaseFirestore.getInstance()
            firebase.collection("usuarios").whereEqualTo("uid", currentUser).get().addOnSuccessListener { documents ->
                if (!documents.isEmpty) {
                    val intent = Intent(this@SplashActivity, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                } else {
                    val intent = Intent(this@SplashActivity, WelcomeActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    startActivity(intent)
                }
                finish()
            }
        } else {
            startActivity(Intent(this@SplashActivity, WelcomeActivity::class.java))
            finish()
        }
    }

}