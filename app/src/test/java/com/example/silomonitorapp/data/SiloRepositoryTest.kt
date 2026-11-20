package com.example.silomonitorapp.data

import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.MovimientoDao
import com.example.silomonitorapp.data.local.MovimientoEntity
import com.example.silomonitorapp.data.local.SiloDao
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.data.local.SolicitudCamionDao
import com.example.silomonitorapp.data.remote.ErroresApi
import com.example.silomonitorapp.data.remote.SiloApiService
import com.example.silomonitorapp.data.remote.SiloDto
import com.example.silomonitorapp.data.remote.aDto
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import java.io.IOException
import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import retrofit2.HttpException
import retrofit2.Response

// PRUEBAS UNITARIAS del SiloRepository con MockK.
// Qué se mockea:  la API Retrofit (SiloApiService) y Room (AppDatabase + DAOs).
// Qué se prueba:  la lógica del repositorio -> qué hace con la respuesta del servidor,
//                 cuándo guarda en Room, cuándo queda pendiente y qué mensaje devuelve.
// Así las pruebas no necesitan internet, ni el microservicio encendido, ni un emulador.
class SiloRepositoryTest {

    private lateinit var api: SiloApiService
    private lateinit var db: AppDatabase
    private lateinit var siloDao: SiloDao
    private lateinit var movimientoDao: MovimientoDao
    private lateinit var repository: SiloRepository

    // Silo de prueba: 1000 kg de capacidad con 400 kg de stock.
    private val silo = SiloEntity(
        "SIL-001", "Silo Norte A", "Granja El Paico", "Galpón 1", "Engorda",
        capacidadMaxima = 1000.0, nivelActual = 400.0, consumoPromedioDiario = 100.0,
        latitud = -33.7, longitud = -71.0
    )

    @Before
    fun setUp() {
        api = mockk()
        // relaxed = true: las funciones no configuradas devuelven valores por defecto (0, Unit...)
        siloDao = mockk(relaxed = true)
        movimientoDao = mockk(relaxed = true)
        db = mockk(relaxed = true)
        every { db.siloDao() } returns siloDao
        every { db.movimientoDao() } returns movimientoDao
        every { db.solicitudCamionDao() } returns mockk<SolicitudCamionDao>(relaxed = true)
        // La transacción de Room se simula ejecutando directamente el bloque.
        every { db.runInTransaction(any<Runnable>()) } answers { firstArg<Runnable>().run() }

        repository = SiloRepository(db, api)
    }

    // Crea un error HTTP como el que manda el GlobalExceptionHandler del microservicio.
    private fun errorHttp(codigo: Int, mensaje: String) = HttpException(
        Response.error<Any>(codigo, """{"error":"$mensaje"}""".toResponseBody("application/json".toMediaType()))
    )

    // ---------- Movimientos ----------

    @Test
    fun registrarMovimiento_conServidor_usaElStockDelServidorYNoQuedaPendiente() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo
        coEvery { api.registrarMovimiento("SIL-001", any()) } returns silo.copy(nivelActual = 700.0).aDto()
        val guardado = slot<MovimientoEntity>()
        every { movimientoDao.insertar(capture(guardado)) } returns 1L

        val resultado = repository.registrarMovimiento("SIL-001", 300.0, esCarga = true)

