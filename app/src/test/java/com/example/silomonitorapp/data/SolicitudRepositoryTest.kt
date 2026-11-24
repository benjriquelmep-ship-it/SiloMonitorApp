package com.example.silomonitorapp.data

import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.MovimientoDao
import com.example.silomonitorapp.data.local.SiloDao
import com.example.silomonitorapp.data.local.SiloEntity
import com.example.silomonitorapp.data.local.SiloRepository
import com.example.silomonitorapp.data.local.SolicitudCamionDao
import com.example.silomonitorapp.data.local.SolicitudCamionEntity
import com.example.silomonitorapp.data.remote.RevisionRequestDto
import com.example.silomonitorapp.data.remote.SiloApiService
import com.example.silomonitorapp.data.remote.SolicitudApiService
import com.example.silomonitorapp.data.remote.SolicitudDto
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
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
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import retrofit2.HttpException
import retrofit2.Response

// PRUEBAS UNITARIAS de las solicitudes de camión contra el microservicio ms-solicitudes.
// Qué se mockea: SolicitudApiService (Retrofit) y Room (AppDatabase + DAOs).
// Qué se prueba:  envío al servidor, respaldo offline (id negativo), revisión y sincronización.
class SolicitudesRepositoryTest {

    private lateinit var apiSolicitudes: SolicitudApiService
    private lateinit var db: AppDatabase
    private lateinit var siloDao: SiloDao
    private lateinit var solicitudDao: SolicitudCamionDao
    private lateinit var repository: SiloRepository

    // Silo de 1000 kg con 400 kg: caben hasta 600 kg.
    private val silo = SiloEntity(
        "SIL-001", "Silo Norte A", "Granja El Paico", "Galpón 1", "Engorda",
        capacidadMaxima = 1000.0, nivelActual = 400.0, consumoPromedioDiario = 100.0,
        latitud = -33.7, longitud = -71.0
    )

    @BeforeEach
    fun setUp() {
        apiSolicitudes = mockk()
        siloDao = mockk(relaxed = true)
        solicitudDao = mockk(relaxed = true)
        db = mockk(relaxed = true)
        every { db.siloDao() } returns siloDao
        every { db.movimientoDao() } returns mockk<MovimientoDao>(relaxed = true)
        every { db.solicitudCamionDao() } returns solicitudDao
        every { db.runInTransaction(any<Runnable>()) } answers { firstArg<Runnable>().run() }
        every { siloDao.obtenerSiloPorId("SIL-001") } returns silo
        every { solicitudDao.contarPendientesDeSilo(any()) } returns 0

        repository = SiloRepository(db, mockk<SiloApiService>(), apiSolicitudes)
    }

    private fun dto(id: Long, estado: String = "PENDIENTE", siloId: String = "SIL-001") = SolicitudDto(
        id = id, siloId = siloId, kgSolicitados = 500.0, urgente = true, observacion = "Nivel bajo",
        solicitadoPor = "supervisor", fechaSolicitud = 1000L, estado = estado, revisadoPor = null, fechaRevision = null
    )

    private fun errorHttp(codigo: Int, mensaje: String) = HttpException(
        Response.error<Any>(codigo, """{"error":"$mensaje"}""".toResponseBody("application/json".toMediaType()))
    )

    // ---------- Solicitar camión ----------

    @Test
    fun `solicitar con servidor guarda la copia del servidor ya sincronizada`() = runTest {
        coEvery { apiSolicitudes.crear(any()) } returns dto(id = 7)
        val guardada = slot<SolicitudCamionEntity>()
        every { solicitudDao.insertarOActualizar(capture(guardada)) } returns 7L

        repository.solicitarCamion("SIL-001", 500.0, true, "Nivel bajo", "supervisor").isSuccess shouldBe true

        guardada.captured.id shouldBe 7L
        guardada.captured.pendienteSincronizar shouldBe false
    }

    @Test
    fun `solicitar sin conexion guarda local con id negativo y pendiente`() = runTest {
        coEvery { apiSolicitudes.crear(any()) } throws IOException("sin internet")
        val guardada = slot<SolicitudCamionEntity>()
        every { solicitudDao.insertarOActualizar(capture(guardada)) } returns 1L

        repository.solicitarCamion("SIL-001", 500.0, false, "", "supervisor").isSuccess shouldBe true

        (guardada.captured.id < 0) shouldBe true
        guardada.captured.pendienteSincronizar shouldBe true
        guardada.captured.estado shouldBe "PENDIENTE"
    }

    @Test
    fun `solicitar rechazado por el servidor devuelve su mensaje y no guarda`() = runTest {
        coEvery { apiSolicitudes.crear(any()) } throws errorHttp(409, "Ya hay un camión pendiente de aprobación para este silo.")

        val resultado = repository.solicitarCamion("SIL-001", 500.0, false, "", "supervisor")

        resultado.exceptionOrNull()?.message shouldContain "camión pendiente"
        verify(exactly = 0) { solicitudDao.insertarOActualizar(any()) }
    }

    @Test
    fun `solicitar mas kilos de los que caben no llama al servidor`() = runTest {
        repository.solicitarCamion("SIL-001", 900.0, false, "", "supervisor").isFailure shouldBe true

        coVerify(exactly = 0) { apiSolicitudes.crear(any()) }
    }

    @Test
    fun `solicitar con camion pendiente local no llama al servidor`() = runTest {
        every { solicitudDao.contarPendientesDeSilo("SIL-001") } returns 1

        repository.solicitarCamion("SIL-001", 100.0, false, "", "supervisor").isFailure shouldBe true

        coVerify(exactly = 0) { apiSolicitudes.crear(any()) }
    }

