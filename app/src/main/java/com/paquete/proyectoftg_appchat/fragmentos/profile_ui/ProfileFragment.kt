package com.paquete.proyectoftg_appchat.fragmentos.profile_ui

import android.content.ContentValues
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.ktx.storage
import com.paquete.proyectoftg_appchat.data.AppDatabase
import com.paquete.proyectoftg_appchat.databinding.FragmentProfileBinding
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class ProfileFragment : Fragment() {
    private var _binding: FragmentProfileBinding? = null
    private val binding get() = _binding!!
    private val REQUEST_IMAGE_CAPTURE = 1
    private val REQUEST_PICK_IMAGE = 2

    private val storage = Firebase.storage
    private val storageRef = storage.reference


    val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { url ->
        if (url != null) {
            saveImageUrlToFirebase(url.toString())
        }
    }
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentProfileBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            title = "Informacion de Perfil"
            setDisplayHomeAsUpEnabled(true)
        }
        val usuarioActual = FirebaseAuth.getInstance().currentUser?.uid

        val userData = arguments?.getParcelable<DataUser>("userData")
        userData?.let {
            // Utiliza los datos del usuario aquí
            loadImageFromUrl(it.imageUrl.toString(), binding.imagenPerfil)
            binding.editTextNombreCompleto.setText(it.nombreCompleto ?: "")
            binding.editTextNombreUsuario.setText(it.nombreUsuario ?: "")
            binding.editTextTelefono.setText(it.telefono ?: "")
        }


        binding.editFoto.setOnClickListener {
            showOptionsDialog()

        }


    }

    private fun showOptionsDialog() {
        val options = arrayOf<CharSequence>("Tomar foto", "Elegir de la galería", "Cancelar")
        MaterialAlertDialogBuilder(requireActivity()).setTitle("Elige una opción").setItems(options) { _, item ->
            when (options[item]) {

                "Elegir de la galería" -> pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                // Agrega otros casos aquí si es necesario
            }
        }.show()
    }


    private fun saveImageUrlToFirebase(imageUrl: String) {
        val usuarioActual = FirebaseUtils.getCurrentUserId()

        val imagesRef = storageRef.child("profile_user/$usuarioActual")

        // Crear una referencia con la URL de la imagen
        val url = Uri.parse(imageUrl)

        // Subir la URL al Firebase Storage
        imagesRef.putFile(url).addOnSuccessListener { taskSnapshot ->
            // Obtener la URL de descarga de la imagen
            imagesRef.downloadUrl.addOnSuccessListener { uri ->
                val downloadUrl = uri.toString()
                loadImageFromUrl(downloadUrl, binding.imagenPerfil)

                // Actualizar la URL de la imagen en Firestore
                saveImageUrlToFirestore(downloadUrl)

                // Actualizar la URL de la imagen en la base de datos Room
                val usuarioActual = FirebaseAuth.getInstance().currentUser
                usuarioActual?.uid?.let { userId ->
                    CoroutineScope(Dispatchers.Main).launch {
                        val userDao = AppDatabase.getDatabase(requireContext()).datosDao()
                        userDao.updateUserImageUrl(userId, downloadUrl)
                    }
                }

                Log.d("Foto", downloadUrl)
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


    override fun onDestroyView() {
        super.onDestroyView() // Limpiar el binding al destruir la vista
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

    companion object {
        private const val REQUEST_CAMERA_PERMISSION = 101
    }
}