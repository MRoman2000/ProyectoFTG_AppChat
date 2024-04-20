package com.paquete.proyectoftg_appchat.fragmentos


import android.content.ContentValues.TAG
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.android.material.appbar.MaterialToolbar
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions
import com.google.firebase.storage.FirebaseStorage
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.adapters.MessageAdapter
import com.paquete.proyectoftg_appchat.databinding.FragmentMessageBinding
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.ProfileFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.model.Message
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.Utils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }
    private val db = FirebaseFirestore.getInstance()
    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>
    private lateinit var mDbRef: FirebaseFirestore
    private var recipientId: String? = null
    private lateinit var linearLayoutManager: LinearLayoutManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE
        return binding.root
    }

    @RequiresApi(Build.VERSION_CODES.TIRAMISU)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(true)
        // Infla tu vista personalizada en la barra de herramientas
        val materialToolbar = requireActivity().findViewById<MaterialToolbar>(R.id.materialToolbar)

        // Infla tu vista personalizada en el MaterialToolbar
        val inflater = LayoutInflater.from(requireContext())
        val customToolbarView = inflater.inflate(R.layout.custom_toolbar_view, materialToolbar, false)

        // Añade tu vista personalizada al MaterialToolbar
        materialToolbar.addView(customToolbarView)

        // Obtiene las referencias a los elementos en tu vista personalizada
        val profileImage: ImageView = customToolbarView.findViewById(R.id.image_profile)
        val usernameTextView: TextView = customToolbarView.findViewById(R.id.name_user_textView)
        val statusTextView: TextView = customToolbarView.findViewById(R.id.status_textView)

        // Actualiza la información del usuario
        profileImage.setImageResource(R.drawable.ic_person)


        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        val pickMedia = registerForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
            if (uri != null) {
                // Aquí puedes llamar a tu función sendMessage pasando la URI de la imagen
                sendMessage(senderUid.toString(), recipientId.toString(), uri)
            }
        }

        mDbRef = FirebaseFirestore.getInstance()
        messageList = ArrayList()
        messageAdapter = MessageAdapter(requireContext(), messageList)

        val userData = arguments?.getParcelable("userData", DataUser::class.java)

        val channelId = arguments?.getString("channelId")
        recipientId = arguments?.getString("recipientISd")


        // Llama al método para cargar la lista de usuarios
        elementosViewModel.cargarUsuarios()
        profileImage.setOnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putParcelable("userData", userData)
            profileFragment.arguments = bundle
            Utils.navigateToFragment(requireActivity(), profileFragment)
        }

        userData?.let {
            usernameTextView.text = userData.nombreCompleto
            Glide.with(requireContext()).load(userData.imageUrl).apply(RequestOptions.circleCropTransform()).into(profileImage)
            // Recupera y muestra el estado del usuario
            fetchUserStatusRealTime(userData.uid, statusTextView)
        }
        recuperarMensajes(channelId.toString())



        binding.btnSendPhoto.setOnClickListener {
            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
        }

        // En el método onActivityResult

        binding.btnSendMessage.setOnClickListener {
            sendMessage(senderUid.toString(), recipientId.toString())
        }


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
        mDbRef.collection("chats").document(channelId).collection("messages").orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, e ->
                if (e != null) {
                    Log.w(TAG, "Listen failed", e)
                    return@addSnapshotListener
                }

                if (snapshot != null && !snapshot.isEmpty) {
                    val newMessages = snapshot.toObjects(Message::class.java)
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
        val message = binding.editTextMessege.text.toString().trim()
        if (message.isNotEmpty() || imageUri != null) {
            val currentTime = Timestamp.now()

            if (imageUri != null) {
                // Si hay una imagen, subirla a Firebase Storage
                val storageRef = FirebaseStorage.getInstance().reference.child("images/${UUID.randomUUID()}")
                storageRef.putFile(imageUri).addOnSuccessListener { taskSnapshot ->
                    // Obtener la URL de la imagen cargada
                    storageRef.downloadUrl.addOnSuccessListener { uri ->
                        val imageUrl = uri.toString()
                        val messageObject = Message(message, senderUid, currentTime, imageUrl)
                        sendMessageToFirestore(senderUid, recipientId, messageObject)
                    }.addOnFailureListener { e ->
                        // Manejar errores al obtener la URL de la imagen
                    }
                }.addOnFailureListener { e ->
                    // Manejar errores al cargar la imagen
                }
            } else {
                val messageObject = Message(message, senderUid, currentTime)
                sendMessageToFirestore(senderUid, recipientId, messageObject)
            }
        } else {
            Toast.makeText(context, "No puedes enviar un mensaje vacío", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendMessageToFirestore(senderUid: String, recipientId: String, messageObject: Message) {
        // Guardar el mensaje en la colección de mensajes del chat
        val chatroomId = generateChatroomId(senderUid, recipientId)
        val messageCollectionRef = mDbRef.collection("chats").document(chatroomId).collection("messages")
        messageCollectionRef.add(messageObject)
            .addOnSuccessListener { // Mensaje guardado exitosamente, ahora actualiza el último mensaje y la hora en el chat
                val chatRef = mDbRef.collection("chats").document(chatroomId)
                val lastMessageContent = if (messageObject.imageUrl != null) {
                    // Si hay una URL de imagen, indicar que se envió una imagen
                    "Imagen"
                } else {
                    // Si no hay una URL de imagen, usar el contenido del mensaje
                    messageObject.message ?: ""
                }

                val data = hashMapOf("userIds" to listOf(senderUid, recipientId),
                    "chatroomId" to chatroomId,
                    "lastMessage" to lastMessageContent,
                    "lastMessageSenderId" to senderUid,
                    "lastMessageTimestamp" to messageObject.timestamp,
                    "lastMessageImageUrl" to messageObject.imageUrl)

                chatRef.set(data, SetOptions.merge()).addOnSuccessListener { // Chat actualizado exitosamente
                    binding.editTextMessege.text = null
                    binding.recyclerViewMensajes.smoothScrollToPosition(messageList.size)
                }.addOnFailureListener { e -> // Manejar el fallo al actualizar el chat
                }
            }.addOnFailureListener { e -> // Manejar el fallo al enviar el mensaje
            }
    }

    fun downloadImage(imageUrl: String) {
        val storageRef = FirebaseStorage.getInstance().getReferenceFromUrl(imageUrl)

        // Crear un archivo temporal para almacenar la imagen descargada
        val localFile = File.createTempFile("image", "jpg")

        storageRef.getFile(localFile).addOnSuccessListener {
            // La imagen se descargó exitosamente, ahora puedes guardarla o mostrarla en tu aplicación
            // Por ejemplo, puedes mostrarla en un ImageView o guardarla en el almacenamiento local
            val bitmap = BitmapFactory.decodeFile(localFile.absolutePath)
            // Aquí puedes hacer lo que quieras con la imagen descargada, como mostrarla en un ImageView
        }.addOnFailureListener { exception ->
            // Ocurrió un error al descargar la imagen, maneja el error según sea necesario
            Log.e(TAG, "Error downloading image", exception)
            // Por ejemplo, muestra un mensaje de error al usuario
            Toast.makeText(context, "Error al descargar la imagen", Toast.LENGTH_SHORT).show()
        }
    }


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
        val materialToolbar = requireActivity().findViewById<MaterialToolbar>(R.id.materialToolbar)
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.VISIBLE
        materialToolbar.removeViewAt(materialToolbar.childCount - 1)
    }

}
