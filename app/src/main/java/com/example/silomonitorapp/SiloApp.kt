package com.example.silomonitorapp

import android.app.Application
import com.google.firebase.FirebaseApp

class SiloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}