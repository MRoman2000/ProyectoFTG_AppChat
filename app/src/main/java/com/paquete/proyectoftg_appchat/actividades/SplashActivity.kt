package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.SharePreference


class SplashActivity : AppCompatActivity() {


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)
        val themeManager = SharePreference(this)

        themeManager.applySavedTheme()
        enableEdgeToEdge()
        Handler().postDelayed({
            verificarAutenticacion()
        }, 1000) // Espera 1 segundo antes de verificar la autenticación
    }
    

    private fun verificarAutenticacion() {
        val currentUser = FirebaseUtils.getCurrentUserId()
        if (currentUser != null) {
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


}