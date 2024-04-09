package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.paquete.proyectoftg_appchat.databinding.FragmentProfileBinding
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import java.io.ByteArrayOutputStream


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 2

    var imagenPickLauncher: ActivityResultLauncher<Intent>? = null

    private val storage = Firebase.storage
    private val storageRef = storage.reference
    var selectedImageUrl: Uri? = null

    private lateinit var elementosViewModel: ElementosViewModel

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)




        elementosViewModel = ViewModelProvider(requireActivity()).get(ElementosViewModel::class.java)
        elementosViewModel.elementos.observe(viewLifecycleOwner) { elementos ->
            // Filtrar los elementos para obtener solo los del usuario actual
            val usuarioActual = FirebaseAuth.getInstance().currentUser
            val uidUsuarioActual = usuarioActual?.uid
            val elementosUsuarioActual = elementos.filter { it.uid == uidUsuarioActual }
            elementosUsuarioActual.firstOrNull()?.let { primerElemento ->
                Log.d("PRIMER_ELEMENTO_DEBUG", "Primer elemento del usuario actual: $primerElemento")
                loadImageFromUrl(primerElemento.imageUrl.toString(), binding.imagenPerfil)
                binding.editTextNombreCompleto.setText(primerElemento.nombreCompleto ?: "")
                binding.editTextNombreUsuario.setText(primerElemento.nombreUsuario ?: "")
                binding.editTextTelefono.setText(primerElemento.telefono ?: "")
            }
        }


        binding.editFoto.setOnClickListener {
            showOptionsDialog()
        }


    }

    private fun showOptionsDialog() {
        val options = arrayOf<CharSequence>("Tomar foto", "Elegir de la galería", "Cancelar")
        MaterialAlertDialogBuilder(requireActivity()).setTitle("Elige una opción").setItems(options) { _, item ->
            when (options[item]) {
                "Tomar foto" -> dispatchTakePictureIntent()
                "Elegir de la galería" -> dispatchPickImageIntent()
            }
        }.show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            // Si no tienes permisos, los solicitas
            requestCameraPermission()
        } else {
            // Si ya tienes permisos, puedes proceder con la acción que requiere permisos
            dispatchTakePictureIntent()
        }
    }


    private fun dispatchTakePictureIntent() {
        if (hasCameraPermission()) {
            val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
            takePictureIntent.resolveActivity(requireActivity().packageManager)?.let {
                takePicture.launch(takePictureIntent)
            }
        } else {
            requestCameraPermission()
        }
    }

    private fun hasCameraPermission(): Boolean {
        return ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestCameraPermission() {
        requestPermissions(arrayOf(Manifest.permission.CAMERA), REQUEST_CAMERA_PERMISSION)
    }

    private val takePicture = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val data: Intent? = result.data
            val imageBitmap = data?.extras?.get("data") as Bitmap
            saveImageToFirebase(imageBitmap)
        } else {
            Toast.makeText(requireContext(), "La captura de imagen fue cancelada", Toast.LENGTH_SHORT).show()
        }
    }

    private fun dispatchPickImageIntent() {
        val pickImageIntent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        pickImageIntent.type = "image/*"
        startActivityForResult(pickImageIntent, REQUEST_PICK_IMAGE)
    }

    private fun saveImageToFirebase(bitmap: Bitmap) {
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos)
        val data = baos.toByteArray()
        val usuarioActual = FirebaseAuth.getInstance().currentUser?.uid
        val imagesRef = storageRef.child("profile_user/$usuarioActual")
        val uploadTask = imagesRef.putBytes(data)
        uploadTask.addOnSuccessListener { taskSnapshot ->
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                loadImageFromUrl(downloadUrl, binding.imagenPerfil)
                saveImageUrlToFirestore(downloadUrl)
                Log.d("Foto", downloadUrl.toString())
                Toast.makeText(requireContext(), "Imagen subida exitosamente", Toast.LENGTH_SHORT).show()
            }.addOnFailureListener { e ->
                Toast.makeText(requireContext(), "Error al obtener la URL de descarga: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }.addOnFailureListener { e ->
            Toast.makeText(requireContext(), "Error al subir la imagen: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveImageUrlToFirestore(imageUrl: String) {
        val usuarioActual = FirebaseAuth.getInstance().currentUser
        usuarioActual?.uid?.let { userId ->
            val db = FirebaseFirestore.getInstance()
            val userRef = db.collection("usuarios").document(userId)
            userRef.update("imageUrl", imageUrl).addOnSuccessListener {
                Log.d("Error foto", "URL de la imagen guardada en Firestore")
            }.addOnFailureListener { e ->
                Log.w(ContentValues.TAG, "Error al guardar la URL de la imagen en Firestore", e)
            }
        }
    }

    private fun loadImageFromUrl(imageUrl: String, imageView: ImageView) {
        Glide.with(this).load(imageUrl).apply(RequestOptions.circleCropTransform()).into(imageView)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            when (requestCode) {
                REQUEST_PICK_IMAGE -> {
                    val imageUri = data?.data
                    val imageStream = requireContext().contentResolver.openInputStream(imageUri!!)
                    val imageBitmap = BitmapFactory.decodeStream(imageStream)
                    saveImageToFirebase(imageBitmap)
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }
}
