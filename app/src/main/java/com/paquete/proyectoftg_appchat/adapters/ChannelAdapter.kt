package com.paquete.proyectoftg_appchat.adapters


import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.databinding.ViewholderChannelBinding
import com.paquete.proyectoftg_appchat.fragmentos.MessageFragment
import com.paquete.proyectoftg_appchat.model.ChatRoom
import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.model.DataUser
import com.paquete.proyectoftg_appchat.room.ElementosViewModel
import com.paquete.proyectoftg_appchat.utils.FirebaseUtils
import com.paquete.proyectoftg_appchat.utils.Utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ChannelAdapter(private val messageList: ArrayList<ChatRoom>,
    private val elementosViewModel: ElementosViewModel) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {

    private var userList: List<DataUser> = emptyList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ChannelViewHolder {
        val binding = ViewholderChannelBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ChannelViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ChannelViewHolder, position: Int) {
        val channel = messageList[position]
        holder.bind(channel)
    }

    override fun getItemCount(): Int {
        return messageList.size
    }


    fun actualizarUsuarios(usuarios: List<DataUser>) {
        userList = usuarios
        notifyDataSetChanged() // Notifica al adaptador de que los datos han cambiado
        Log.d("UserList", "Tamaño de userList: ${userList.size}") // Agrega un registro para verificar el tamaño de userList
    }


    inner class ChannelViewHolder(private val binding: ViewholderChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
        val currentUserUid = FirebaseAuth.getInstance().currentUser?.uid
        fun bind(chatRoom: ChatRoom) {

            val lastMessageText = if (chatRoom.lastMessageSenderId == currentUserUid) {
                "Tú: ${chatRoom.lastMessage ?: ""}"
            } else {
                chatRoom.lastMessage ?: ""
            }
            binding.textViewLastMessage.text = lastMessageText


            FirebaseUtils.getOtrosUser(chatRoom.userIds)?.get()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val otherUserModel = task.result.toObject(DataUser::class.java)
                    processOtherUser(chatRoom, otherUserModel)
                }
            }
            mostrarDatos(chatRoom)

        }

        private fun processOtherUser(chatRoom: ChatRoom, otherUserModel: DataUser?) {

            val numero = otherUserModel?.telefono
            binding.textViewDate.text = sdf.format(chatRoom.lastMessageTimestamp!!.toDate())

            CoroutineScope(Dispatchers.Main).launch {
                val contactos = withContext(Dispatchers.IO) {
                    Contactos.obtenerContactos(binding.root.context)
                }
                // Verificar si el número está en la lista de contactos
                val usuario = contactos?.find { it.numero == numero }

                // Si el usuario está en la lista de contactos, mostrar su nombre, de lo contrario, mostrar el número de teléfono
                val nombreRemitente = usuario?.nombre ?: otherUserModel?.telefono ?: numero

                // Verificar si se obtuvieron datos del usuario de Firebase
                if (otherUserModel != null) {
                    val channelId = chatRoom.chatroomId
                    val participants = chatRoom.userIds
                    val recipientId = participants?.firstOrNull { it != FirebaseAuth.getInstance().currentUser?.uid }
                    val elemento = DataUser(uid = otherUserModel.uid,
                        email = otherUserModel.email,
                        nombreCompleto = nombreRemitente,
                        nombreUsuario = otherUserModel.nombreUsuario,
                        telefono = otherUserModel.telefono,
                        imageUrl = otherUserModel.imageUrl)
                    elementosViewModel.insertar(elemento)

                    binding.textViewUser.text = nombreRemitente
                    Glide.with(binding.root.context).load(otherUserModel.imageUrl).apply(RequestOptions.circleCropTransform())
                        .into(binding.imagenPerfil.imageView)

                    itemView.setOnClickListener {
                        val profileFragment = MessageFragment()
                        val bundle = Bundle().apply {
                            putParcelable("dataUser", otherUserModel)
                            putString("channelId", channelId)
                            putString("recipientId", recipientId)
                            putString("nombreRemitente", nombreRemitente)
                        }
                        profileFragment.arguments = bundle
                        Utils.navigateToFragment(itemView.context as FragmentActivity, profileFragment)
                    }
                }
            }
        }

        fun mostrarDatos(chatRoom: ChatRoom) {

            val otherUserId = chatRoom.userIds?.find { it != currentUserUid }
            Log.d("ChannelAdapter", "otherUserId: $otherUserId")
            otherUserId?.let { userId ->
                // Intentamos encontrar al otro usuario en la lista local
                val otherUser = userList.find { it.uid == userId }
                Log.d("ChannelAdapter", "otherUser: $otherUser")
                otherUser?.let { user ->
                    // Mostramos los datos del usuario en la interfaz de usuario
                    binding.textViewUser.text = user.nombreCompleto
                    Glide.with(binding.root.context).load(user.imageUrl).apply(RequestOptions.circleCropTransform())
                        .into(binding.imagenPerfil.imageView)
                }
            }

        }

    }


}