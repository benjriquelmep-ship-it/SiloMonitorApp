package com.example.silomonitorapp

import android.app.Application
import com.google.firebase.FirebaseApp

// Clase Application: se crea antes que cualquier pantalla.
// Inicializa Firebase una sola vez para que la sincronización con Firestore funcione.
class SiloApp : Application() {
    override fun onCreate() {
        super.onCreate()
        FirebaseApp.initializeApp(this)
    }
}