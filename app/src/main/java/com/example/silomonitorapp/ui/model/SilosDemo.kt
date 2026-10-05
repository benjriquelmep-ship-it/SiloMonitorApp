package com.example.silomonitorapp.ui.model

import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.snapshots.SnapshotStateList

/**
 * Fuente de datos TEMPORAL para desarrollar y probar la interfaz
 * mientras se integra Room + ViewModel (Integrante 1).
 * Reemplazar por el estado expuesto por el ViewModel.
 */
class SilosDemoStore {

    val silos: SnapshotStateList<SiloUi> = mutableStateListOf(*silosDemo.toTypedArray())

    fun buscar(id: String): SiloUi? = silos.firstOrNull { it.id == id }

    /** Devuelve un mensaje de error o null si el movimiento se aplicó. */
    fun aplicarMovimiento(id: String, tipo: TipoMovimiento, kg: Double): String? {
        val indice = silos.indexOfFirst { it.id == id }
        if (indice < 0) return "Silo no encontrado"
        val silo = silos[indice]
        val nuevoStock = when (tipo) {
            TipoMovimiento.CARGA -> silo.stockActualKg + kg
            TipoMovimiento.CONSUMO -> silo.stockActualKg - kg
        }
        if (nuevoStock > silo.capacidadMaxKg) return "La carga supera la capacidad máxima del silo"
        if (nuevoStock < 0) return "El consumo supera el stock disponible"
        silos[indice] = silo.copy(stockActualKg = nuevoStock)
        return null
    }
}

val silosDemo = listOf(
    SiloUi("S-001", "SIL-001", "Silo Norte A", "Granja El Paico", "Galpón 1",
        30000.0, 21500.0, 3200.0, -33.7012, -71.0034),
    SiloUi("S-002", "SIL-002", "Silo Norte B", "Granja El Paico", "Galpón 2",
        30000.0, 9800.0, 3000.0, -33.7031, -71.0071),
    SiloUi("S-003", "SIL-003", "Silo Sur A", "Granja El Paico", "Galpón 3",
        25000.0, 3900.0, 2800.0, -33.7058, -71.0010),
    SiloUi("S-004", "SIL-004", "Silo Principal", "Granja Pomaire", "Galpón 1",
        40000.0, 31000.0, 4100.0, -33.6489, -71.1612),
    SiloUi("S-005", "SIL-005", "Silo Auxiliar", "Granja Pomaire", "Galpón 2",
        20000.0, 3500.0, 2500.0, -33.6512, -71.1655),
    SiloUi("S-006", "SIL-006", "Silo Central", "Granja Melipilla", "Galpón 4",
        35000.0, 12600.0, 3600.0, -33.6875, -71.2148),
)
