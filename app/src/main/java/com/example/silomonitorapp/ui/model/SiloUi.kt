package com.example.silomonitorapp.ui.model

/**
 * Modelo de presentación que consumen las pantallas.
 * Al integrar Room, el ViewModel debe mapear SiloEntity -> SiloUi.
 */
data class SiloUi(
    val id: String,
    val codigo: String,
    val nombre: String,
    val granja: String,
    val galpon: String,
    val capacidadMaxKg: Double,
    val stockActualKg: Double,
    val consumoPromedioDiarioKg: Double,
    val latitud: Double,
    val longitud: Double,
) {
    val porcentaje: Float
        get() = if (capacidadMaxKg <= 0.0) 0f
        else (stockActualKg / capacidadMaxKg * 100).toFloat().coerceIn(0f, 100f)

    val estado: EstadoSilo
        get() = EstadoSilo.desdePorcentaje(porcentaje)

    /** Horas de autonomía = (Stock actual / Consumo promedio diario) * 24 */
    val horasAutonomia: Double?
        get() = if (consumoPromedioDiarioKg > 0.0) stockActualKg / consumoPromedioDiarioKg * 24 else null
}

enum class EstadoSilo(val etiqueta: String) {
    NORMAL("Normal"),
    ADVERTENCIA("Advertencia"),
    CRITICO("Crítico");

    companion object {
        /** Normal > 40%, Advertencia 20% – 40%, Crítico < 20% */
        fun desdePorcentaje(porcentaje: Float): EstadoSilo = when {
            porcentaje < 20f -> CRITICO
            porcentaje <= 40f -> ADVERTENCIA
            else -> NORMAL
        }
    }
}

enum class TipoMovimiento(val etiqueta: String) {
    CARGA("Carga"),
    CONSUMO("Consumo"),
}
