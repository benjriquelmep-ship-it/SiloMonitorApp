package com.example.silomonitorapp.data.remote

import com.google.gson.annotations.SerializedName
import retrofit2.http.GET
import retrofit2.http.Query

// SERVICIO RETROFIT de la API EXTERNA Open-Meteo (https://open-meteo.com).
// Es un servicio público de terceros: no lo programamos nosotros y no requiere API key.
// Diferencia con ms-silos: aquí solo LEEMOS datos (clima actual en la ubicación GPS del silo);
// no guardamos nada en Room ni en nuestros microservicios.
//
// Ejemplo de llamada:
// GET https://api.open-meteo.com/v1/forecast?latitude=-33.70&longitude=-71.00
//     &current=temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code&timezone=auto
interface ClimaApiService {

    @GET("v1/forecast")
    suspend fun climaActual(
        @Query("latitude") latitud: Double,
        @Query("longitude") longitud: Double,
        @Query("current") variables: String = VARIABLES,
        @Query("timezone") zonaHoraria: String = "auto"
    ): ClimaRespuestaDto

    companion object {
        // Datos que se piden a Open-Meteo (separados por coma).
        const val VARIABLES = "temperature_2m,relative_humidity_2m,precipitation,wind_speed_10m,weather_code"
    }
}

// DTOs: forma exacta del JSON de Open-Meteo.
// @SerializedName traduce los nombres con guion bajo del JSON a nombres de Kotlin.

/** Respuesta completa: solo interesa el bloque "current". */
data class ClimaRespuestaDto(
    val current: ClimaActualDto?
)

/** Bloque "current" de la respuesta. */
data class ClimaActualDto(
    val time: String?,
    @SerializedName("temperature_2m") val temperatura: Double?,
    @SerializedName("relative_humidity_2m") val humedad: Int?,
    @SerializedName("precipitation") val precipitacion: Double?,
    @SerializedName("wind_speed_10m") val viento: Double?,
    @SerializedName("weather_code") val codigoClima: Int?
)