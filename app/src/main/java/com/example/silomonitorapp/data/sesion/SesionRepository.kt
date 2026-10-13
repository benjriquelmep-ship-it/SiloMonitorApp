package com.example.silomonitorapp.data.sesion

import android.content.Context
import com.example.silomonitorapp.domain.Usuario

/**
 * Recuerda quién inició sesión para no pedir el login cada vez que se abre la app.
 * Solo guarda el nombre de usuario, nunca la contraseña.
 */
class SesionRepository(context: Context) {

    // SharedPreferences: archivo pequeño clave-valor privado de la app.
    // Aquí se guarda solo el nombre de usuario conectado.
    private val preferencias = context.applicationContext
        .getSharedPreferences("sesion_silomonitor", Context.MODE_PRIVATE)

    // Lee la sesión guardada al abrir la app (null si nadie inició sesión).
    fun usuarioGuardado(): Usuario? =
        preferencias.getString(CLAVE_USUARIO, null)?.let { UsuariosDemo.buscar(it) }

    // Valida usuario y contraseña; si son correctos, guarda la sesión.
    fun iniciarSesion(usuario: String, clave: String): Usuario? {
        val autenticado = UsuariosDemo.autenticar(usuario, clave) ?: return null
        preferencias.edit().putString(CLAVE_USUARIO, autenticado.usuario).apply()
        return autenticado
    }

    // Borra la sesión guardada (botón salir).
    fun cerrarSesion() {
        preferencias.edit().remove(CLAVE_USUARIO).apply()
    }

    private companion object {
        const val CLAVE_USUARIO = "usuario"
    }
}
