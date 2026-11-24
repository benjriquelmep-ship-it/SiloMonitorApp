package com.example.silomonitorapp

import android.app.Application
import com.example.silomonitorapp.data.local.MovimientoEntity
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.data.local.SolicitudCamionEntity
import com.example.silomonitorapp.data.remote.ResultadoSync
import com.example.silomonitorapp.data.remote.SyncRepository
import com.example.silomonitorapp.notificaciones.NotificadorAlertas
import com.example.silomonitorapp.ui.model.DatosSilo
import com.example.silomonitorapp.ui.model.EstadoSolicitud
import com.example.silomonitorapp.ui.model.TipoMovimiento
import com.example.silomonitorapp.ui.viewmodel.SiloViewModel
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// PRUEBAS UNITARIAS del resto de funciones de SiloViewModel:
// historial, gráfico de consumo, bandeja de solicitudes, formulario de silos y movimientos.
// El repositorio se reemplaza por un mock de MockK (sin Room ni microservicios).
@OptIn(ExperimentalCoroutinesApi::class)
class SiloViewModelFuncionesTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var application: Application
    private lateinit var repository: SiloRepository
    private lateinit var syncRepository: SyncRepository

    // 21.000 de 30.000 kg = 70 % (estado normal).
    private val siloDemo = SiloEntity(
        id = "SIL-001", nombre = "Silo Norte A", granja = "Granja El Paico", galpon = "Galpón 1",
        tipoAlimento = "Engorda", capacidadMaxima = 30_000.0, nivelActual = 21_000.0,
        consumoPromedioDiario = 2_000.0, latitud = -33.7012, longitud = -71.0034
    )

    private val datosValidos = DatosSilo(
        codigo = " sil-010 ", nombre = "Silo Nuevo", granja = "Granja El Paico", galpon = "Galpón 9",
        tipoAlimento = "Engorda", capacidadMaxKg = 20_000.0, stockActualKg = 5_000.0,
        consumoPromedioDiarioKg = 1_000.0, latitud = -33.70, longitud = -71.00
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

    private fun crearViewModel() = SiloViewModel(application, repository, syncRepository)

    // ---------- Historial y gráfico ----------

    @Test
    fun `historial convierte los movimientos de Room a MovimientoUi`() = runTest(testDispatcher) {
        every { repository.historialFlow("SIL-001") } returns flowOf(
            listOf(
                MovimientoEntity(id = 1, siloId = "SIL-001", tipo = "CARGA", cantidadKg = 500.0, fecha = 10L, observacion = "Camión 1", pendienteSincronizar = false),
                MovimientoEntity(id = 2, siloId = "SIL-001", tipo = "CONSUMO", cantidadKg = 200.0, fecha = 20L, fotoUri = "content://foto")
            )
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val historial = viewModel.historial("SIL-001").first()

        historial.size shouldBe 2
        historial[0].tipo shouldBe TipoMovimiento.CARGA
        historial[0].observacion shouldBe "Camión 1"
        historial[1].tipo shouldBe TipoMovimiento.CONSUMO
        historial[1].fotoUri shouldBe "content://foto"
        historial[1].pendienteSincronizar shouldBe true
    }

    @Test
    fun `consumo de los ultimos dias suma solo los consumos de hoy`() = runTest(testDispatcher) {
        val ahora = System.currentTimeMillis()
        every { repository.movimientosDesdeFlow(any()) } returns flowOf(
            listOf(
                MovimientoEntity(siloId = "SIL-001", tipo = "CONSUMO", cantidadKg = 300.0, fecha = ahora),
                MovimientoEntity(siloId = "SIL-001", tipo = "CONSUMO", cantidadKg = 200.0, fecha = ahora),
                MovimientoEntity(siloId = "SIL-001", tipo = "CARGA", cantidadKg = 9_999.0, fecha = ahora)
            )
        )
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val dias = viewModel.consumoUltimosDias().first()

        dias.size shouldBe 7
        dias.last().kg shouldBe 500.0          // hoy: solo consumos (la carga no cuenta)
        dias.sumOf { it.kg } shouldBe 500.0    // los otros días quedan en 0
    }

    // ---------- Bandeja de solicitudes ----------

    @Test
    fun `solicitudesUi agrega nombre y granja del silo`() = runTest(testDispatcher) {
        every { repository.solicitudesFlow } returns flowOf(
            listOf(
                SolicitudCamionEntity(id = 5, siloId = "SIL-001", kgSolicitados = 500.0, urgente = true, solicitadoPor = "supervisor"),
                SolicitudCamionEntity(
                    id = 6, siloId = "SIL-XXX", kgSolicitados = 100.0, urgente = false, solicitadoPor = "supervisor",
                    estado = "APROBADA", revisadoPor = "jefatura"
                )
            )
        )
        val viewModel = crearViewModel()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.solicitudesUi.collect() }
        advanceUntilIdle()

        val lista = viewModel.solicitudesUi.value
        lista.size shouldBe 2
        lista[0].siloNombre shouldBe "Silo Norte A"
        lista[0].granja shouldBe "Granja El Paico"
        lista[0].estado shouldBe EstadoSolicitud.PENDIENTE
        // Silo que no está en Room: se muestra el código y sin granja.
        lista[1].siloNombre shouldBe "SIL-XXX"
        lista[1].granja shouldBe ""
        lista[1].estado shouldBe EstadoSolicitud.APROBADA
        lista[1].revisadoPor shouldBe "jefatura"
    }

    @Test
    fun `revisar solicitud rechazada y con error muestran el mensaje`() = runTest(testDispatcher) {
        coEvery { repository.revisarSolicitud(7L, false, "jefatura") } returns Result.success(Unit)
        coEvery { repository.revisarSolicitud(8L, true, "jefatura") } returns Result.failure(Exception("La solicitud ya fue revisada."))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.revisarSolicitud(7L, aprobada = false, usuario = "jefatura")
        advanceUntilIdle()
        viewModel.mensajeOperacion.value shouldBe "Solicitud rechazada"

        viewModel.revisarSolicitud(8L, aprobada = true, usuario = "jefatura")
        advanceUntilIdle()
        viewModel.mensajeOperacion.value shouldBe "La solicitud ya fue revisada."
    }

    // ---------- Búsqueda y formulario de silos ----------

    @Test
    fun `existeSilo responde true o deja un mensaje si no existe`() = runTest(testDispatcher) {
        coEvery { repository.obtenerSiloPorId("SIL-001") } returns siloDemo
        coEvery { repository.obtenerSiloPorId("SIL-999") } returns null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.existeSilo("SIL-001") shouldBe true
        viewModel.mensajeOperacion.value shouldBe null

        viewModel.existeSilo("SIL-999") shouldBe false
        viewModel.mensajeOperacion.value shouldContain "SIL-999"
    }

    @Test
    fun `obtenerDatosSilo precarga el formulario de edicion`() = runTest(testDispatcher) {
        coEvery { repository.obtenerSiloPorId("SIL-001") } returns siloDemo
        coEvery { repository.obtenerSiloPorId("SIL-999") } returns null
        val viewModel = crearViewModel()
        advanceUntilIdle()

        val datos = viewModel.obtenerDatosSilo("SIL-001")
        datos?.codigo shouldBe "SIL-001"
        datos?.capacidadMaxKg shouldBe 30_000.0
        datos?.stockActualKg shouldBe 21_000.0
        viewModel.obtenerDatosSilo("SIL-999") shouldBe null
    }

    @Test
    fun `guardarSilo con datos invalidos devuelve el error y no llama al repositorio`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.guardarSilo(datosValidos.copy(codigo = "  "), esNuevo = true) shouldBe "El código del silo es obligatorio."
        viewModel.guardarSilo(datosValidos.copy(stockActualKg = 50_000.0), esNuevo = true) shouldBe
                "El stock no puede superar la capacidad máxima."
        coVerify(exactly = 0) { repository.crearSilo(any()) }
    }

    @Test
    fun `guardarSilo nuevo normaliza el codigo y confirma`() = runTest(testDispatcher) {
        coEvery { repository.crearSilo(any()) } returns Result.success(Unit)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.guardarSilo(datosValidos, esNuevo = true) shouldBe null

        coVerify { repository.crearSilo(match { it.id == "SIL-010" && it.nombre == "Silo Nuevo" }) }
        viewModel.mensajeOperacion.value shouldBe "Silo SIL-010 creado"
    }

    @Test
    fun `guardarSilo editado confirma o devuelve el error del servidor`() = runTest(testDispatcher) {
        coEvery { repository.editarSilo(match { it.id == "SIL-010" }) } returns Result.success(Unit)
        coEvery { repository.editarSilo(match { it.id == "SIL-011" }) } returns Result.failure(Exception("Sin conexión"))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.guardarSilo(datosValidos, esNuevo = false) shouldBe null
        viewModel.mensajeOperacion.value shouldBe "Silo SIL-010 actualizado"

        viewModel.guardarSilo(datosValidos.copy(codigo = "SIL-011"), esNuevo = false) shouldBe "Sin conexión"
    }

    // ---------- Movimientos ----------

    @Test
    fun `registrar carga exitosa confirma y no notifica si el silo queda normal`() = runTest(testDispatcher) {
        coEvery { repository.registrarMovimiento("SIL-001", 100.0, true, "", null) } returns Result.success(siloDemo)
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.registrarMovimiento("SIL-001", TipoMovimiento.CARGA, 100.0) shouldBe null
        viewModel.mensajeOperacion.value shouldBe "Carga registrada con éxito"
    }

    @Test
    fun `registrar consumo que deja el silo critico dispara la notificacion`() = runTest(testDispatcher) {
        mockkObject(NotificadorAlertas)
        try {
            every { NotificadorAlertas.notificarNivelCritico(any(), any()) } just Runs
            val critico = siloDemo.copy(nivelActual = 1_000.0) // 3 % de la capacidad
            coEvery { repository.registrarMovimiento("SIL-001", 20_000.0, false, "Consumo alto", null) } returns Result.success(critico)
            val viewModel = crearViewModel()
            advanceUntilIdle()

            viewModel.registrarMovimiento("SIL-001", TipoMovimiento.CONSUMO, 20_000.0, "Consumo alto") shouldBe null

            viewModel.mensajeOperacion.value shouldBe "Consumo registrado con éxito"
            verify { NotificadorAlertas.notificarNivelCritico(any(), critico) }
        } finally {
            unmockkObject(NotificadorAlertas)
        }
    }

    @Test
    fun `registrar movimiento rechazado devuelve el mensaje de error`() = runTest(testDispatcher) {
        coEvery { repository.registrarMovimiento(any(), any(), any(), any(), any()) } returns
                Result.failure(Exception("La carga supera la capacidad del silo."))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.registrarMovimiento("SIL-001", TipoMovimiento.CARGA, 99_999.0) shouldBe "La carga supera la capacidad del silo."
    }

    // ---------- Eliminar, sincronizar y mensajes ----------

    @Test
    fun `eliminar silo con error devuelve false y muestra el mensaje`() = runTest(testDispatcher) {
        coEvery { repository.eliminarSilo("SIL-001") } returns Result.failure(Exception("Falla interna"))
        val viewModel = crearViewModel()
        advanceUntilIdle()

        viewModel.eliminarSilo("SIL-001") shouldBe false
        viewModel.mensajeOperacion.value shouldBe "Falla interna"
    }

    @Test
    fun `sincronizar con error informa el problema`() = runTest(testDispatcher) {
        val viewModel = crearViewModel()
        advanceUntilIdle()
        coEvery { repository.sincronizarConServidor() } returns Result.failure(Exception("Sin conexión con el servidor"))

        viewModel.sincronizar()
        advanceUntilIdle()

        viewModel.mensajeOperacion.value shouldBe "Error al sincronizar: Sin conexión con el servidor"
        viewModel.sincronizando.value shouldBe false
    }

    @Test
    fun `limpiarMensaje deja el mensaje en null`() = runTest(testDispatcher) {
        coEvery { repository.sincronizarConServidor() } returns Result.failure(Exception("x"))
        val viewModel = crearViewModel()
        advanceUntilIdle()
        viewModel.mensajeOperacion.value shouldContain "Modo sin conexión"

        viewModel.limpiarMensaje()

        viewModel.mensajeOperacion.value shouldBe null
    }
}