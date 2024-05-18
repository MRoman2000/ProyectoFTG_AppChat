package com.paquete.proyectoftg_appchat.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.data.ViewModel
import com.paquete.proyectoftg_appchat.databinding.ViewholderContactosBinding
import com.paquete.proyectoftg_appchat.fragmentos.MostrarDatosContactoFragment
import com.paquete.proyectoftg_appchat.model.Contactos

class ContactAdapter(private val context: Context,
    private val elementosViewModel: ViewModel) : RecyclerView.Adapter<ContactAdapter.ElementoViewHolder>() {
    private var contactos: List<Contactos> = emptyList()

    fun establecerListaContactos(contactos: List<Contactos>) {
        this.contactos = contactos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ElementoViewHolder {
        val binding = ViewholderContactosBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return ElementoViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ElementoViewHolder, position: Int) {
        val contacto = contactos[position]
        holder.bind(contacto)
    }

    override fun getItemCount(): Int {
        return contactos.size
    }

    inner class ElementoViewHolder(private val binding: ViewholderContactosBinding) : RecyclerView.ViewHolder(binding.root) {
        @SuppressLint("SetTextI18n")
        fun bind(contacto: Contactos) {

            binding.nombre.text = contacto.nombreCompleto ?: "Sin nombre"
            binding.textViewNombreUsuario.text = "@${contacto.nombreUsuario}"
            Glide.with(binding.root.context).load(contacto.imageUrl).apply(RequestOptions.circleCropTransform())
                .into(binding.imagenPerfil.imageView)

            itemView.setOnClickListener {
                elementosViewModel.seleccionarContacto(contacto)
                val mostrar = MostrarDatosContactoFragment()
                val transaction = (context as FragmentActivity).supportFragmentManager.beginTransaction()
                transaction.replace(R.id.main_frame_layout, mostrar)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        }

    }

}