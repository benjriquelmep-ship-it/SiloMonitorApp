package com.example.silomonitorapp.data.remote

import com.example.silomonitorapp.domain.ReglasClima
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

/** Clima ya interpretado, listo para mostrar en la ficha del silo. */
data class ClimaSilo(
    val temperaturaC: Double,
    val humedad: Int?,
    val vientoKmh: Double?,
    val precipitacionMm: Double?,
    val descripcion: String,
    val recomendacion: String
)

// REPOSITORY de la API externa Open-Meteo.
// La API llega por constructor para reemplazarla por un mock en las pruebas unitarias.
class ClimaRepository(
    private val api: ClimaApiService = RetrofitClient.climaApi
) {

    // Consulta el clima actual en las coordenadas del silo.
    // No se guarda nada en Room: es información de apoyo que se consulta al abrir la ficha.
    suspend fun obtenerClima(latitud: Double, longitud: Double): Result<ClimaSilo> = withContext(Dispatchers.IO) {
        // Coordenadas fuera de rango (ej: silo sin GPS registrado en 0,0 o datos corruptos).
        if (latitud !in -90.0..90.0 || longitud !in -180.0..180.0 || (latitud == 0.0 && longitud == 0.0)) {
            return@withContext Result.failure(Exception("El silo no tiene una ubicación GPS válida."))
        }
        try {
            val actual = api.climaActual(latitud, longitud).current
            val temperatura = actual?.temperatura
                ?: return@withContext Result.failure(Exception("Open-Meteo no entregó datos para esta ubicación."))
            Result.success(
                ClimaSilo(
                    temperaturaC = temperatura,
                    humedad = actual.humedad,
                    vientoKmh = actual.viento,
                    precipitacionMm = actual.precipitacion,
                    descripcion = ReglasClima.descripcion(actual.codigoClima),
                    recomendacion = ReglasClima.recomendacion(
                        actual.codigoClima, temperatura, actual.viento, actual.precipitacion
                    )
                )
            )
        } catch (e: CancellationException) {
            throw e // se respeta la cancelación de corrutinas
        } catch (e: HttpException) {
            Result.failure(Exception("El servicio de clima respondió con error (${e.code()})."))
        } catch (e: IOException) {
            Result.failure(Exception("Sin conexión: no se pudo consultar el clima."))
        }
    }
}