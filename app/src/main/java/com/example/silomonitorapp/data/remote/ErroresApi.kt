package com.example.silomonitorapp.data.remote

import com.google.gson.Gson
import java.io.IOException
import retrofit2.HttpException

// Traduce los errores de red a mensajes que se muestran al usuario.
//  - HttpException: el servidor respondió con error (400, 404, 409...). Se lee {"error": "..."}.
//  - IOException:   no hubo respuesta (sin internet, microservicio apagado, timeout).
object ErroresApi {

    const val SIN_CONEXION = "Sin conexión con el servidor de silos. Revisa que ms-silos esté encendido."

    fun mensaje(error: Throwable): String = when (error) {
        is HttpException -> leerMensajeServidor(error) ?: "Error del servidor (${error.code()})"
        is IOException -> SIN_CONEXION
        else -> error.message ?: "Error desconocido"
    }

    fun esSinConexion(error: Throwable): Boolean = error is IOException

    private fun leerMensajeServidor(error: HttpException): String? = runCatching {
        val cuerpo = error.response()?.errorBody()?.string()
        Gson().fromJson(cuerpo, ErrorDto::class.java)?.error
    }.getOrNull()
}