package com.example.silomonitorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiloDao {

    // Monitoreo en tiempo real de todos los silos
    @Query("SELECT * FROM silos ORDER BY nombre ASC")
    fun obtenerTodosLosSilos(): Flow<List<SiloEntity>>

    // Buscar silo específico por ID
    @Query("SELECT * FROM silos WHERE id = :idSilo LIMIT 1")
    suspend fun obtenerSiloPorId(idSilo: String): SiloEntity?

    // Precargar o reemplazar lotes de silos
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSilos(silos: List<SiloEntity>)

    // Insertar un silo individual
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertarSilo(silo: SiloEntity)

    // Actualizar silo completo
    @Update
    suspend fun actualizarSilo(silo: SiloEntity)

    // Registrar carga o consumo actualizando stock en kg
    @Query("UPDATE silos SET nivelActual = :nuevoNivel, ultimaActualizacion = :fecha WHERE id = :idSilo")
    suspend fun actualizarNivel(idSilo: String, nuevoNivel: Double, fecha: Long = System.currentTimeMillis())
}