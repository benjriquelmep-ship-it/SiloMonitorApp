package com.example.silomonitorapp.domain

// Reglas puras (sin Android) para interpretar el clima que entrega la API externa Open-Meteo.
// Se separan del repositorio y de la pantalla para poder probarlas con pruebas unitarias.
object ReglasClima {

    // Umbrales que afectan la descarga de alimento desde el camión.
    const val VIENTO_FUERTE_KMH = 40.0
    const val CALOR_EXTREMO_C = 32.0
    const val HELADA_C = 0.0

    // Códigos WMO que usa Open-Meteo (https://open-meteo.com/en/docs, sección "WMO Weather interpretation codes").
    private val LLOVIZNA = setOf(51, 53, 55, 56, 57)
    private val LLUVIA = setOf(61, 63, 65, 66, 67, 80, 81, 82)
    private val NIEVE = setOf(71, 73, 75, 77, 85, 86)
    private val TORMENTA = setOf(95, 96, 99)

    /** Traduce el código WMO de Open-Meteo a un texto en español. */
    fun descripcion(codigo: Int?): String = when {
        codigo == null -> "Sin datos"
        codigo == 0 -> "Despejado"
        codigo == 1 || codigo == 2 -> "Parcialmente nublado"
        codigo == 3 -> "Nublado"
        codigo == 45 || codigo == 48 -> "Neblina"
        codigo in LLOVIZNA -> "Llovizna"
        codigo in LLUVIA -> "Lluvia"
        codigo in NIEVE -> "Nieve"
        codigo in TORMENTA -> "Tormenta eléctrica"
        else -> "Condición desconocida"
    }

    /** true si llueve, llovizna, nieva o hay tormenta (por código o por milímetros de agua). */
    fun hayPrecipitacion(codigo: Int?, precipitacionMm: Double?): Boolean {
        if ((precipitacionMm ?: 0.0) > 0.0) return true
        if (codigo == null) return false
        return codigo in LLOVIZNA || codigo in LLUVIA || codigo in NIEVE || codigo in TORMENTA
    }

    /**
     * Recomendación para la faena según el clima del silo.
     * Prioridad: tormenta > precipitaciones > viento fuerte > helada > calor extremo > condiciones normales.
     */
    fun recomendacion(codigo: Int?, temperaturaC: Double?, vientoKmh: Double?, precipitacionMm: Double?): String = when {
        codigo != null && codigo in TORMENTA -> "Tormenta: suspender la descarga de alimento hasta que pase."
        hayPrecipitacion(codigo, precipitacionMm) -> "Precipitaciones: proteger la tolva al descargar y prever retrasos del camión."
        (vientoKmh ?: 0.0) >= VIENTO_FUERTE_KMH -> "Viento fuerte: asegurar la manga de descarga."
        temperaturaC != null && temperaturaC <= HELADA_C -> "Helada: revisar que el alimento no se apelmace en la salida."
        temperaturaC != null && temperaturaC >= CALOR_EXTREMO_C -> "Calor extremo: el consumo de las aves puede variar."
        else -> "Condiciones normales para la descarga."
    }
}