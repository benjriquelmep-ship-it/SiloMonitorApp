package com.example.silomonitorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

// DAO de la tabla 'movimientos' (historial de cargas y consumos).
@Dao
interface MovimientoDao {

    // Guarda un movimiento nuevo y devuelve su id.
    @Insert
    fun insertar(movimiento: MovimientoEntity): Long

    // Últimos movimientos de un silo, del más reciente al más antiguo (ficha del silo).
    @Query("SELECT * FROM movimientos WHERE siloId = :siloId ORDER BY fecha DESC LIMIT :limite")
    fun obtenerPorSilo(siloId: String, limite: Int = 20): Flow<List<MovimientoEntity>>

    // Para el dashboard: todos los movimientos desde una fecha
    @Query("SELECT * FROM movimientos WHERE fecha >= :desde ORDER BY fecha ASC")
    fun obtenerDesde(desde: Long): Flow<List<MovimientoEntity>>

    // Movimientos que todavía no se suben a la nube.
    @Query("SELECT * FROM movimientos WHERE pendienteSincronizar = 1")
    fun obtenerPendientesSincronizar(): List<MovimientoEntity>

    // Marca como enviados los movimientos que ya se subieron a Firestore.
    @Query("UPDATE movimientos SET pendienteSincronizar = 0 WHERE id IN (:ids)")
    fun marcarComoSincronizados(ids: List<Long>): Int
}
