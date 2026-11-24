package com.example.silomonitorapp.data

import com.example.silomonitorapp.data.remote.ClimaActualDto
import com.example.silomonitorapp.data.remote.ClimaApiService
import com.example.silomonitorapp.data.remote.ClimaRepository
import com.example.silomonitorapp.data.remote.ClimaRespuestaDto
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

// PRUEBAS UNITARIAS del ClimaRepository (API externa Open-Meteo) con MockK.
// Qué se mockea: ClimaApiService (Retrofit), así no se necesita internet.
// Qué se prueba:  cómo el repositorio interpreta la respuesta, los errores y las coordenadas inválidas.
class ClimaRepositoryTest {

    private lateinit var api: ClimaApiService
    private lateinit var repository: ClimaRepository

    @BeforeEach
    fun setUp() {
        api = mockk()
        repository = ClimaRepository(api)
    }

    private fun respuesta(temperatura: Double?, codigo: Int?, viento: Double? = 10.0, precipitacion: Double? = 0.0) =
        ClimaRespuestaDto(
            ClimaActualDto(
                time = "2026-11-23T12:00",
                temperatura = temperatura,
                humedad = 55,
                precipitacion = precipitacion,
                viento = viento,
                codigoClima = codigo
            )
        )

    @Test
    fun `obtenerClima con respuesta valida devuelve el clima interpretado`() = runTest {
        coEvery { api.climaActual(-33.7, -71.0, any(), any()) } returns respuesta(temperatura = 18.5, codigo = 0)

        val clima = repository.obtenerClima(-33.7, -71.0).getOrThrow()

        clima.temperaturaC shouldBe 18.5
        clima.humedad shouldBe 55
        clima.descripcion shouldBe "Despejado"
        clima.recomendacion shouldContain "Condiciones normales"
    }

    @Test
    fun `obtenerClima con lluvia entrega la recomendacion de precipitaciones`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } returns respuesta(18.0, codigo = 63, precipitacion = 3.2)

        val clima = repository.obtenerClima(-33.7, -71.0).getOrThrow()

        clima.descripcion shouldBe "Lluvia"
        clima.recomendacion shouldContain "Precipitaciones"
    }

    @Test
    fun `obtenerClima sin bloque current devuelve error`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } returns ClimaRespuestaDto(current = null)

        val resultado = repository.obtenerClima(-33.7, -71.0)

        resultado.isFailure shouldBe true
        resultado.exceptionOrNull()?.message shouldContain "no entregó datos"
    }

    @Test
    fun `obtenerClima sin temperatura devuelve error`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } returns respuesta(temperatura = null, codigo = 0)

        repository.obtenerClima(-33.7, -71.0).isFailure shouldBe true
    }

    @Test
    fun `obtenerClima sin internet devuelve mensaje de sin conexion`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } throws IOException("timeout")

        repository.obtenerClima(-33.7, -71.0).exceptionOrNull()?.message shouldContain "Sin conexión"
    }

    @Test
    fun `obtenerClima con error HTTP informa el codigo`() = runTest {
        coEvery { api.climaActual(any(), any(), any(), any()) } throws HttpException(
            Response.error<Any>(500, "error".toResponseBody("text/plain".toMediaType()))
        )

        repository.obtenerClima(-33.7, -71.0).exceptionOrNull()?.message shouldContain "500"
    }

    @Test
    fun `obtenerClima con coordenadas invalidas no llama a la API`() = runTest {
        repository.obtenerClima(0.0, 0.0).isFailure shouldBe true
        repository.obtenerClima(120.0, -71.0).isFailure shouldBe true
        repository.obtenerClima(-33.7, 200.0).isFailure shouldBe true

        coVerify(exactly = 0) { api.climaActual(any(), any(), any(), any()) }
    }
}