package com.example.silomonitorapp.data.remote

import android.content.Context
import com.example.silomonitorapp.data.local.MovimientoEntity
import com.example.silomonitorapp.data.local.SiloEntity
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Repositorio remoto encargado de sincronizar silos y movimientos con Firestore.
 * Diseñado con tolerancia a fallos offline y configuración diferida de Firebase.
 */
class SyncRepository(private val context: Context) {

    // Inicialización perezosa y segura: no crashea si Firebase tarda o está sin red
    private val db: FirebaseFirestore? by lazy {
        runCatching {
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }
            FirebaseFirestore.getInstance()
        }.getOrNull()
    }

    /** Sube o actualiza la información de un silo en la nube */
    suspend fun sincronizarSilo(silo: SiloEntity): Result<Unit> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.failure(Exception("Firebase no está inicializado"))
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

    /** Sube los movimientos pendientes de sincronización */
    suspend fun sincronizarMovimientos(movimientos: List<MovimientoEntity>): Result<List<Long>> = withContext(Dispatchers.IO) {
        val firestore = db ?: return@withContext Result.failure(Exception("Firebase no está inicializado"))
        if (movimientos.isEmpty()) return@withContext Result.success(emptyList())

        runCatching {
            val sincronizadosIds = mutableListOf<Long>()
            for (mov in movimientos) {
                val datos = hashMapOf(
                    "siloId" to mov.siloId,
                    "tipo" to mov.tipo,
                    "cantidadKg" to mov.cantidadKg,
                    "fecha" to mov.fecha,
                    "observacion" to mov.observacion,
                    "fotoUri" to (mov.fotoUri ?: "")
                )
                firestore.collection("movimientos")
                    .document("${mov.siloId}_${mov.fecha}")
                    .set(datos)
                    .await()
                sincronizadosIds.add(mov.id)
            }
            sincronizadosIds
        }
    }
}