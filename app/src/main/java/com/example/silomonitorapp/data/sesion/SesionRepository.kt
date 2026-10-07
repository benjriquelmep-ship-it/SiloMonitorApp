package com.example.silomonitorapp.data.sesion

import android.content.Context
import com.example.silomonitorapp.domain.Usuario

/**
 * Recuerda quién inició sesión para no pedir el login cada vez que se abre la app.
 * Solo guarda el nombre de usuario, nunca la contraseña.
 */
class SesionRepository(context: Context) {

    private val preferencias = context.applicationContext
        .getSharedPreferences("sesion_silomonitor", Context.MODE_PRIVATE)

    fun usuarioGuardado(): Usuario? =
        preferencias.getString(CLAVE_USUARIO, null)?.let { UsuariosDemo.buscar(it) }

    fun iniciarSesion(usuario: String, clave: String): Usuario? {
        val autenticado = UsuariosDemo.autenticar(usuario, clave) ?: return null
        preferencias.edit().putString(CLAVE_USUARIO, autenticado.usuario).apply()
        return autenticado
    }

    fun cerrarSesion() {
        preferencias.edit().remove(CLAVE_USUARIO).apply()
    }

    private companion object {
        const val CLAVE_USUARIO = "usuario"
    }
}
