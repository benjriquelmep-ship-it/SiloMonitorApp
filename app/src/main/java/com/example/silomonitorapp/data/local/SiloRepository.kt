package com.example.silomonitorapp.data.local

import com.example.silomonitorapp.data.remote.ErroresApi
import com.example.silomonitorapp.data.remote.MovimientoRequestDto
import com.example.silomonitorapp.data.remote.RetrofitClient
import com.example.silomonitorapp.data.remote.RevisionRequestDto
import com.example.silomonitorapp.data.remote.SiloApiService
import com.example.silomonitorapp.data.remote.SolicitudApiService
import com.example.silomonitorapp.data.remote.aDto
import com.example.silomonitorapp.data.remote.aEntidad
import com.example.silomonitorapp.data.remote.aRequest
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
//  - api            (Retrofit -> microservicio ms-silos): fuente OFICIAL de los silos y movimientos.
//  - apiSolicitudes (Retrofit -> microservicio ms-solicitudes): fuente OFICIAL de las solicitudes de camión.
//  - db             (Room): copia local que alimenta la UI y permite seguir trabajando sin internet.
// Las APIs llegan por constructor para poder reemplazarlas por mocks en las pruebas unitarias.
class SiloRepository(
    private val db: AppDatabase,
    private val api: SiloApiService = RetrofitClient.siloApi,
    private val apiSolicitudes: SolicitudApiService = RetrofitClient.solicitudApi
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
        // 4) Se envía al microservicio ms-solicitudes (POST /api/solicitudes).
        //    - Si responde OK: se guarda en Room con el id del servidor (ya sincronizada).
        //    - Si el servidor la rechaza (HttpException 409): se muestra su mensaje y no se guarda.
        //    - Si no hay conexión (IOException): se guarda local con id NEGATIVO y queda pendiente.
        //      El id negativo evita chocar con los ids del servidor hasta que se sincronice.
        val fecha = System.currentTimeMillis()
        val nueva = SolicitudCamionEntity(
            id = -fecha,
            siloId = siloId,
            kgSolicitados = kgSolicitados,
            urgente = urgente,
            observacion = observacion,
            solicitadoPor = solicitadoPor,
            fechaSolicitud = fecha
        )
        val creada = try {
            apiSolicitudes.crear(nueva.aRequest())
        } catch (e: HttpException) {
            return@withContext Result.failure(Exception(ErroresApi.mensaje(e)))
        } catch (e: IOException) {
            null
        }
        solicitudDao.insertarOActualizar(creada?.aEntidad() ?: nueva)
        Result.success(Unit)
    }

    // La Jefatura aprueba o rechaza una solicitud pendiente
    suspend fun revisarSolicitud(id: Long, aprobada: Boolean, revisadoPor: String): Result<Unit> = withContext(Dispatchers.IO) {
        val fecha = System.currentTimeMillis()
        // 1) Si la solicitud ya existe en el servidor (id positivo), se revisa allá
        //    (PATCH /api/solicitudes/{id}/revision) y se guarda la respuesta en Room.
        if (id > 0) {
            try {
                val revisada = apiSolicitudes.revisar(id, RevisionRequestDto(aprobada, revisadoPor, fecha))
                solicitudDao.insertarOActualizar(revisada.aEntidad())
                return@withContext Result.success(Unit)
            } catch (e: HttpException) {
                // Ej: otro usuario ya la revisó (409).
                return@withContext Result.failure(Exception(ErroresApi.mensaje(e)))
            } catch (e: IOException) {
                // Sin conexión: se revisa localmente y se envía en la próxima sincronización.
            }
        }
        // 2) Revisión local: solo cambia solicitudes PENDIENTES y la deja pendiente de sincronizar.
        val estado = if (aprobada) SolicitudCamionEntity.ESTADO_APROBADA else SolicitudCamionEntity.ESTADO_RECHAZADA
        val filas = solicitudDao.revisar(id, estado, revisadoPor, fecha)
        if (filas == 0) Result.failure(Exception("La solicitud ya fue revisada.")) else Result.success(Unit)
    }

    // SINCRONIZACIÓN con ms-solicitudes:
    // 1) Envía lo hecho sin internet: solicitudes nuevas (id negativo) y revisiones pendientes.
    // 2) Descarga la bandeja oficial del servidor y reemplaza la copia de Room.
    // Devuelve cuántas solicitudes pendientes se enviaron.
    suspend fun sincronizarSolicitudes(): Result<Int> = withContext(Dispatchers.IO) {
        llamarApi {
            var enviadas = 0
            for (local in solicitudDao.obtenerPendientesSincronizar()) {
                try {
                    // Nueva sin internet: se crea en el servidor y se usa el id que devuelve.
                    val idServidor = if (local.id < 0) apiSolicitudes.crear(local.aRequest()).id else local.id
                    // Revisada sin internet: se envía la aprobación o el rechazo.
                    if (local.estado != SolicitudCamionEntity.ESTADO_PENDIENTE) {
                        apiSolicitudes.revisar(
                            idServidor,
                            RevisionRequestDto(
                                aprobada = local.estado == SolicitudCamionEntity.ESTADO_APROBADA,
                                revisadoPor = local.revisadoPor.orEmpty(),
                                fechaRevision = local.fechaRevision
                            )
                        )
                    }
                    enviadas++
                } catch (e: HttpException) {
                    // El servidor la rechazó (ej: ya había un camión pendiente para ese silo).
                    // Se descarta; la bandeja oficial llega en el paso 2.
                }
            }

            // Solo se guardan solicitudes de silos que existen en Room (la tabla tiene clave foránea).
            val silosLocales = siloDao.obtenerTodosDirecto().map { it.id }.toSet()
            val servidor = apiSolicitudes.listar()                    // GET /api/solicitudes
                .map { it.aEntidad() }
                .filter { it.siloId in silosLocales }
            db.runInTransaction(Runnable {
                solicitudDao.eliminarTodas()
                solicitudDao.insertarTodas(servidor)
            })
            enviadas
        }
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