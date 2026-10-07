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

    /**
     * Valida los datos maestros de un silo antes de darlo de alta o editarlo.
     * Devuelve el mensaje de error, o null si los datos son válidos.
     */
    fun validarSilo(
        codigo: String,
        nombre: String,
        granja: String,
        capacidadMaxima: Double,
        stockActual: Double,
        consumoPromedioDiario: Double,
        latitud: Double,
        longitud: Double
    ): String? = when {
        codigo.isBlank() -> "El código del silo es obligatorio."
        // Sin espacios ni símbolos: el mismo código va impreso en el QR del silo
        !Regex("^[A-Z0-9-]{1,20}$").matches(codigo) -> "El código solo puede tener letras, números y guiones (máx. 20)."
        nombre.isBlank() -> "El nombre del silo es obligatorio."
        granja.isBlank() -> "La granja es obligatoria."
        capacidadMaxima <= 0 -> "La capacidad máxima debe ser mayor a 0 kg."
        stockActual < 0 -> "El stock no puede ser negativo."
        stockActual > capacidadMaxima -> "El stock no puede superar la capacidad máxima."
        consumoPromedioDiario < 0 -> "El consumo diario no puede ser negativo."
        latitud !in -90.0..90.0 || longitud !in -180.0..180.0 -> "Las coordenadas no son válidas."
        else -> null
    }

    /** Kilos sugeridos para un camión de reposición: lo que falta para llenar el silo. */
    fun kgSugeridosReposicion(stockActual: Double, capacidadMaxima: Double): Double =
        (capacidadMaxima - stockActual).coerceAtLeast(0.0)

    /**
     * Valida una solicitud de camión: la carga pedida debe caber en el silo.
     * Devuelve el mensaje de error, o null si es válida.
     */
    fun validarSolicitudCamion(stockActual: Double, capacidadMaxima: Double, kgSolicitados: Double): String? {
        val espacioLibre = kgSugeridosReposicion(stockActual, capacidadMaxima)
        return when {
            kgSolicitados <= 0 -> "La cantidad debe ser mayor a 0 kg."
            kgSolicitados > espacioLibre -> "Solo caben ${espacioLibre.toInt()} kg en el silo."
            else -> null
        }
    }

    /** Horas de autonomía = (Stock actual / Consumo promedio diario) * 24 */
    fun horasAutonomia(stockActualKg: Double, consumoPromedioDiarioKg: Double): Double? =
        if (consumoPromedioDiarioKg > 0.0) stockActualKg / consumoPromedioDiarioKg * 24 else null
}
