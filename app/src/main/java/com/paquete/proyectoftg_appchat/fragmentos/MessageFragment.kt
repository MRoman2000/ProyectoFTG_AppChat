package com.paquete.proyectoftg_appchat.fragmentos


import android.content.ContentValues.TAG
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
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
import com.paquete.proyectoftg_appchat.model.Message
import com.paquete.proyectoftg_appchat.room.ElementosViewModel

class MessageFragment : Fragment() {

    private var _binding: FragmentMessageBinding? = null
    private val binding get() = _binding!!
    private val elementosViewModel by lazy {
        ViewModelProvider(requireActivity())[ElementosViewModel::class.java]
    }
    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    private lateinit var messageAdapter: MessageAdapter
    private lateinit var messageList: ArrayList<Message>

    private lateinit var mDbRef: FirebaseFirestore
    private lateinit var linearLayoutManager: LinearLayoutManager
    var receiverRoom: String? = null
    var senderRoom: String? = null

    companion object {
        fun newInstance(channelId: String?, recipientId: String, nombreRemitente: String): MessageFragment {
            val fragment = MessageFragment()
            val args = Bundle().apply {
                putString("channelId", channelId)
                putString("recipientId", recipientId)
                putString("nombreRemitente", nombreRemitente)
            }
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        _binding = FragmentMessageBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val senderUid = FirebaseAuth.getInstance().currentUser?.uid
        mDbRef = FirebaseFirestore.getInstance()
        messageList = ArrayList()
        messageAdapter = MessageAdapter(requireContext(), messageList)

        val channelId = arguments?.getString("channelId")
        val recipientId = arguments?.getString("recipientId")
        val nombreRemitente = arguments?.getString("nombreRemitente")
        val imageUrl = arguments?.getString("imagenRemitente")

        elementosViewModel.obtenerDatosUsuario(recipientId).observe(viewLifecycleOwner) { usuario ->
            usuario?.let {
                // Si los datos del usuario están disponibles, úsalos
                binding.nameUser.text = usuario.nombreCompleto
                Glide.with(requireContext()).load(usuario.imageUrl).apply(RequestOptions.circleCropTransform()).into(binding.imageProfile)
            }
        }
        recuperarMensajes(channelId.toString())

        val bottomNavigationView = requireActivity().findViewById<View>(R.id.bottom_navigation)
        bottomNavigationView.visibility = View.GONE

        val sendButton = binding.btnSendMessage

        sendButton.setOnClickListener {

            sendMessage(senderUid.toString(), recipientId.toString())

        }


        //      binding.nameUser.text = nombreRemitente
        //     Glide.with(requireContext()).load(imageUrl).apply(RequestOptions.circleCropTransform()).into(binding.imageProfile)

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
    }


}
