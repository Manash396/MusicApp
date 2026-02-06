package com.example.music

import android.app.Application
import com.example.music.util.SharedPref

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()
        SharedPref.init(this)
    }
}