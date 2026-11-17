package com.example.silomonitorapp.data

import com.example.silomonitorapp.data.remote.ErroresApi
import com.example.silomonitorapp.data.remote.SiloDto
import com.example.silomonitorapp.data.remote.aDto
import com.example.silomonitorapp.data.remote.aEntidad
import java.io.IOException
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

// Pruebas de los mappers DTO <-> Entidad y de la traducción de errores de red.
class SiloDtoTest {

    @Test
    fun aEntidad_camposNulosDelServidor_usaValoresPorDefecto() {
        val dto = SiloDto("SIL-009", "Nuevo", "Granja", null, null, 1000.0, 0.0, null, null, null)

        val entidad = dto.aEntidad()

        assertEquals("", entidad.galpon)
        assertEquals("", entidad.tipoAlimento)
        assertEquals(0.0, entidad.consumoPromedioDiario, 0.0)
        assertEquals(0.0, entidad.latitud, 0.0)
    }

    @Test
    fun aDto_yDeVuelta_conservaLosDatos() {
        val dto = SiloDto("SIL-001", "Silo", "Granja", "G1", "Engorda", 1000.0, 400.0, 100.0, -33.7, -71.0)

        assertEquals(dto, dto.aEntidad().aDto())
    }

    @Test
    fun mensaje_errorHttp_leeElCampoErrorDelServidor() {
        val error = HttpException(
            Response.error<Any>(409, """{"error":"Ya existe un silo"}""".toResponseBody("application/json".toMediaType()))
        )

        assertEquals("Ya existe un silo", ErroresApi.mensaje(error))
    }

    @Test
    fun mensaje_errorHttpSinCuerpo_muestraElCodigo() {
        val error = HttpException(Response.error<Any>(500, "".toResponseBody(null)))

        assertEquals("Error del servidor (500)", ErroresApi.mensaje(error))
    }

    @Test
    fun mensaje_sinConexion() {
        assertEquals(ErroresApi.SIN_CONEXION, ErroresApi.mensaje(IOException()))
        assertTrue(ErroresApi.esSinConexion(IOException()))
    }
}