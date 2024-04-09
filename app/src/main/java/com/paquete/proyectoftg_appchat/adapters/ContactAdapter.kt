package com.paquete.proyectoftg_appchat.adapters

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.paquete.proyectoftg_appchat.R
import com.paquete.proyectoftg_appchat.databinding.ViewholderContactosBinding
import com.paquete.proyectoftg_appchat.fragmentos.MostrarDatosContactoFragment

import com.paquete.proyectoftg_appchat.model.Contactos
import com.paquete.proyectoftg_appchat.room.ElementosViewModel

class ContactAdapter(private val context: Context,
    private val elementosViewModel: ElementosViewModel) : RecyclerView.Adapter<ContactAdapter.ElementoViewHolder>() {
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


        /*     if (contacto.foto.isNullOrEmpty()) { // Cargar una imagen predeterminada si no hay foto
                 holder.binding.imagenPerfil.imageView.setImageResource(R.drawable.ic_person)
             } else { // Cargar la foto del contacto si está disponible
                 Glide.with(holder.itemView).load(contacto.foto).apply(RequestOptions.circleCropTransform())
                     .into(holder.binding.imagenPerfil.imageView)
             } */


     /*   holder.itemView.setOnClickListener {
            contacto?.let {
                elementosViewModel.seleccionarContacto(contacto)
                val mostrar = MostrarPerfilFragment()
                val transaction = (context as FragmentActivity).supportFragmentManager.beginTransaction()
                transaction.replace(R.id.main_frame_layout, mostrar)
                transaction.addToBackStack(null)
                transaction.commit()
            }
        } */
    }

    override fun getItemCount(): Int {
        return contactos.size ?: 0
    }


    inner class ElementoViewHolder(private val binding: ViewholderContactosBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(contacto: Contactos) {
            binding.nombre.text = contacto.nombre ?: "Sin nombre"
            binding.textViewNumero.text = contacto.numero ?: "Sin número"

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

