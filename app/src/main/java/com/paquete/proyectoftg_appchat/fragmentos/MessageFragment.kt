package com.paquete.proyectoftg_appchat.fragmentos


import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
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
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.adapters.MessageAdapter
import com.paquete.proyectoftg_appchat.databinding.FragmentMessageBinding
import com.paquete.proyectoftg_appchat.fragmentos.profile_ui.ProfileFragment
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.model.Message
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.Utils
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

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
    private lateinit var linearLayoutManager: LinearLayoutManager


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val activity = requireActivity() as AppCompatActivity
        activity.supportActionBar?.hide()

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseFirestore.getInstance()
        messageList = ArrayList()
        messageAdapter = MessageAdapter(requireContext(), messageList)

        val userData = arguments?.getParcelable<DataUser>("userData")
        val channelId = arguments?.getString("channelId")
        val recipientId = arguments?.getString("recipientId")


        // Llama al método para cargar la lista de usuarios
        elementosViewModel.cargarUsuarios()
        binding.imageProfile.setOnClickListener {
            val profileFragment = ProfileFragment()
            val bundle = Bundle()
            bundle.putParcelable("userData", userData)
            profileFragment.arguments = bundle
            Utils.navigateToFragment(requireActivity(), profileFragment)
        }

        userData?.let {
            binding.nameUser.text = userData.nombreCompleto
            Glide.with(requireContext()).load(userData.imageUrl).apply(RequestOptions.circleCropTransform()).into(binding.imageProfile)
            // Recupera y muestra el estado del usuario
            fetchUserStatusRealTime(userData.uid)
        }
        recuperarMensajes(channelId.toString())

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE

        val sendButton = binding.btnSendMessage

        sendButton.setOnClickListener {
            sendMessage(senderUid.toString(), recipientId.toString())
        }
    }

    private fun fetchUserStatusRealTime(userId: String) {
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
                    binding.online.text = "Online"
                } else {
                    binding.online.text = "Última vez: $lastConnectionText"
                }
            } else {
                Log.d(TAG, "El documento del usuario $userId no existe")
            }
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
                    _binding?.let { safeBinding ->
                        safeBinding.recyclerViewMensajes.layoutManager = linearLayoutManager
                    }
                    _binding?.let { safeBinding ->
                        safeBinding.recyclerViewMensajes.adapter = messageAdapter
                    }

                    binding.recyclerViewMensajes.post {
                        binding.recyclerViewMensajes.scrollToPosition(messageList.size - 1)
                    }
                } else {
                    Log.d(TAG, "No messages")
                }
            }

    }

    private fun generateChatroomId(sender: String, receiver: String): String {
        val participants = listOf(sender, receiver).sorted()
        return participants.joinToString("")
    }


    fun sendMessage(senderUid: String, recipientId: String) {
        val message = binding.editTextMessege.text.toString().trim()
        if (message.isNotEmpty()) {
            val currentTime = Timestamp.now()
            val messageObject = Message(message, senderUid, currentTime)

            // Guardar el mensaje en la colección de mensajes del chat
            val chatroomId = generateChatroomId(senderUid, recipientId)
            val messageCollectionRef = mDbRef.collection("chats").document(chatroomId).collection("messages")
            messageCollectionRef.add(messageObject)
                .addOnSuccessListener { // Mensaje guardado exitosamente, ahora actualiza el último mensaje y la hora en el chat
                    val chatRef = mDbRef.collection("chats").document(chatroomId)
                    val data = hashMapOf("userIds" to listOf(senderUid, recipientId),
                        "chatroomId" to chatroomId,
                        "lastMessage" to message,
                        "lastMessageSenderId" to senderUid,
                        "lastMessageTimestamp" to currentTime)
                    chatRef.set(data,
                        SetOptions.merge()) // Utiliza merge para actualizar solo los campos especificados sin sobrescribir otros campos
                        .addOnSuccessListener { // Chat actualizado exitosamente
                            binding.editTextMessege.text = null
                            binding.recyclerViewMensajes.smoothScrollToPosition(messageList.size)
                        }.addOnFailureListener { e -> // Manejar el fallo al actualizar el chat
                        }
                }.addOnFailureListener { e -> // Manejar el fallo al enviar el mensaje
                }
        } else {
            Toast.makeText(context, "No puedes enviar un mensaje vacío", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onResume() {
        super.onResume()
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.VISIBLE
        (requireActivity() as AppCompatActivity).supportActionBar?.show()
        (requireActivity() as AppCompatActivity).supportActionBar?.setDisplayHomeAsUpEnabled(false)
    }

}
