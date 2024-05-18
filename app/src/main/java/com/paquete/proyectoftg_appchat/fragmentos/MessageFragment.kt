package com.paquete.proyectoftg_appchat.fragmentos


import android.Manifest
import android.annotation.SuppressLint
import android.content.ContentValues.TAG
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.google.gson.Gson
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.adapters.MessageAdapter
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.FragmentMessageBinding
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.MediaPickerBottomSheetFragment
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.ProfileFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.model.Mensaje
import com.paquete.proyectoftg_appchat.notifications.NotificationData
import com.paquete.proyectoftg_appchat.notifications.PushNotification
import com.paquete.proyectoftg_appchat.notifications.RetrofitInstance
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MessageFragment : Fragment(), MediaPickerBottomSheetFragment.MediaPickerListener {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val db = FirebaseFirestore.getInstance()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Mensaje>
    private lateinit var firestore: FirebaseFirestore
    private lateinit var linearLayoutManager: LinearLayoutManager
    private lateinit var url: Uri
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ViewModel::class.java]
    }
    private lateinit var takePictureLauncher: ActivityResultLauncher<Uri>
    private lateinit var pickMediaLauncher: ActivityResultLauncher<PickVisualMediaRequest>
    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
        if (isGranted) {
            takePictureLauncher.launch(url)
        } else {
            // El usuario denegó el permiso de cámara, muestra un mensaje o realiza alguna acción apropiada
            Toast.makeText(requireContext(), "Permiso de cámara denegado", Toast.LENGTH_SHORT).show()
        }
    }


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE
        (requireActivity() as AppCompatActivity).supportActionBar?.apply {
            //       setDisplayHomeAsUpEnabled(true)
            hide()
        }
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val userData = arguments?.getParcelable<DataUser>("dataUser")
        val channelId = arguments?.getString("channelId")
        val recipientId = arguments?.getString("recipientId")

        url = createImageUri()
        val senderUid = FirebaseAuth.getInstance().currentUser?.uid

        firestore = FirebaseFirestore.getInstance()
        messageList = ArrayList()
        messageAdapter = MessageAdapter(requireContext(), messageList)
        pickMediaLauncher = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                sendMessage(senderUid.toString(), recipientId.toString(), uri)
            }
        }
        takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
            // Aquí manejas el resultado de la captura de la imagen, por ejemplo, mostrando la imagen capturada
            if (success) {
                sendMessage(senderUid.toString(), recipientId.toString(), url)
            } else {

            }
        }

        binding.btnBack.setOnClickListener {
            requireActivity().onBackPressedDispatcher.onBackPressed()
        }

        binding.imageProfile.setOnClickListener {
            if (isAdded) {
                val profileFragment = ProfileFragment()
                val bundle = Bundle()
                bundle.putParcelable("dataUser", userData)
                profileFragment.arguments = bundle
                Utils.navigateToFragment(requireActivity(), profileFragment)
            } else {
                // Manejar el caso cuando el fragmento no está adjunto a la actividad
                Log.e("MessageFragment", "Fragment not attached to activity")
            }
        }

        elementosViewModel.obtenerDatosUsuarioActual(FirebaseUtils.getCurrentUserId())
        binding.nameUserTextView.text = userData?.nombreCompleto ?: ""
        Glide.with(requireContext()).load(userData?.imageUrl).apply(RequestOptions.circleCropTransform()).into(binding.imageProfile)
        userData?.uid?.let { fetchUserStatusRealTime(it, binding.statusTextView) }
        recuperarMensajes(channelId.toString())

        binding.btnSendPhoto.setOnClickListener {
            val bottomSheetFragment = MediaPickerBottomSheetFragment()
            bottomSheetFragment.setListener(this)
            bottomSheetFragment.show(parentFragmentManager, bottomSheetFragment.tag)
        }

        binding.btnSendMessage.setOnClickListener {
            sendMessage(senderUid.toString(), recipientId.toString())
        }


    }

    private fun createImageUri(): Uri {
        // Obtenemos el directorio de archivos de la aplicación
        val storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)

        // Creamos un archivo temporal en el directorio de imágenes
        val imageFile = File.createTempFile("temp_image", /* prefijo */
            ".png", /* sufijo */
            storageDir /* directorio */)

        // Devolvemos el URI del archivo temporal utilizando FileProvider
        return FileProvider.getUriForFile(requireContext(), "com.paquete.proyectoftg_appchat.fileprovider", imageFile)
        // Crear un nombre único para el archivo de imagen
        /*   val timeStamp: String = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val storageDir: File? = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
            val imageFileName = "JPEG_${timeStamp}_"
            val imageFile = File.createTempFile(imageFileName, ".jpg", storageDir)

            // Obtener la Uri del archivo de imagen
            return FileProvider.getUriForFile(requireContext(), "com.paquete.proyectoftg_appchat.fileprovider", imageFile) */
    }

    private fun formatDate(date: Date): String {
        val calendar = Calendar.getInstance()
        val today = calendar.get(Calendar.DAY_OF_YEAR)
        calendar.time = date
        val connectionDay = calendar.get(Calendar.DAY_OF_YEAR)

        return if (today == connectionDay) {
            // Hoy
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            "hoy a las ${formatter.format(date)}"
        } else if (today - connectionDay == 1) {
            // Ayer
            val formatter = SimpleDateFormat("HH:mm", Locale.getDefault())
            "ayer a las ${formatter.format(date)}"
        } else {
            // Otra fecha
            val formatter = SimpleDateFormat("dd/MM HH:mm", Locale.getDefault())
            formatter.format(date)
        }
    }

    fun recuperarMensajes(channelId: String) {
        if (!isAdded) {
            return
        }
        linearLayoutManager = LinearLayoutManager(requireContext())
        firestore.collection("chats").document(channelId).collection("mensajes").orderBy("marcaDeTiempo", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val newMessages = snapshot.toObjects(Mensaje::class.java)
                    messageList.clear()
                    messageList.addAll(newMessages)
                    messageAdapter.notifyDataSetChanged()
                    linearLayoutManager.stackFromEnd = true
                    binding.recyclerViewMensajes.layoutManager = linearLayoutManager
                    binding.recyclerViewMensajes.adapter = messageAdapter
                    binding.recyclerViewMensajes.post {
                        binding.recyclerViewMensajes.scrollToPosition(messageList.size - 1)
                    }
                } else {
                    Log.d(TAG, "No messages")
                }
            }
    }


    fun sendMessage(senderUid: String, recipientId: String, imageUri: Uri? = null) {
        val mensaje = binding.editTextMessege.text.toString().trim()
        elementosViewModel.dataUser.observe(viewLifecycleOwner) { dataUser ->
            dataUser?.let { user ->
                val nombreCompleto = user.nombreCompleto

                FirebaseUtils.getFCMToken(recipientId).addOnSuccessListener { documentSnapshot ->
                    val userData = documentSnapshot.toObject(DataUser::class.java)
                    val recipientToken = userData?.fcmToken

                    if (mensaje.isNotEmpty() || imageUri != null) {
                        val currentTime = Timestamp.now()
                        if (imageUri != null) {
                            val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}")
                            storageRef.putFile(imageUri).addOnSuccessListener { taskSnapshot ->
                                storageRef.downloadUrl.addOnSuccessListener { uri ->
                                    val imageUrl = uri.toString()
                                    val messageObject = Mensaje(mensaje, senderUid, currentTime, imageUrl)
                                    sendMessageToFirestore(senderUid, recipientId, messageObject)
                                    sendNotification(recipientToken, nombreCompleto.toString(), "imagen", senderUid)
                                }.addOnFailureListener { e ->
                                    // Manejar errores al obtener la URL de la imagen
                                }
                            }.addOnFailureListener { e ->
                                // Manejar errores al cargar la imagen
                            }
                        } else {
                            val messageObject = Mensaje(mensaje, senderUid, currentTime)
                            sendMessageToFirestore(senderUid, recipientId, messageObject)
                            sendNotification(recipientToken, nombreCompleto.toString(), mensaje, senderUid)
                            binding.editTextMessege.text = null
                        }
                    } else {
                        Toast.makeText(context, "No puedes enviar un mensaje vacío", Toast.LENGTH_SHORT).show()
                    }
                }.addOnFailureListener { exception ->
                    // Manejar errores al obtener el token FCM
                }
            }
        }
    }

    private fun sendNotification(recipientToken: String?, nombreCompleto: String, message: String, senderUid: String) {
        recipientToken?.let {
            val notificationData = NotificationData(nombreCompleto, message, senderUid)
            val pushNotification = PushNotification(notificationData, recipientToken)
            sendNotification(pushNotification)
        } ?: run {
            // El destinatario no tiene un token FCM registrado
        }
    }


    private fun sendNotification(notification: PushNotification) = CoroutineScope(Dispatchers.IO).launch {
        try {
            val response = RetrofitInstance.api.postNotification(notification)
            if (response.isSuccessful) {
                Log.d(TAG, "Response: ${Gson().toJson(response)}")
            } else {
                Log.e(TAG, response.errorBody().toString())
            }
        } catch (e: Exception) {
            Log.e(TAG, e.toString())
        }
    }

    private fun sendMessageToFirestore(senderUid: String, recipientId: String, messageObject: Mensaje) {
        val chatroomId = generateChatroomId(senderUid, recipientId)
        val messageCollectionRef = firestore.collection("chats").document(chatroomId).collection("mensajes")
        messageCollectionRef.add(messageObject)
            .addOnSuccessListener { // Mensaje guardado exitosamente, ahora actualiza el último mensaje y la hora en el chat
                val chatRef = firestore.collection("chats").document(chatroomId)
                val lastMessageContent = if (messageObject.urlImagen != null) {
                    // Si hay una URL de imagen, indicar que se envió una imagen
                    "Imagen"
                } else {
                    // Si no hay una URL de imagen, usar el contenido del mensaje
                    messageObject.mensaje ?: ""
                }
                val data = hashMapOf("userIds" to listOf(senderUid, recipientId),
                    "chatroomId" to chatroomId,
                    "lastMessage" to lastMessageContent,
                    "lastMessageSenderId" to senderUid,
                    "lastMessageTimestamp" to messageObject.marcaDeTiempo)

                chatRef.set(data, SetOptions.merge()).addOnSuccessListener { // Chat actualizado exitosamente
                    binding.editTextMessege.text = null
                    binding.recyclerViewMensajes.smoothScrollToPosition(messageList.size)
                }.addOnFailureListener { e -> // Manejar el fallo al actualizar el chat
                }
            }.addOnFailureListener { e -> // Manejar el fallo al enviar el mensaje
            }
    }


    @SuppressLint("SetTextI18n")
    private fun fetchUserStatusRealTime(userId: String, statusTextView: TextView) {
        db.collection("usuarios").document(userId).addSnapshotListener { snapshot, exception ->
            if (exception != null) {
                Log.e(TAG, "Error al obtener el estado del usuario en tiempo real", exception)
                return@addSnapshotListener
            }

            if (snapshot != null && snapshot.exists()) {
                val estado = snapshot.getString("estado")

                // Obtener la hora de la última conexión
                val lastConnection = snapshot.getTimestamp("ultimaConexion")

                // Formatear la hora y el día de la última conexión (si está disponible)
                val lastConnectionDate = lastConnection?.toDate()
                val lastConnectionText = lastConnectionDate?.let { formatDate(it) } ?: "Desconocido"

                // Actualizar la interfaz de usuario según el estado y la última conexión del usuario
                if (estado == "online") {
                    statusTextView.text = "Online"
                } else {
                    statusTextView.text = "Última vez: $lastConnectionText"
                }
            } else {
                Log.d(TAG, "El documento del usuario $userId no existe")
            }
        }
    }


    private fun generateChatroomId(sender: String, receiver: String): String {
        val participants = listOf(sender, receiver).sorted()
        return participants.joinToString("")
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE

    }


    override fun onDestroyView() {
        super.onDestroyView()
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        //    val materialToolbar = requireActivity().findViewById<MaterialToolbar>(R.id.materialToolbarMain)
        //   materialToolbar.removeViewAt(materialToolbar.childCount - 1)
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.VISIBLE

    }

    private fun requestCameraPermission(photoUri: Uri) {
        try {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                // El permiso de cámara fue otorgado, puedes proceder a capturar la foto
                takePictureLauncher.launch(photoUri)
            } else {
                // El permiso de cámara no ha sido otorgado, solicitar permiso
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun onCameraOptionClicked() {
        // Solicitar permiso para acceder a la cámara y capturar la imagen
        requestCameraPermission(url)
    }

    override fun onGalleryOptionClicked() {
        pickMediaLauncher.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
    }
}
