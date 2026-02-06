package com.example.music.util

import android.content.Context
import android.content.SharedPreferences

object SharedPref {

    private lateinit var prefs: SharedPreferences
    private const val PREF_NAME = "music_prefs"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    object Keys {
        const val IS_LOOPING = "isLooping"
    }

    fun saveBoolean(key: String, default: Boolean = false) {
        prefs.edit().putBoolean(key, default)
    }

    fun getBoolean(key: String): Boolean {
        return prefs.getBoolean(key, false)
    }

}