package com.paquete.proyectoftg_appchat.adapters


import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.google.firebase.auth.FirebaseAuth
import com.paquete.proyectoftg_appchat.data.AppDatabase
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
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Locale

class ChannelAdapter(val context: Context,
    val messageList: ArrayList<ChatRoom>,
    private val elementosViewModel: ElementosViewModel) : RecyclerView.Adapter<ChannelAdapter.ChannelViewHolder>() {
    private lateinit var db: AppDatabase
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

            db = AppDatabase.getDatabase(context)
            var otherUserModel: DataUser? = null

            FirebaseUtils.getOtrosUser(chatRoom.userIds)?.get()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    otherUserModel = task.result.toObject(DataUser::class.java)
                    val numero = otherUserModel?.telefono
                    binding.textViewDate.text = sdf.format(chatRoom.lastMessageTimestamp!!.toDate())
                    //    val foto =     otherUserModel?.let { loadProfileImage(it.uid) }
                    //  numero?.let { loadUserName(it, chatRoom, binding.root) }
                    CoroutineScope(Dispatchers.Main).launch {
                        val contactos = withContext(Dispatchers.IO) { Contactos.obtenerContactos(context) }
                        val usuario = contactos?.find { it.numero == numero }
                        //     binding.textViewUser.text = usuario?.nombre ?: numero
                        val channelId = chatRoom.chatroomId
                        val participants = chatRoom.userIds
                        val recipientId = participants?.firstOrNull { it != FirebaseAuth.getInstance().currentUser?.uid }
                        val nombreRemitente = usuario?.nombre ?: numero
                        GlobalScope.launch {
                            db.datosDao().insertar(DataUser(otherUserModel!!.uid,
                                otherUserModel!!.email,
                                "",
                                nombreRemitente,
                                otherUserModel!!.nombreUsuario,
                                usuario?.numero,
                                otherUserModel!!.imageUrl))
                        }
                        itemView.setOnClickListener {
                            val messageFragment = MessageFragment.newInstance(channelId, recipientId.toString(), nombreRemitente.toString())
                            Utils.navigateToFragment(activity = context as FragmentActivity, fragment = messageFragment)
                        }
                        elementosViewModel.obtenerDatosUsuario(otherUserModel?.uid).observe((context as FragmentActivity)) { usuario ->
                            usuario?.let {
                                binding.textViewUser.text = usuario.nombreCompleto
                                Glide.with(context).load(usuario.imageUrl).apply(RequestOptions.circleCropTransform())
                                    .into(binding.imagenPerfil.imageView)
                            }
                        }
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
