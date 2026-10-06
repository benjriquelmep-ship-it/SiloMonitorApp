package com.example.silomonitorapp.data.local

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

class SiloRepository(private val siloDao: SiloDao) {

    val silosFlow: Flow<List<SiloEntity>> = siloDao.obtenerTodosLosSilos()

    suspend fun obtenerSiloPorId(id: String): SiloEntity? = withContext(Dispatchers.IO) {
        siloDao.obtenerSiloPorId(id)
    }

    // Reglas de negocio de terreno (Ariztía)
    suspend fun registrarMovimiento(idSilo: String, cantidadKg: Double, esCarga: Boolean): Result<Unit> = withContext(Dispatchers.IO) {
        val silo = siloDao.obtenerSiloPorId(idSilo)
            ?: return@withContext Result.failure(Exception("Silo no encontrado en el sistema"))

        val nuevoNivel = if (esCarga) {
            silo.nivelActual + cantidadKg
        } else {
            silo.nivelActual - cantidadKg
        }

        // Regla 1: Bloqueo de Sobrellenado
        if (nuevoNivel > silo.capacidadMaxima) {
            val exceso = nuevoNivel - silo.capacidadMaxima
            return@withContext Result.failure(Exception("Bloqueo de Sobrellenado: Excede la capacidad por ${exceso.toInt()} kg."))
        }

        // Regla 2: Prevención de Saldo Negativo
        if (nuevoNivel < 0) {
            return@withContext Result.failure(Exception("Prevención de Saldo Negativo: Stock insuficiente (${silo.nivelActual.toInt()} kg disponibles)."))
        }

        siloDao.actualizarNivel(idSilo, nuevoNivel)
        Result.success(Unit)
    }

    // Datos semilla iniciales coincidentes con la UI de Ariztía (cada silo con su propia ubicación)
    suspend fun precargarSilosSiEstaVacio() = withContext(Dispatchers.IO) {
        if (siloDao.contarSilos() > 0) return@withContext

        val silosIniciales = listOf(
            SiloEntity("SIL-001", "Silo Norte A", "Granja El Paico", "Galpón 1", "Engorda Inicial", 30000.0, 21500.0, 2000.0, -33.7012, -71.0034),
            SiloEntity("SIL-002", "Silo Norte B", "Granja El Paico", "Galpón 2", "Engorda Concentrado", 30000.0, 9800.0, 2200.0, -33.7031, -71.0071),
            SiloEntity("SIL-003", "Silo Sur A", "Granja El Paico", "Galpón 3", "Engorda Pollos", 25000.0, 3900.0, 1800.0, -33.7058, -71.0010),
            SiloEntity("SIL-004", "Silo Principal", "Granja Pomaire", "Galpón 1", "Ponedora Fase 1", 28000.0, 22000.0, 1900.0, -33.6489, -71.1612),
            SiloEntity("SIL-005", "Silo Auxiliar", "Granja Pomaire", "Galpón 2", "Iniciador Pollitas", 20000.0, 3500.0, 1600.0, -33.6512, -71.1655),
            SiloEntity("SIL-006", "Silo Central", "Granja Melipilla", "Galpón 4", "Terminación Plus", 35000.0, 12600.0, 2400.0, -33.6875, -71.2148)
        )
        siloDao.insertarOActualizarSilos(silosIniciales)
    }
}