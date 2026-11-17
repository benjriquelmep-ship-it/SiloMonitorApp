package com.example.silomonitorapp.data.local

import com.example.silomonitorapp.data.remote.ErroresApi
import com.example.silomonitorapp.data.remote.MovimientoRequestDto
import com.example.silomonitorapp.data.remote.RetrofitClient
import com.example.silomonitorapp.data.remote.SiloApiService
import com.example.silomonitorapp.data.remote.aDto
import com.example.silomonitorapp.data.remote.aEntidad
import com.example.silomonitorapp.domain.ReglasTerreno
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import retrofit2.HttpException

// REPOSITORY (patrón MVVM): único punto de acceso a los datos de silos.
// El ViewModel nunca habla con Room ni con Retrofit directamente, siempre pasa por aquí.
// Todas las funciones 'suspend' corren en Dispatchers.IO para no congelar la pantalla.
//
// Fuentes de datos:
//  - api (Retrofit -> microservicio ms-silos): fuente OFICIAL de los silos y movimientos.
//  - db  (Room): copia local que alimenta la UI y permite seguir trabajando sin internet.
// El 'api' llega por constructor para poder reemplazarlo por un mock en las pruebas unitarias.
class SiloRepository(
    private val db: AppDatabase,
    private val api: SiloApiService = RetrofitClient.siloApi
) {

    // Los DAO de cada tabla (las consultas SQL).
    private val siloDao = db.siloDao()
    private val movimientoDao = db.movimientoDao()
    private val solicitudDao = db.solicitudCamionDao()

    // Flujo reactivo con todos los silos: cuando Room cambia, emite la lista nueva.
    val silosFlow: Flow<List<SiloEntity>> = siloDao.obtenerTodosLosSilos()

    // Historial de movimientos de un silo (para la ficha).
    fun historialFlow(siloId: String): Flow<List<MovimientoEntity>> = movimientoDao.obtenerPorSilo(siloId)

    // Movimientos desde una fecha (para el gráfico del dashboard).
    fun movimientosDesdeFlow(desde: Long): Flow<List<MovimientoEntity>> = movimientoDao.obtenerDesde(desde)

    // Todas las solicitudes de camión (para la bandeja de reposición).
    val solicitudesFlow: Flow<List<SolicitudCamionEntity>> = solicitudDao.obtenerTodas()

    // Coordinar reposición: el Supervisor pide un camión para un silo
    suspend fun solicitarCamion(
        siloId: String,
        kgSolicitados: Double,
        urgente: Boolean,
        observacion: String,
        solicitadoPor: String
    ): Result<Unit> = withContext(Dispatchers.IO) {
        // 1) El silo debe existir.
        val silo = siloDao.obtenerSiloPorId(siloId)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))
        // 2) Los kilos pedidos deben caber en el silo.
        ReglasTerreno.validarSolicitudCamion(silo.nivelActual, silo.capacidadMaxima, kgSolicitados)
            ?.let { return@withContext Result.failure(Exception(it)) }
        // 3) No se permite un segundo camión si ya hay uno pendiente.
        if (solicitudDao.contarPendientesDeSilo(siloId) > 0) {
            return@withContext Result.failure(Exception("Ya hay un camión pendiente de aprobación para este silo."))
        }
        // 4) Se guarda la solicitud (queda PENDIENTE y pendiente de sincronizar).
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
        // Solo cambia solicitudes PENDIENTES; si devuelve 0 filas, ya había sido revisada.
        val estado = if (aprobada) SolicitudCamionEntity.ESTADO_APROBADA else SolicitudCamionEntity.ESTADO_RECHAZADA
        val filas = solicitudDao.revisar(id, estado, revisadoPor, System.currentTimeMillis())
        if (filas == 0) Result.failure(Exception("La solicitud ya fue revisada.")) else Result.success(Unit)
    }

    // Busca un silo por código en un hilo de fondo.
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
        // 1) Busca el silo.
        val silo = siloDao.obtenerSiloPorId(idSilo)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))

        // 2) Valida localmente (respuesta inmediata, incluso sin internet).
        val nivelLocal = ReglasTerreno.calcularNuevoNivel(silo.nivelActual, silo.capacidadMaxima, cantidadKg, esCarga)
            .getOrElse { return@withContext Result.failure(it) }

        val tipo = if (esCarga) "CARGA" else "CONSUMO"
        val fecha = System.currentTimeMillis()

        // 3) Envía el movimiento al microservicio (POST /api/silos/{id}/movimientos).
        //    - Si responde OK: se usa el stock que calculó el servidor (fuente oficial).
        //    - Si el servidor lo rechaza (HttpException 409): se muestra su mensaje y no se guarda.
        //    - Si no hay conexión (IOException): se guarda local y queda PENDIENTE (offline-first).
        val siloServidor = try {
            api.registrarMovimiento(idSilo, MovimientoRequestDto(tipo, cantidadKg, observacion, fecha))
        } catch (e: HttpException) {
            return@withContext Result.failure(Exception(ErroresApi.mensaje(e)))
        } catch (e: IOException) {
            null
        }
        val nivelFinal = siloServidor?.nivelActual ?: nivelLocal

        // 4) Nivel e historial se guardan juntos en Room: o quedan ambos o ninguno.
        db.runInTransaction(Runnable {
            siloDao.actualizarNivel(idSilo, nivelFinal)
            movimientoDao.insertar(
                MovimientoEntity(
                    siloId = idSilo,
                    tipo = tipo,
                    cantidadKg = cantidadKg,
                    fecha = fecha,
                    observacion = observacion,
                    fotoUri = fotoUri,
                    pendienteSincronizar = siloServidor == null
                )
            )
        })
        // Devuelve el silo con el stock nuevo (el ViewModel lo usa para la alerta crítica).
        Result.success(silo.copy(nivelActual = nivelFinal))
    }

    // CREATE: alta de un silo nuevo. Primero se crea en el microservicio y luego se guarda en Room.
    suspend fun crearSilo(silo: SiloEntity): Result<Unit> = withContext(Dispatchers.IO) {
        // No se permiten códigos repetidos.
        if (siloDao.obtenerSiloPorId(silo.id) != null) {
            return@withContext Result.failure(Exception("Ya existe un silo con el código ${silo.id}."))
        }
        llamarApi {
            val creado = api.crearSilo(silo.aDto())        // POST /api/silos
            siloDao.insertarSilo(creado.aEntidad())
            Unit
        }
    }

    // UPDATE: edita los datos maestros; el stock se conserva porque solo cambia con movimientos
    suspend fun editarSilo(silo: SiloEntity): Result<Unit> = withContext(Dispatchers.IO) {
        // Se lee el silo actual para conservar su stock real.
        val actual = siloDao.obtenerSiloPorId(silo.id)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))
        if (actual.nivelActual > silo.capacidadMaxima) {
            return@withContext Result.failure(Exception("La capacidad no puede ser menor al stock actual (${actual.nivelActual.toInt()} kg)."))
        }
        llamarApi {
            val conStock = silo.copy(nivelActual = actual.nivelActual)
            val editado = api.editarSilo(silo.id, conStock.aDto())   // PUT /api/silos/{id}
            siloDao.actualizarSilo(editado.aEntidad())
            Unit
        }
    }

    // DELETE: elimina el silo en el microservicio y después en Room.
    suspend fun eliminarSilo(id: String): Result<Unit> = withContext(Dispatchers.IO) {
        llamarApi {
            val respuesta = api.eliminarSilo(id)                     // DELETE /api/silos/{id}
            // 404 = ya no existía en el servidor: igual se borra la copia local.
            if (!respuesta.isSuccessful && respuesta.code() != 404) throw HttpException(respuesta)
            siloDao.eliminarPorId(id)
            Unit
        }
    }

    // SINCRONIZACIÓN con ms-silos:
    // 1) Envía los movimientos hechos sin internet (pendientes).
    // 2) Descarga todos los silos del servidor y actualiza Room (READ).
    // Devuelve cuántos movimientos pendientes se enviaron.
    suspend fun sincronizarConServidor(): Result<Int> = withContext(Dispatchers.IO) {
        llamarApi {
            var enviados = 0
            for (mov in movimientoDao.obtenerPendientesSincronizar()) {
                try {
                    api.registrarMovimiento(
                        mov.siloId,
                        MovimientoRequestDto(mov.tipo, mov.cantidadKg, mov.observacion, mov.fecha)
                    )
                    enviados++
                } catch (e: HttpException) {
                    // El servidor lo rechazó (ej: ya no cabe porque otro usuario cargó antes).
                    // Se descarta para no bloquear la cola; el stock oficial llega en el paso 2.
                }
                // Se marca uno por uno: si se corta internet a la mitad, no se reenvían duplicados.
                movimientoDao.marcarComoSincronizados(listOf(mov.id))
            }

            val silosServidor = api.listarSilos().map { it.aEntidad() }   // GET /api/silos
            db.runInTransaction(Runnable {
                siloDao.upsertSilos(silosServidor)
                siloDao.eliminarAusentes(silosServidor.map { it.id })
            })
            enviados
        }
    }

    // Ejecuta una llamada al microservicio y convierte cualquier error en un mensaje legible.
    // 'inline' permite llamar funciones suspend dentro del bloque.
    private inline fun <T> llamarApi(bloque: () -> T): Result<T> =
        try {
            Result.success(bloque())
        } catch (e: CancellationException) {
            throw e // se respeta la cancelación de corrutinas
        } catch (e: Exception) {
            Result.failure(Exception(ErroresApi.mensaje(e)))
        }

    // Datos semilla iniciales coincidentes con la UI de Ariztía (cada silo con su propia ubicación)
    suspend fun precargarSilosSiEstaVacio() = withContext(Dispatchers.IO) {
        // Solo carga los 6 silos de ejemplo la primera vez (tabla vacía).
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