    @Test
    fun `solicitar para un silo inexistente falla`() = runTest {
        every { siloDao.obtenerSiloPorId("SIL-999") } returns null

        repository.solicitarCamion("SIL-999", 100.0, false, "", "supervisor").isFailure shouldBe true
    }

    // ---------- Revisar (aprobar / rechazar) ----------

    @Test
    fun `revisar con servidor envia la revision y guarda la respuesta`() = runTest {
        val revision = slot<RevisionRequestDto>()
        coEvery { apiSolicitudes.revisar(7L, capture(revision)) } returns dto(id = 7, estado = "APROBADA")

        repository.revisarSolicitud(7L, aprobada = true, revisadoPor = "jefatura").isSuccess shouldBe true

        revision.captured.aprobada shouldBe true
        revision.captured.revisadoPor shouldBe "jefatura"
        verify { solicitudDao.insertarOActualizar(match { it.estado == "APROBADA" }) }
        verify(exactly = 0) { solicitudDao.revisar(any(), any(), any(), any()) }
    }

    @Test
    fun `revisar ya revisada en el servidor devuelve su mensaje`() = runTest {
        coEvery { apiSolicitudes.revisar(7L, any()) } throws errorHttp(409, "La solicitud ya fue revisada (APROBADA).")

        repository.revisarSolicitud(7L, aprobada = false, revisadoPor = "jefatura")
            .exceptionOrNull()?.message shouldContain "ya fue revisada"
    }

    @Test
    fun `revisar sin conexion la deja revisada localmente`() = runTest {
        coEvery { apiSolicitudes.revisar(any(), any()) } throws IOException("sin internet")
        every { solicitudDao.revisar(7L, "RECHAZADA", "jefatura", any()) } returns 1

        repository.revisarSolicitud(7L, aprobada = false, revisadoPor = "jefatura").isSuccess shouldBe true
    }

    @Test
    fun `revisar una solicitud creada sin internet no llama al servidor`() = runTest {
        every { solicitudDao.revisar(-5L, "APROBADA", "jefatura", any()) } returns 1

        repository.revisarSolicitud(-5L, aprobada = true, revisadoPor = "jefatura").isSuccess shouldBe true

        coVerify(exactly = 0) { apiSolicitudes.revisar(any(), any()) }
    }

    @Test
    fun `revisar localmente una solicitud ya revisada falla`() = runTest {
        every { solicitudDao.revisar(-5L, any(), any(), any()) } returns 0

        repository.revisarSolicitud(-5L, aprobada = true, revisadoPor = "jefatura").isFailure shouldBe true
    }

    // ---------- Sincronización ----------

    @Test
    fun `sincronizar envia pendientes y reemplaza la bandeja con la del servidor`() = runTest {
        val nuevaOffline = SolicitudCamionEntity(id = -1L, siloId = "SIL-001", kgSolicitados = 500.0, urgente = false, solicitadoPor = "supervisor")
        val revisadaOffline = SolicitudCamionEntity(
            id = 3L, siloId = "SIL-001", kgSolicitados = 200.0, urgente = false, solicitadoPor = "supervisor",
            estado = "RECHAZADA", revisadoPor = "jefatura", fechaRevision = 2000L
        )
        every { solicitudDao.obtenerPendientesSincronizar() } returns listOf(nuevaOffline, revisadaOffline)
        coEvery { apiSolicitudes.crear(any()) } returns dto(id = 10)
        coEvery { apiSolicitudes.revisar(3L, any()) } returns dto(id = 3, estado = "RECHAZADA")
        // El servidor devuelve una solicitud de un silo que no existe en Room: no se debe guardar.
        coEvery { apiSolicitudes.listar() } returns listOf(dto(id = 10), dto(id = 3, estado = "RECHAZADA"), dto(id = 99, siloId = "SIL-XXX"))
        every { siloDao.obtenerTodosDirecto() } returns listOf(silo)
        val guardadas = slot<List<SolicitudCamionEntity>>()
        every { solicitudDao.insertarTodas(capture(guardadas)) } returns Unit

        repository.sincronizarSolicitudes().getOrThrow() shouldBe 2

        coVerify { apiSolicitudes.revisar(3L, RevisionRequestDto(false, "jefatura", 2000L)) }
        verify { solicitudDao.eliminarTodas() }
        guardadas.captured.map { it.id } shouldBe listOf(10L, 3L)
    }

    @Test
    fun `sincronizar descarta la pendiente que el servidor rechaza`() = runTest {
        val nuevaOffline = SolicitudCamionEntity(id = -1L, siloId = "SIL-001", kgSolicitados = 500.0, urgente = false, solicitadoPor = "supervisor")
        every { solicitudDao.obtenerPendientesSincronizar() } returns listOf(nuevaOffline)
        coEvery { apiSolicitudes.crear(any()) } throws errorHttp(409, "Ya hay un camión pendiente")
        coEvery { apiSolicitudes.listar() } returns emptyList()
        every { siloDao.obtenerTodosDirecto() } returns listOf(silo)

        repository.sincronizarSolicitudes().getOrThrow() shouldBe 0
    }

    @Test
    fun `sincronizar sin conexion falla y no borra la copia local`() = runTest {
        every { solicitudDao.obtenerPendientesSincronizar() } returns emptyList()
        coEvery { apiSolicitudes.listar() } throws IOException("sin internet")

        repository.sincronizarSolicitudes().isFailure shouldBe true

        verify(exactly = 0) { solicitudDao.eliminarTodas() }
    }
}