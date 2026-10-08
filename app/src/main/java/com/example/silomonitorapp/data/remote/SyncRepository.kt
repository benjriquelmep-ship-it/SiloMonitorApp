package com.example.silomonitorapp.data.remote

import android.content.Context
import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.MovimientoEntity
import com.example.silomonitorapp.data.local.SiloEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Modelo de resultado esperado por SiloViewModel tras el proceso de sincronización.
 */
data class ResultadoSync(
    val exitoso: Boolean,
    val movimientosSincronizados: Int = 0,
    val solicitudesSincronizadas: Int = 0,
    val error: String? = null
)

class SyncRepository(private val context: Context) {

    private val db: FirebaseFirestore? by lazy {
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        }.getOrNull()
    }

    private val localDb by lazy { AppDatabase.obtenerBaseDatos(context) }

    /**
     * Sincronización general llamada directamente desde SiloViewModel sin argumentos.
     * Lee pendientes locales, actualiza Firestore y marca los registros sincronizados.
     */
    suspend fun sincronizarTodo(): ResultadoSync = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext ResultadoSync(
            exitoso = false,
            error = "Firebase no está inicializado o no hay conexión con el servicio."
        )

        runCatching {
            var totalMovs = 0
            val totalSols = 0

            // 1. Obtener y subir silos actuales
            val silos = localDb.siloDao().obtenerTodosDirecto()
            for (silo in silos) {
                val datosSilo = hashMapOf(
                    "id" to silo.id,
                    "nombre" to silo.nombre,
                    "granja" to silo.granja,
                    "galpon" to silo.galpon,
                    "tipoAlimento" to silo.tipoAlimento,
                    "capacidadMaxima" to silo.capacidadMaxima,
                    "nivelActual" to silo.nivelActual,
                    "consumoPromedioDiario" to silo.consumoPromedioDiario,
                    "latitud" to silo.latitud,
                    "longitud" to silo.longitud,
                    "ultimaActualizacion" to System.currentTimeMillis()
                )
                firestore.collection("silos")
                    .document(silo.id)
                    .set(datosSilo, SetOptions.merge())
                    .await()
            }

            // 2. Obtener movimientos pendientes de sincronizar
            val pendientes = localDb.movimientoDao().obtenerPendientesSincronizar()
            if (pendientes.isNotEmpty()) {
                val idsExitosos = mutableListOf<Long>()
                for (mov in pendientes) {
                    val datosMov = hashMapOf(
                        "siloId" to mov.siloId,
                        "tipo" to mov.tipo,
                        "cantidadKg" to mov.cantidadKg,
                        "fecha" to mov.fecha,
                        "observacion" to mov.observacion,
                        "fotoUri" to (mov.fotoUri ?: "")
                    )
                    firestore.collection("movimientos")
                        .document("${mov.siloId}_${mov.fecha}")
                        .set(datosMov)
                        .await()
                    idsExitosos.add(mov.id)
                }
                localDb.movimientoDao().marcarComoSincronizados(idsExitosos)
                totalMovs = idsExitosos.size
            }

            ResultadoSync(
                exitoso = true,
                movimientosSincronizados = totalMovs,
                solicitudesSincronizadas = totalSols
            )
        }.getOrElse { exception ->
            ResultadoSync(
                exitoso = false,
                error = exception.localizedMessage ?: "Error desconocido durante la sincronización"
            )
        }
    }

    suspend fun sincronizarSilo(silo: SiloEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.failure(Exception("Firebase no inicializado"))
        runCatching {
            val datos = hashMapOf(
                "id" to silo.id,
                "nombre" to silo.nombre,
                "granja" to silo.granja,
                "galpon" to silo.galpon,
                "tipoAlimento" to silo.tipoAlimento,
                "capacidadMaxima" to silo.capacidadMaxima,
                "nivelActual" to silo.nivelActual,
                "consumoPromedioDiario" to silo.consumoPromedioDiario,
                "latitud" to silo.latitud,
                "longitud" to silo.longitud,
                "ultimaActualizacion" to System.currentTimeMillis()
            )
            firestore.collection("silos")
                .document(silo.id)
                .set(datos, SetOptions.merge())
                .await()
            Unit
        }
    }
}