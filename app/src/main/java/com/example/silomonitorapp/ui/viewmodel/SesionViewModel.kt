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

// VIEWMODEL de sesión: guarda quién está conectado y lo expone a la UI con StateFlow.
// MainActivity lo observa: si 'usuario' es null muestra el Login, si no, la app.
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

    // Cierra la sesión: al quedar 'usuario' en null, la UI vuelve sola al Login.
    fun cerrarSesion() {
        repository.cerrarSesion()
        _usuario.value = null
    }

    // Factory: le dice a Android cómo crear este ViewModel (necesita el Application).
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
