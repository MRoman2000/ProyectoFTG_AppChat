package com.paquete.proyectoftg_appchat.utils

import android.content.Context
import android.net.Uri
import android.widget.ImageView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.paquete.proyectoftg_appchat.R


class Utils {
    companion object {
        fun navigateToFragment(activity: FragmentActivity, fragment: Fragment) {
            try {
                val transaction = activity.supportFragmentManager.beginTransaction()
                transaction.replace(R.id.main_frame_layout, fragment)
                transaction.addToBackStack(null)
                transaction.commit()
            } catch (e: ClassCastException) {
                e.printStackTrace()
            }
        }

        fun setProfilePic(context: Context?, imageUri: Uri?, imageView: ImageView?) {
            Glide.with(context!!).load(imageUri).apply(RequestOptions.circleCropTransform()).into(imageView!!)
        }

        fun showMessage(context: Context?, mensaje: String) {
            Toast.makeText(context, mensaje, Toast.LENGTH_SHORT).show()
        }
    }


}