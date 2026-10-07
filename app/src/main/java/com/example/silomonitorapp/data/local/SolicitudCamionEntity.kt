package com.example.silomonitorapp.data.local

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** Solicitud de camión de reposición: la pide el Supervisor y la aprueba la Jefatura. */
@Entity(
    tableName = "solicitudes_camion",
    foreignKeys = [ForeignKey(
        entity = SiloEntity::class,
        parentColumns = ["id"],
        childColumns = ["siloId"],
        onDelete = ForeignKey.CASCADE
    )],
    indices = [Index("siloId")]
)
data class SolicitudCamionEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val siloId: String,
    val kgSolicitados: Double,
    val urgente: Boolean,
    val observacion: String = "",
    val solicitadoPor: String,
    val fechaSolicitud: Long = System.currentTimeMillis(),
    val estado: String = ESTADO_PENDIENTE, // PENDIENTE, APROBADA o RECHAZADA
    val revisadoPor: String? = null,
    val fechaRevision: Long? = null,
    // Offline-first: queda pendiente hasta que se transmita al servidor central
    val pendienteSincronizar: Boolean = true
) {
    companion object {
        const val ESTADO_PENDIENTE = "PENDIENTE"
        const val ESTADO_APROBADA = "APROBADA"
        const val ESTADO_RECHAZADA = "RECHAZADA"
    }
}
