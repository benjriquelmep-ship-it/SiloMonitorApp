package com.example.silomonitorapp.data.remote

import android.content.Context
import com.example.silomonitorapp.data.local.AppDatabase
import com.example.silomonitorapp.data.local.MovimientoEntity
import com.example.silomonitorapp.data.local.SolicitudCamionEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

data class SyncResult(
    val exitoso: Boolean,
    val movimientosSincronizados: Int = 0,
    val solicitudesSincronizadas: Int = 0,
    val error: String? = null
)

class SyncRepository(private val context: Context) {

    private val db = AppDatabase.obtenerBaseDatos(context)
    private val movimientoDao = db.movimientoDao()
    private val solicitudDao = db.solicitudCamionDao()

    // Obtiene Firestore solo si FirebaseApp fue inicializado, sin crashear la app
    private fun obtenerFirestore(): FirebaseFirestore? {
        return try {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        } catch (e: Exception) {
            null
        }
    }

    suspend fun sincronizarTodo(): SyncResult = withContext(Dispatchers.IO) {
        try {
            val firestore = obtenerFirestore()
                ?: return@withContext SyncResult(
                    exitoso = false,
                    error = "Firebase no está inicializado o falta google-services.json"
                )

            val movPendientes: List<MovimientoEntity> = movimientoDao.obtenerPendientesSincronizar()
            val solPendientes: List<SolicitudCamionEntity> = solicitudDao.obtenerPendientesSincronizar()

            if (movPendientes.isEmpty() && solPendientes.isEmpty()) {
                return@withContext SyncResult(exitoso = true, 0, 0)
            }

            val idsMovExitosos = mutableListOf<Long>()
            for (mov: MovimientoEntity in movPendientes) {
                val data: Map<String, Any?> = mapOf(
                    "idLocal" to mov.id,
                    "siloId" to mov.siloId,
                    "tipo" to mov.tipo,
                    "cantidadKg" to mov.cantidadKg,
                    "fecha" to mov.fecha,
                    "observacion" to mov.observacion,
                    "fotoUri" to mov.fotoUri
                )
                firestore.collection("movimientos")
                    .document("mov_${mov.siloId}_${mov.id}")
                    .set(data)
                    .await()
                idsMovExitosos.add(mov.id)
            }

            if (idsMovExitosos.isNotEmpty()) {
                movimientoDao.marcarComoSincronizados(idsMovExitosos)
            }

            val idsSolExitosos = mutableListOf<Long>()
            for (sol: SolicitudCamionEntity in solPendientes) {
                val data: Map<String, Any?> = mapOf(
                    "idLocal" to sol.id,
                    "siloId" to sol.siloId,
                    "kgSolicitados" to sol.kgSolicitados,
                    "urgente" to sol.urgente,
                    "observacion" to sol.observacion,
                    "solicitadoPor" to sol.solicitadoPor,
                    "fechaSolicitud" to sol.fechaSolicitud,
                    "estado" to sol.estado,
                    "revisadoPor" to sol.revisadoPor,
                    "fechaRevision" to sol.fechaRevision
                )
                firestore.collection("solicitudes_camion")
                    .document("sol_${sol.siloId}_${sol.id}")
                    .set(data)
                    .await()
                idsSolExitosos.add(sol.id)
            }

            if (idsSolExitosos.isNotEmpty()) {
                solicitudDao.marcarComoSincronizadas(idsSolExitosos)
            }

            SyncResult(
                exitoso = true,
                movimientosSincronizados = idsMovExitosos.size,
                solicitudesSincronizadas = idsSolExitosos.size
            )
        } catch (e: Exception) {
            SyncResult(exitoso = false, error = e.localizedMessage ?: "Error al sincronizar")
        }
    }
}