package com.example.silomonitorapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.example.silomonitorapp.data.sesion.SesionRepository
import com.example.silomonitorapp.domain.Usuario
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SesionViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SesionRepository(application)

    // Se restaura al abrir la app: si ya había sesión, se entra directo
    private val _usuario = MutableStateFlow(repository.usuarioGuardado())
    val usuario: StateFlow<Usuario?> = _usuario.asStateFlow()

    /** Devuelve el mensaje de error para mostrar en el login, o null si entró. */
    fun iniciarSesion(usuario: String, clave: String): String? {
        if (usuario.isBlank() || clave.isBlank()) return "Ingresa tu usuario y contraseña."
        val autenticado = repository.iniciarSesion(usuario, clave)
            ?: return "Usuario o contraseña incorrectos."
        _usuario.value = autenticado
        return null
    }

    fun cerrarSesion() {
        repository.cerrarSesion()
        _usuario.value = null
    }

    class Factory(private val application: Application) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SesionViewModel::class.java)) {
                return SesionViewModel(application) as T
            }
            throw IllegalArgumentException("Clase ViewModel desconocida")
        }
    }
}
