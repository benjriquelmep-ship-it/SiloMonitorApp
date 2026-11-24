package com.example.silomonitorapp

import com.example.silomonitorapp.data.remote.ClimaRepository
import com.example.silomonitorapp.data.remote.ClimaSilo
import com.example.silomonitorapp.ui.viewmodel.ClimaViewModel
import com.example.silomonitorapp.ui.viewmodel.EstadoClima
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// PRUEBAS UNITARIAS del ClimaViewModel (estado de la tarjeta de clima).
// Qué se mockea: ClimaRepository. Qué se prueba: los estados Cargando -> Exito / Error.
@OptIn(ExperimentalCoroutinesApi::class)
class ClimaViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var repository: ClimaRepository

    private val climaDemo = ClimaSilo(
        temperaturaC = 21.0, humedad = 40, vientoKmh = 12.0, precipitacionMm = 0.0,
        descripcion = "Despejado", recomendacion = "Condiciones normales para la descarga."
    )

    @BeforeEach
    fun setUp() {
        // viewModelScope usa Dispatchers.Main: en pruebas se reemplaza por un dispatcher controlado.
        Dispatchers.setMain(testDispatcher)
        repository = mockk()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `estado inicial es Cargando`() {
        ClimaViewModel(repository).estado.value shouldBe EstadoClima.Cargando
    }

    @Test
    fun `cargar con exito publica el clima`() = runTest(testDispatcher) {
        coEvery { repository.obtenerClima(-33.7, -71.0) } returns Result.success(climaDemo)
        val viewModel = ClimaViewModel(repository)

        viewModel.cargar(-33.7, -71.0)
        viewModel.estado.value shouldBe EstadoClima.Cargando
        advanceUntilIdle()

        viewModel.estado.value shouldBe EstadoClima.Exito(climaDemo)
    }

    @Test
    fun `cargar con error publica el mensaje`() = runTest(testDispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } returns Result.failure(Exception("Sin conexión"))
        val viewModel = ClimaViewModel(repository)

        viewModel.cargar(-33.7, -71.0)
        advanceUntilIdle()

        val error = viewModel.estado.value.shouldBeInstanceOf<EstadoClima.Error>()
        error.mensaje shouldBe "Sin conexión"
    }

    @Test
    fun `no repite la consulta para la misma ubicacion salvo que se fuerce`() = runTest(testDispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } returns Result.success(climaDemo)
        val viewModel = ClimaViewModel(repository)

        viewModel.cargar(-33.7, -71.0)
        advanceUntilIdle()
        viewModel.cargar(-33.7, -71.0)            // misma ubicación: no llama otra vez
        advanceUntilIdle()
        viewModel.cargar(-33.7, -71.0, forzar = true) // botón "Reintentar"
        advanceUntilIdle()

        coVerify(exactly = 2) { repository.obtenerClima(-33.7, -71.0) }
    }

    @Test
    fun `una ubicacion distinta vuelve a consultar`() = runTest(testDispatcher) {
        coEvery { repository.obtenerClima(any(), any()) } returns Result.success(climaDemo)
        val viewModel = ClimaViewModel(repository)

        viewModel.cargar(-33.7, -71.0)
        advanceUntilIdle()
        viewModel.cargar(-33.6, -71.1)
        advanceUntilIdle()

        coVerify(exactly = 1) { repository.obtenerClima(-33.6, -71.1) }
    }
}