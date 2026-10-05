package com.example.silomonitorapp.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "silos")
data class SiloEntity(
    @PrimaryKey
    val id: String,                 // Código único (ej: "SILO-01")
    val nombre: String,             // Nombre del silo (ej: "Silo Pabellón A")
    val tipoAlimento: String,       // Ej: "Engorda", "Inicio", "Terminación"
    val capacidadMaxima: Double,    // Capacidad total en kg (ej: 10000.0)
    val nivelActual: Double,        // Nivel actual en kg (ej: 4500.0)
    val latitud: Double,            // Coordenada latitud
    val longitud: Double,           // Coordenada longitud
    val ultimaActualizacion: Long = System.currentTimeMillis()
)