        assertEquals(700.0, resultado.getOrThrow().nivelActual, 0.0)
        verify { siloDao.actualizarNivel("SIL-001", 700.0) }
        assertEquals("CARGA", guardado.captured.tipo)
        assertFalse(guardado.captured.pendienteSincronizar)
    }

    @Test
    fun registrarMovimiento_sinConexion_guardaLocalYQuedaPendiente() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo
        coEvery { api.registrarMovimiento(any(), any()) } throws IOException("sin internet")
        val guardado = slot<MovimientoEntity>()
        every { movimientoDao.insertar(capture(guardado)) } returns 1L

        val resultado = repository.registrarMovimiento("SIL-001", 100.0, esCarga = false)

        assertEquals(300.0, resultado.getOrThrow().nivelActual, 0.0)
        assertTrue(guardado.captured.pendienteSincronizar)
    }

    @Test
    fun registrarMovimiento_servidorLoRechaza_devuelveSuMensajeYNoGuarda() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo
        coEvery { api.registrarMovimiento(any(), any()) } throws errorHttp(409, "Sobrellenado en servidor")

        val resultado = repository.registrarMovimiento("SIL-001", 100.0, esCarga = true)

        assertEquals("Sobrellenado en servidor", resultado.exceptionOrNull()?.message)
        verify(exactly = 0) { movimientoDao.insertar(any()) }
    }

    @Test
    fun registrarMovimiento_sobrellenadoLocal_noLlamaAlServidor() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo

        val resultado = repository.registrarMovimiento("SIL-001", 900.0, esCarga = true)

        assertTrue(resultado.isFailure)
        coVerify(exactly = 0) { api.registrarMovimiento(any(), any()) }
    }

    @Test
    fun registrarMovimiento_siloInexistente_falla() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-999") } returns null

        val resultado = repository.registrarMovimiento("SIL-999", 10.0, esCarga = true)

        assertEquals("Silo no encontrado en el sistema", resultado.exceptionOrNull()?.message)
    }

    // ---------- CRUD de silos ----------

    @Test
    fun crearSilo_ok_loGuardaEnRoomConLaRespuestaDelServidor() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns null
        coEvery { api.crearSilo(any()) } returns silo.aDto()

        val resultado = repository.crearSilo(silo)

        assertTrue(resultado.isSuccess)
        verify { siloDao.insertarSilo(silo) }
    }

    @Test
    fun crearSilo_codigoRepetido_fallaSinLlamarAlServidor() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo

        val resultado = repository.crearSilo(silo)

        assertTrue(resultado.isFailure)
        coVerify(exactly = 0) { api.crearSilo(any()) }
    }

    @Test
    fun crearSilo_sinConexion_devuelveMensajeSinConexion() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns null
        coEvery { api.crearSilo(any()) } throws IOException()

        val resultado = repository.crearSilo(silo)

        assertEquals(ErroresApi.SIN_CONEXION, resultado.exceptionOrNull()?.message)
        verify(exactly = 0) { siloDao.insertarSilo(any()) }
    }

    @Test
    fun editarSilo_conservaElStockActual() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo
        val enviado = slot<SiloDto>()
        coEvery { api.editarSilo("SIL-001", capture(enviado)) } answers { enviado.captured }

        // El formulario manda nivel 0, pero el stock real (400) no se debe perder.
        val resultado = repository.editarSilo(silo.copy(nombre = "Editado", nivelActual = 0.0))

        assertTrue(resultado.isSuccess)
        assertEquals(400.0, enviado.captured.nivelActual, 0.0)
        assertEquals("Editado", enviado.captured.nombre)
    }

    @Test
    fun editarSilo_capacidadMenorAlStock_falla() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo

        val resultado = repository.editarSilo(silo.copy(capacidadMaxima = 100.0))

        assertTrue(resultado.isFailure)
        coVerify(exactly = 0) { api.editarSilo(any(), any()) }
    }

    @Test
    fun eliminarSilo_ok_borraLaCopiaLocal() = runTest {
        coEvery { api.eliminarSilo("SIL-001") } returns Response.success(Unit)

        val resultado = repository.eliminarSilo("SIL-001")

        assertTrue(resultado.isSuccess)
        verify { siloDao.eliminarPorId("SIL-001") }
    }

    @Test
    fun eliminarSilo_errorDelServidor_noBorraLocal() = runTest {
        coEvery { api.eliminarSilo("SIL-001") } returns
                Response.error(500, """{"error":"Falla interna"}""".toResponseBody("application/json".toMediaType()))

        val resultado = repository.eliminarSilo("SIL-001")

        assertEquals("Falla interna", resultado.exceptionOrNull()?.message)
        verify(exactly = 0) { siloDao.eliminarPorId(any()) }
    }

    // ---------- Sincronización ----------

    @Test
    fun sincronizar_enviaPendientesYGuardaLosSilosDelServidor() = runTest {
        val pendiente = MovimientoEntity(id = 7, siloId = "SIL-001", tipo = "CONSUMO", cantidadKg = 50.0)
        every { movimientoDao.obtenerPendientesSincronizar() } returns listOf(pendiente)
        coEvery { api.registrarMovimiento("SIL-001", any()) } returns silo.aDto()
        coEvery { api.listarSilos() } returns listOf(silo.aDto())

        val resultado = repository.sincronizarConServidor()

        assertEquals(1, resultado.getOrThrow())
        verify { movimientoDao.marcarComoSincronizados(listOf(7L)) }
        verify { siloDao.upsertSilos(listOf(silo)) }
        verify { siloDao.eliminarAusentes(listOf("SIL-001")) }
    }

    @Test
    fun sincronizar_sinConexion_devuelveError() = runTest {
        every { movimientoDao.obtenerPendientesSincronizar() } returns emptyList()
        coEvery { api.listarSilos() } throws IOException()

        val resultado = repository.sincronizarConServidor()

        assertEquals(ErroresApi.SIN_CONEXION, resultado.exceptionOrNull()?.message)
    }

    @Test
    fun sincronizar_servidorRetornaListaVacia_actualizaRoomSinErrores() = runTest {
        every { movimientoDao.obtenerPendientesSincronizar() } returns emptyList()
        coEvery { api.listarSilos() } returns emptyList()

        val resultado = repository.sincronizarConServidor()

        assertTrue(resultado.isSuccess)
        assertEquals(0, resultado.getOrThrow())
        verify { siloDao.upsertSilos(emptyList()) }
    }
}