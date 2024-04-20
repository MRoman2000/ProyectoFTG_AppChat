package com.paquete.proyectoftg_appchat.actividades

import android.os.Bundle
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.google.android.material.appbar.MaterialToolbar
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.download.AndroidDownloader


class FullScreenImageActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_full_screen_image)
        val downloader = AndroidDownloader(this)
        val imageUrl = intent.getStringExtra("imageUrl")
        val imageView: ImageView = findViewById(R.id.fullScreenImageView)
        val downloadButton: ImageView = findViewById(R.id.downloadIcon)
        val toolbar: MaterialToolbar = findViewById(R.id.materialToolbar)

        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        Glide.with(this).load(imageUrl).into(imageView)

        downloadButton.setOnClickListener {
            if (!imageUrl.isNullOrEmpty()) {
                downloader.downloadFile(imageUrl)
            } else {
                Toast.makeText(this, "No se puede descargar la imagen: URL de imagen no válida", Toast.LENGTH_SHORT).show()
            }
        }

        toolbar.setNavigationOnClickListener {
            finish()
        }
    }


    interface Downloader {
        fun downloadFile(url: String): Long
    }

}



