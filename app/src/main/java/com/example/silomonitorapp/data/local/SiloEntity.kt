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
    val consumoPromedioDiario: Double, // kg diarios para cálculo de autonomía
    // Sin valor por defecto: cada silo debe tener su propia ubicación en el mapa
    val latitud: Double,
    val longitud: Double
)
