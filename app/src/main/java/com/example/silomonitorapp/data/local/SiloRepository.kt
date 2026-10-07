package com.example.silomonitorapp.data.local

import com.example.silomonitorapp.domain.ReglasTerreno
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SiloRepository(private val db: AppDatabase) {

    private val siloDao = db.siloDao()
    private val movimientoDao = db.movimientoDao()
    private val solicitudDao = db.solicitudCamionDao()

    val silosFlow: Flow<List<SiloEntity>> = siloDao.obtenerTodosLosSilos()

    fun historialFlow(siloId: String): Flow<List<MovimientoEntity>> = movimientoDao.obtenerPorSilo(siloId)

    fun movimientosDesdeFlow(desde: Long): Flow<List<MovimientoEntity>> = movimientoDao.obtenerDesde(desde)

    val solicitudesFlow: Flow<List<SolicitudCamionEntity>> = solicitudDao.obtenerTodas()

    // Coordinar reposición: el Supervisor pide un camión para un silo
    suspend fun solicitarCamion(
        siloId: String,
        kgSolicitados: Double,
        urgente: Boolean,
        observacion: String,
        solicitadoPor: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        val silo = siloDao.obtenerSiloPorId(siloId)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))
        ReglasTerreno.validarSolicitudCamion(silo.nivelActual, silo.capacidadMaxima, kgSolicitados)
            ?.let { return@withContext Result.failure(Exception(it)) }
        if (solicitudDao.contarPendientesDeSilo(siloId) > 0) {
            return@withContext Result.failure(Exception("Ya hay un camión pendiente de aprobación para este silo."))
        }
        solicitudDao.insertar(
            SolicitudCamionEntity(
                siloId = siloId,
                kgSolicitados = kgSolicitados,
                urgente = urgente,
                observacion = observacion,
                solicitadoPor = solicitadoPor
            )
        )
        Result.success(Unit)
    }

    // La Jefatura aprueba o rechaza una solicitud pendiente
    suspend fun revisarSolicitud(id: Long, aprobada: Boolean, revisadoPor: String): Result<Unit> = withContext(Dispatchers.IO) {
        val estado = if (aprobada) SolicitudCamionEntity.ESTADO_APROBADA else SolicitudCamionEntity.ESTADO_RECHAZADA
        val filas = solicitudDao.revisar(id, estado, revisadoPor, System.currentTimeMillis())
        if (filas == 0) Result.failure(Exception("La solicitud ya fue revisada.")) else Result.success(Unit)
    }

    suspend fun obtenerSiloPorId(id: String): SiloEntity? = withContext(Dispatchers.IO) {
        siloDao.obtenerSiloPorId(id)
    }

    // Reglas de negocio de terreno (Ariztía)
    // Devuelve el silo con su nivel actualizado
    suspend fun registrarMovimiento(
        idSilo: String,
        cantidadKg: Double,
        esCarga: Boolean,
        observacion: String = "",
        fotoUri: String? = null
    ): Result<SiloEntity> = withContext(Dispatchers.IO) {
        val silo = siloDao.obtenerSiloPorId(idSilo)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))

        val nuevoNivel = ReglasTerreno.calcularNuevoNivel(silo.nivelActual, silo.capacidadMaxima, cantidadKg, esCarga)
            .getOrElse { return@withContext Result.failure(it) }

        // Nivel e historial se guardan juntos: o quedan ambos o ninguno
        db.runInTransaction {
            siloDao.actualizarNivel(idSilo, nuevoNivel)
            movimientoDao.insertar(
                MovimientoEntity(
                    siloId = idSilo,
                    tipo = if (esCarga) "CARGA" else "CONSUMO",
                    cantidadKg = cantidadKg,
                    observacion = observacion,
                    fotoUri = fotoUri
                )
            )
        }
        Result.success(silo.copy(nivelActual = nuevoNivel))
    }

    // Alta manual de un silo nuevo (respaldo ante fallas del QR y alta de infraestructura)
    suspend fun crearSilo(silo: SiloEntity): Result<Unit> = withContext(Dispatchers.IO) {
        if (siloDao.obtenerSiloPorId(silo.id) != null) {
            return@withContext Result.failure(Exception("Ya existe un silo con el código ${silo.id}."))
        }
        siloDao.insertarSilo(silo)
        Result.success(Unit)
    }

    // Edita los datos maestros; el stock se conserva porque solo cambia con movimientos
    suspend fun editarSilo(silo: SiloEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val actual = siloDao.obtenerSiloPorId(silo.id)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))
        if (actual.nivelActual > silo.capacidadMaxima) {
            return@withContext Result.failure(Exception("La capacidad no puede ser menor al stock actual (${actual.nivelActual.toInt()} kg)."))
        }
        siloDao.actualizarSilo(silo.copy(nivelActual = actual.nivelActual))
        Result.success(Unit)
    }

    // Datos semilla iniciales coincidentes con la UI de Ariztía (cada silo con su propia ubicación)
    suspend fun precargarSilosSiEstaVacio() = withContext(Dispatchers.IO) {
        if (siloDao.contarSilos() > 0) return@withContext

        val silosIniciales = listOf(
            SiloEntity("SIL-001", "Silo Norte A", "Granja El Paico", "Galpón 1", "Engorda Inicial", 30000.0, 21500.0, 2000.0, -33.7012, -71.0034),
            SiloEntity("SIL-002", "Silo Norte B", "Granja El Paico", "Galpón 2", "Engorda Concentrado", 30000.0, 9800.0, 2200.0, -33.7031, -71.0071),
            SiloEntity("SIL-003", "Silo Sur A", "Granja El Paico", "Galpón 3", "Engorda Pollos", 25000.0, 3900.0, 1800.0, -33.7058, -71.0010),
            SiloEntity("SIL-004", "Silo Principal", "Granja Pomaire", "Galpón 1", "Ponedora Fase 1", 28000.0, 22000.0, 1900.0, -33.6489, -71.1612),
            SiloEntity("SIL-005", "Silo Auxiliar", "Granja Pomaire", "Galpón 2", "Iniciador Pollitas", 20000.0, 3500.0, 1600.0, -33.6512, -71.1655),
            SiloEntity("SIL-006", "Silo Central", "Granja Melipilla", "Galpón 4", "Terminación Plus", 35000.0, 12600.0, 2400.0, -33.6875, -71.2148)
        )
        siloDao.insertarOActualizarSilos(silosIniciales)
    }
}