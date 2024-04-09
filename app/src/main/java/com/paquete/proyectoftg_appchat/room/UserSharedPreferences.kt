package com.paquete.proyectoftg_appchat.room

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.paquete.proyectoftg_appchat.model.DataUser


object UserSharedPreferences {
    private const val PREF_NAME = "user_pref"
    private const val KEY_USER_DATA = "user_data"

    private fun getSharedPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun saveUserData(context: Context, userData: DataUser) {
        val sharedPreferences = getSharedPreferences(context)
        val editor = sharedPreferences.edit()
        val gson = Gson()
        val userDataJson = gson.toJson(userData)
        editor.putString(KEY_USER_DATA, userDataJson)
        editor.apply()
    }

    fun getUserData(context: Context): DataUser? {
        val sharedPreferences = getSharedPreferences(context)
        val gson = Gson()
        val userDataJson = sharedPreferences.getString(KEY_USER_DATA, null)
        return gson.fromJson(userDataJson, DataUser::class.java)
    }
}