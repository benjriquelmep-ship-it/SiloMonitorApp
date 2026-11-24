package com.example.silomonitorapp

import com.example.silomonitorapp.domain.ReglasClima
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldStartWith
import org.junit.jupiter.api.Test

// PRUEBAS UNITARIAS de las reglas que interpretan el clima de la API externa (Open-Meteo).
// Son funciones puras: no necesitan mocks ni internet.
class ReglasClimaTest {

    @Test
    fun `descripcion traduce los codigos WMO al espanol`() {
        ReglasClima.descripcion(0) shouldBe "Despejado"
        ReglasClima.descripcion(2) shouldBe "Parcialmente nublado"
        ReglasClima.descripcion(3) shouldBe "Nublado"
        ReglasClima.descripcion(45) shouldBe "Neblina"
        ReglasClima.descripcion(53) shouldBe "Llovizna"
        ReglasClima.descripcion(63) shouldBe "Lluvia"
        ReglasClima.descripcion(81) shouldBe "Lluvia"
        ReglasClima.descripcion(75) shouldBe "Nieve"
        ReglasClima.descripcion(95) shouldBe "Tormenta eléctrica"
    }

    @Test
    fun `descripcion maneja codigo nulo o desconocido`() {
        ReglasClima.descripcion(null) shouldBe "Sin datos"
        ReglasClima.descripcion(123) shouldBe "Condición desconocida"
    }

    @Test
    fun `hayPrecipitacion detecta lluvia por milimetros o por codigo`() {
        ReglasClima.hayPrecipitacion(codigo = 0, precipitacionMm = 1.2) shouldBe true
        ReglasClima.hayPrecipitacion(codigo = 61, precipitacionMm = 0.0) shouldBe true
        ReglasClima.hayPrecipitacion(codigo = 0, precipitacionMm = 0.0) shouldBe false
        ReglasClima.hayPrecipitacion(codigo = null, precipitacionMm = null) shouldBe false
    }

    @Test
    fun `recomendacion prioriza la tormenta sobre las demas condiciones`() {
        ReglasClima.recomendacion(codigo = 95, temperaturaC = 35.0, vientoKmh = 60.0, precipitacionMm = 5.0) shouldStartWith "Tormenta"
    }

    @Test
    fun `recomendacion para lluvia viento helada calor y normal`() {
        ReglasClima.recomendacion(61, 15.0, 10.0, 2.0) shouldStartWith "Precipitaciones"
        ReglasClima.recomendacion(0, 15.0, 45.0, 0.0) shouldStartWith "Viento fuerte"
        ReglasClima.recomendacion(0, -2.0, 5.0, 0.0) shouldStartWith "Helada"
        ReglasClima.recomendacion(0, 34.0, 5.0, 0.0) shouldStartWith "Calor extremo"
        ReglasClima.recomendacion(1, 20.0, 10.0, 0.0) shouldStartWith "Condiciones normales"
    }

    @Test
    fun `recomendacion sin datos de temperatura ni viento es normal`() {
        ReglasClima.recomendacion(null, null, null, null) shouldStartWith "Condiciones normales"
    }
}