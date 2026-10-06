package com.example.silomonitorapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "silos")
data class SiloEntity(
    @PrimaryKey
    val id: String, // Ej: "SIL-001"
    val nombre: String,
    val granja: String,
    val galpon: String,
    val tipoAlimento: String,
    val capacidadMaxima: Double,
    val nivelActual: Double,
    val consumoPromedioDiario: Double = 1500.0, // kg diarios para cálculo de autonomía
    val latitud: Double = -33.6892,
    val longitud: Double = -71.2178
)