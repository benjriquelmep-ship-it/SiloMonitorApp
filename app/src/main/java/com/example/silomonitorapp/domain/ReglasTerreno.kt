package com.example.silomonitorapp.domain

/**
 * Reglas de negocio de terreno (Ariztía), sin dependencias de Android
 * para poder probarlas con tests unitarios.
 */
object ReglasTerreno {

    /**
     * Calcula el nuevo nivel del silo tras una carga o un consumo.
     * Falla si la cantidad no es válida, si hay sobrellenado o si el saldo queda negativo.
     */
    fun calcularNuevoNivel(
        nivelActual: Double,
        capacidadMaxima: Double,
        cantidadKg: Double,
        esCarga: Boolean
    ): Result<Double> {
        if (cantidadKg <= 0) {
            return Result.failure(IllegalArgumentException("La cantidad debe ser mayor a 0 kg."))
        }

        val nuevoNivel = if (esCarga) nivelActual + cantidadKg else nivelActual - cantidadKg

        // Regla 1: Bloqueo de Sobrellenado
        if (nuevoNivel > capacidadMaxima) {
            val exceso = nuevoNivel - capacidadMaxima
            return Result.failure(IllegalStateException("Bloqueo de Sobrellenado: Excede la capacidad por ${exceso.toInt()} kg."))
        }

        // Regla 2: Prevención de Saldo Negativo
        if (nuevoNivel < 0) {
            return Result.failure(IllegalStateException("Prevención de Saldo Negativo: Stock insuficiente (${nivelActual.toInt()} kg disponibles)."))
        }

        return Result.success(nuevoNivel)
    }

    /** Horas de autonomía = (Stock actual / Consumo promedio diario) * 24 */
    fun horasAutonomia(stockActualKg: Double, consumoPromedioDiarioKg: Double): Double? =
        if (consumoPromedioDiarioKg > 0.0) stockActualKg / consumoPromedioDiarioKg * 24 else null
}
