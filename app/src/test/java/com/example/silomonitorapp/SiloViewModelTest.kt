package com.example.silomonitorapp

import android.app.Application
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.data.remote.ResultadoSync
import com.example.silomonitorapp.data.remote.SyncRepository
import com.example.silomonitorapp.ui.viewmodel.SiloViewModel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SiloViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var repository: SiloRepository
    private lateinit var syncRepository: SyncRepository

    private val siloDemo = SiloEntity(
        id = "SIL-001",
        nombre = "Silo Norte A",
        granja = "Granja El Paico",
        galpon = "Galpón 1",
        tipoAlimento = "Engorda",
        capacidadMaxima = 30_000.0,
        nivelActual = 21_000.0,
        consumoPromedioDiario = 2_000.0,
        latitud = -33.7012,
        longitud = -71.0034
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        application = mockk(relaxed = true)
        repository = mockk()
        syncRepository = mockk()

        every { repository.silosFlow } returns flowOf(listOf(siloDemo))
        every { repository.solicitudesFlow } returns flowOf(emptyList())
        coEvery { repository.precargarSilosSiEstaVacio() } just Runs
        coEvery { repository.sincronizarConServidor() } returns Result.success(0)
        coEvery { repository.sincronizarSolicitudes() } returns Result.success(0)
        coEvery { syncRepository.sincronizarTodo() } returns ResultadoSync(exitoso = true)
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `cargar silos correctamente mapea entidades a SiloUi`() = runTest(testDispatcher) {
        val viewModel = SiloViewModel(application, repository, syncRepository)

        // Activa la suscripción del StateFlow (SharingStarted.WhileSubscribed)
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.silosUi.collect()
        }
        advanceUntilIdle()

        viewModel.silosUi.value.size shouldBe 1
        viewModel.silosUi.value[0].codigo shouldBe "SIL-001"
        viewModel.silosUi.value[0].nombre shouldBe "Silo Norte A"
        viewModel.mensajeOperacion.value shouldBe null
    }

    @Test
    fun `mostrar error de modo sin conexion cuando falla la consulta inicial`() = runTest(testDispatcher) {
        coEvery { repository.sincronizarConServidor() } returns
                Result.failure(Exception("Sin conexión con el servidor"))

        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()

        viewModel.mensajeOperacion.value shouldContain "Modo sin conexión"
    }

    @Test
    fun `sincronizar activa y desactiva el estado de carga correctamente`() = runTest(testDispatcher) {
        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()

        coEvery { repository.sincronizarConServidor() } coAnswers {
            delay(100)
            Result.success(2)
        }

        viewModel.sincronizar()
        runCurrent()
        viewModel.sincronizando.value shouldBe true

        advanceUntilIdle()
        viewModel.sincronizando.value shouldBe false
        viewModel.mensajeOperacion.value shouldBe "Enviados 2 movimientos pendientes"
    }

    @Test
    fun `eliminar silo exitoso retorna true y muestra confirmacion`() = runTest(testDispatcher) {
        coEvery { repository.eliminarSilo("SIL-001") } returns Result.success(Unit)
        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()

        val eliminado = viewModel.eliminarSilo("SIL-001")

        eliminado shouldBe true
        viewModel.mensajeOperacion.value shouldBe "Silo SIL-001 eliminado"
    }

    @Test
    fun `sincronizar informa las solicitudes enviadas a ms-solicitudes`() = runTest(testDispatcher) {
        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()
        coEvery { repository.sincronizarSolicitudes() } returns Result.success(3)

        viewModel.sincronizar()
        advanceUntilIdle()

        viewModel.mensajeOperacion.value shouldBe "Silos actualizados desde el servidor y 3 solicitudes"
    }

    @Test
    fun `solicitar camion exitoso devuelve null y confirma`() = runTest(testDispatcher) {
        coEvery { repository.solicitarCamion("SIL-001", 500.0, true, "Nivel bajo", "supervisor") } returns Result.success(Unit)
        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()

        viewModel.solicitarCamion("SIL-001", 500.0, true, "  Nivel bajo ", "supervisor") shouldBe null
        viewModel.mensajeOperacion.value shouldContain "Camión solicitado"
    }

    @Test
    fun `solicitar camion rechazado devuelve el mensaje de error`() = runTest(testDispatcher) {
        coEvery { repository.solicitarCamion(any(), any(), any(), any(), any()) } returns
                Result.failure(Exception("Ya hay un camión pendiente de aprobación para este silo."))
        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()

        viewModel.solicitarCamion("SIL-001", 500.0, false, "", "supervisor") shouldContain "camión pendiente"
    }

    @Test
    fun `revisar solicitud aprobada muestra confirmacion`() = runTest(testDispatcher) {
        coEvery { repository.revisarSolicitud(7L, true, "jefatura") } returns Result.success(Unit)
        val viewModel = SiloViewModel(application, repository, syncRepository)
        advanceUntilIdle()

        viewModel.revisarSolicitud(7L, aprobada = true, usuario = "jefatura")
        advanceUntilIdle()

        viewModel.mensajeOperacion.value shouldBe "Reposición aprobada"
    }
}