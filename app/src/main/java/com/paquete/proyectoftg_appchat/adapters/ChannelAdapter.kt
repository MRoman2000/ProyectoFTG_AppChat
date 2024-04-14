package com.paquete.proyectoftg_appchat.adapters


import android.os.Bundle
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

    inner class ChannelViewHolder(private val binding: ViewholderChannelBinding) : RecyclerView.ViewHolder(binding.root) {
        private val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())

        fun bind(chatRoom: ChatRoom) {
            val lastMessageText = if (chatRoom.lastMessageSenderId == FirebaseAuth.getInstance().currentUser?.uid) {
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
                    insertElemento(elemento)

                    observeUserData(otherUserModel.uid)

                    itemView.setOnClickListener {
                        val profileFragment = MessageFragment()
                        val bundle = Bundle().apply {
                            putParcelable("userData", elemento)
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


        private fun insertElemento(elemento: DataUser) {
            elementosViewModel.insertar(elemento)
        }

        private fun bindClickListener(channelId: String?, recipientId: String?, nombreRemitente: String?) {
            itemView.setOnClickListener {
                val messageFragment = MessageFragment.newInstance(channelId, recipientId.toString(), nombreRemitente.toString())
                Utils.navigateToFragment(activity = binding.root.context as FragmentActivity, fragment = messageFragment)
            }
        }

        private fun observeUserData(uid: String?) {
            uid?.let {
                elementosViewModel.obtenerDatosUsuario(uid).observe(itemView.context as FragmentActivity) { usuario ->
                    usuario?.let {
                        binding.textViewUser.text = it.nombreCompleto
                        Glide.with(binding.root.context).load(it.imageUrl).apply(RequestOptions.circleCropTransform())
                            .into(binding.imagenPerfil.imageView)
                        return@observe
                    }
                }
            }
        }


        /*   private fun loadUserName(numero: String, chatRoom: ChatRoom, itemView: View) {
               CoroutineScope(Dispatchers.Main).launch {
                   val contactos = withContext(Dispatchers.IO) { Contactos.obtenerContactos(context) }
                   val usuario = contactos?.find { it.numero == numero }
                   //     binding.textViewUser.text = usuario?.nombre ?: numero

                   val channelId = chatRoom.chatroomId
                   val participants = chatRoom.userIds
                   val recipientId = participants?.firstOrNull { it != FirebaseAuth.getInstance().currentUser?.uid }
                   val nombreRemitente = usuario?.nombre.toString()

                   itemView.setOnClickListener {
                       val messageFragment = MessageFragment.newInstance(channelId, recipientId.toString(), nombreRemitente)
                       Utils.navigateToFragment(activity = context as FragmentActivity, fragment = messageFragment)
                   }
               }
           } */
    }
}
