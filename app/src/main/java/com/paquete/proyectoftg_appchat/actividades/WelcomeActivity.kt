package com.paquete.proyectoftg_appchat.actividades

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.paquete.proyectoftg_appchat.databinding.ActivityWelcomeBinding

class WelcomeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityWelcomeBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityWelcomeBinding.inflate(layoutInflater)
        val view = binding.root
        setContentView(view)

        binding.botonRegistrarse.setOnClickListener {
            startActivity(Intent(this, NumeroVertificacion::class.java))
        }

        binding.botonIniciarSesion.setOnClickListener {
            startActivity(Intent(this, InicioSesion::class.java))
        }
    }
}