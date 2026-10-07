package com.example.silomonitorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface MovimientoDao {

    @Insert
    fun insertar(movimiento: MovimientoEntity): Long

    @Query("SELECT * FROM movimientos WHERE siloId = :siloId ORDER BY fecha DESC LIMIT :limite")
    fun obtenerPorSilo(siloId: String, limite: Int = 20): Flow<List<MovimientoEntity>>

    // Para el dashboard: todos los movimientos desde una fecha
    @Query("SELECT * FROM movimientos WHERE fecha >= :desde ORDER BY fecha ASC")
    fun obtenerDesde(desde: Long): Flow<List<MovimientoEntity>>

    @Query("SELECT * FROM movimientos WHERE pendienteSincronizar = 1")
    fun obtenerPendientesSincronizar(): List<MovimientoEntity>

    @Query("UPDATE movimientos SET pendienteSincronizar = 0 WHERE id IN (:ids)")
    fun marcarComoSincronizados(ids: List<Long>): Int
}
