package com.example.silomonitorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface SiloDao {

    @Query("SELECT * FROM silos ORDER BY (nivelActual / capacidadMaxima) ASC")
    fun obtenerTodosLosSilos(): Flow<List<SiloEntity>>

    @Query("SELECT * FROM silos WHERE id = :id LIMIT 1")
    fun obtenerSiloPorId(id: String): SiloEntity?

    @Query("SELECT COUNT(*) FROM silos")
    fun contarSilos(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarOActualizarSilos(silos: List<SiloEntity>): List<Long>

    // ABORT: falla si el código ya existe, para no sobrescribir un silo (ni su historial)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertarSilo(silo: SiloEntity): Long

    @Update
    fun actualizarSilo(silo: SiloEntity): Int

    @Query("UPDATE silos SET nivelActual = :nuevoNivel WHERE id = :id")
    fun actualizarNivel(id: String, nuevoNivel: Double): Int

    @Query("SELECT * FROM silos")
    fun obtenerTodosDirecto(): List<SiloEntity>

}