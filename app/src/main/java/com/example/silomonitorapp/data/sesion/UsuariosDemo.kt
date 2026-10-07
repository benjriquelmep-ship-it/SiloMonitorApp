package com.example.silomonitorapp.data.sesion

import com.example.silomonitorapp.domain.Rol
import com.example.silomonitorapp.domain.Usuario
import java.security.MessageDigest

/**
 * Usuarios de prueba (uno por rol) mientras no exista el backend con autenticación real.
 * Las contraseñas se guardan como hash SHA-256, nunca en texto plano.
 * Las credenciales de prueba están documentadas en el README.
 */
object UsuariosDemo {

    private data class Cuenta(val usuario: Usuario, val hashClave: String)

    private val cuentas = listOf(
        Cuenta(
            Usuario("operario", "Operario El Paico", Rol.OPERARIO, granjasPermitidas = setOf("Granja El Paico")),
            "e644bc78fa4565c929a1d5755135fdf702e586797994b5466e6f4a949aa649fb"
        ),
        Cuenta(
            Usuario("supervisor", "Supervisor Zona Poniente", Rol.SUPERVISOR, granjasPermitidas = setOf("Granja El Paico", "Granja Pomaire")),
            "47015d5d902fb5fb66ce2e6b5d85131323198b8aa31ab2facd91dd08e95f1e3b"
        ),
        Cuenta(
            Usuario("jefatura", "Jefatura Producción", Rol.JEFATURA),
            "9cd83d3c0933ccbf33a6f8e9e00a76532a2a34211dc7e6889047ef8a144ec202"
        ),
        Cuenta(
            Usuario("admin", "Administrador", Rol.ADMIN),
            "04445e6487736590d1ef50186b414e737e0164683cbbec64e00e73c000fd3bef"
        ),
    )

    /** Devuelve el usuario si el nombre y la contraseña coinciden, o null si no. */
    fun autenticar(usuario: String, clave: String): Usuario? {
        val cuenta = cuentas.firstOrNull { it.usuario.usuario == usuario.trim().lowercase() } ?: return null
        return cuenta.usuario.takeIf { sha256(clave) == cuenta.hashClave }
    }

    fun buscar(usuario: String): Usuario? = cuentas.firstOrNull { it.usuario.usuario == usuario }?.usuario

    internal fun sha256(texto: String): String =
        MessageDigest.getInstance("SHA-256").digest(texto.toByteArray())
            .joinToString("") { "%02x".format(it) }
}
