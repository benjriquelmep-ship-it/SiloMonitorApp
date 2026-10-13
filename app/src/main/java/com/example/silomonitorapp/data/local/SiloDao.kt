package com.example.silomonitorapp.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

// DAO (Data Access Object): las consultas SQL de la tabla 'silos'.
// Room genera el código de estas funciones automáticamente.
@Dao
interface SiloDao {

    // Todos los silos, del más vacío al más lleno. Devuelve un Flow:
    // la UI se actualiza sola cada vez que la tabla cambia.
    @Query("SELECT * FROM silos ORDER BY (nivelActual / capacidadMaxima) ASC")
    fun obtenerTodosLosSilos(): Flow<List<SiloEntity>>

    // Busca un silo por su código (null si no existe).
    @Query("SELECT * FROM silos WHERE id = :id LIMIT 1")
    fun obtenerSiloPorId(id: String): SiloEntity?

    // Cuántos silos hay (se usa para saber si hay que cargar los datos iniciales).
    @Query("SELECT COUNT(*) FROM silos")
    fun contarSilos(): Int

    // Inserta varios silos; si el código ya existe, lo reemplaza (solo para los datos iniciales).
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun insertarOActualizarSilos(silos: List<SiloEntity>): List<Long>

    // ABORT: falla si el código ya existe, para no sobrescribir un silo (ni su historial)
    @Insert(onConflict = OnConflictStrategy.ABORT)
    fun insertarSilo(silo: SiloEntity): Long

    // Actualiza los datos de un silo existente (edición desde el formulario).
    @Update
    fun actualizarSilo(silo: SiloEntity): Int

    // Cambia solo el stock del silo (después de una carga o un consumo).
    @Query("UPDATE silos SET nivelActual = :nuevoNivel WHERE id = :id")
    fun actualizarNivel(id: String, nuevoNivel: Double): Int

    // Lista simple (sin Flow) para subirla a Firestore al sincronizar.
    @Query("SELECT * FROM silos")
    fun obtenerTodosDirecto(): List<SiloEntity>

}