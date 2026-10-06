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
}
