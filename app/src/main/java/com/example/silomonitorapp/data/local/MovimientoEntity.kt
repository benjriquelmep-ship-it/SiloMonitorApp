package com.example.silomonitorapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Registro de cada Carga o Consumo hecho en terreno (historial y auditoría). */
@Entity(
    tableName = "movimientos",
    foreignKeys = [ForeignKey(
        entity = SiloEntity::class,
        parentColumns = ["id"],
        childColumns = ["siloId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("siloId")]
)
data class MovimientoEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siloId: String,
    val tipo: String, // "CARGA" o "CONSUMO"
    val cantidadKg: Double,
    val fecha: Long = System.currentTimeMillis(),
    val observacion: String = "",
    val fotoUri: String? = null, // Evidencia visual opcional
    // Offline-first: queda pendiente hasta que se transmita al servidor central
    val pendienteSincronizar: Boolean = true
)
