package com.example.silomonitorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// DAO de la tabla 'solicitudes_camion' (reposición de alimento).
@Dao
interface SolicitudCamionDao {

    // Guarda una solicitud nueva (queda PENDIENTE).
    @Insert
    fun insertar(solicitud: SolicitudCamionEntity): Long

    // Primero las pendientes (urgentes arriba), luego las ya revisadas
    @Query(
        """
        SELECT * FROM solicitudes_camion
        ORDER BY CASE estado WHEN 'PENDIENTE' THEN 0 ELSE 1 END, urgente DESC, fechaSolicitud DESC
        """
    )
    fun obtenerTodas(): Flow<List<SolicitudCamionEntity>>

    // Cuántas solicitudes pendientes tiene un silo (para no pedir dos camiones a la vez).
    @Query("SELECT COUNT(*) FROM solicitudes_camion WHERE siloId = :siloId AND estado = 'PENDIENTE'")
    fun contarPendientesDeSilo(siloId: String): Int

    @Query(
        """
        UPDATE solicitudes_camion
        SET estado = :estado, revisadoPor = :revisadoPor, fechaRevision = :fecha, pendienteSincronizar = 1
        WHERE id = :id AND estado = 'PENDIENTE'
        """
    )
    fun revisar(id: Long, estado: String, revisadoPor: String, fecha: Long): Int

    // Solicitudes que todavía no se suben a la nube.
    @Query("SELECT * FROM solicitudes_camion WHERE pendienteSincronizar = 1")
    fun obtenerPendientesSincronizar(): List<SolicitudCamionEntity>

    // Marca como enviadas las solicitudes que ya se subieron a Firestore.
    @Query("UPDATE solicitudes_camion SET pendienteSincronizar = 0 WHERE id IN (:ids)")
    fun marcarComoSincronizadas(ids: List<Long>): Int
}
