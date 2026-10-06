package com.example.silomonitorapp

import com.example.silomonitorapp.domain.ReglasTerreno
import com.example.silomonitorapp.ui.model.EstadoSilo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ReglasTerrenoTest {

    // --- Carga ---

    @Test
    fun carga_dentroDeCapacidad_sumaAlStock() {
        val resultado = ReglasTerreno.calcularNuevoNivel(10_000.0, 30_000.0, 5_000.0, esCarga = true)
        assertEquals(15_000.0, resultado.getOrThrow(), 0.0)
    }

    @Test
    fun carga_justoHastaCapacidadMaxima_esValida() {
        val resultado = ReglasTerreno.calcularNuevoNivel(25_000.0, 30_000.0, 5_000.0, esCarga = true)
        assertEquals(30_000.0, resultado.getOrThrow(), 0.0)
    }

    @Test
    fun carga_queSuperaCapacidad_seBloqueaPorSobrellenado() {
        val resultado = ReglasTerreno.calcularNuevoNivel(25_000.0, 30_000.0, 6_000.0, esCarga = true)
        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.contains("Sobrellenado"))
    }

    // --- Consumo ---

    @Test
    fun consumo_menorAlStock_restaDelStock() {
        val resultado = ReglasTerreno.calcularNuevoNivel(10_000.0, 30_000.0, 4_000.0, esCarga = false)
        assertEquals(6_000.0, resultado.getOrThrow(), 0.0)
    }

    @Test
    fun consumo_deTodoElStock_dejaSiloEnCero() {
        val resultado = ReglasTerreno.calcularNuevoNivel(4_000.0, 30_000.0, 4_000.0, esCarga = false)
        assertEquals(0.0, resultado.getOrThrow(), 0.0)
    }

    @Test
    fun consumo_mayorAlStock_seBloqueaPorSaldoNegativo() {
        val resultado = ReglasTerreno.calcularNuevoNivel(4_000.0, 30_000.0, 4_001.0, esCarga = false)
        assertTrue(resultado.isFailure)
        assertTrue(resultado.exceptionOrNull()!!.message!!.contains("Saldo Negativo"))
    }

    @Test
    fun cantidadCeroONegativa_esRechazada() {
        assertTrue(ReglasTerreno.calcularNuevoNivel(4_000.0, 30_000.0, 0.0, esCarga = true).isFailure)
        assertTrue(ReglasTerreno.calcularNuevoNivel(4_000.0, 30_000.0, -10.0, esCarga = false).isFailure)
    }

    // --- Autonomía ---

    @Test
    fun autonomia_aplicaFormulaStockSobreConsumoPor24() {
        // 6.000 kg / 2.000 kg diarios = 3 días = 72 horas
        assertEquals(72.0, ReglasTerreno.horasAutonomia(6_000.0, 2_000.0)!!, 0.0001)
    }

    @Test
    fun autonomia_sinConsumo_esNula() {
        assertNull(ReglasTerreno.horasAutonomia(6_000.0, 0.0))
    }

    // --- Semáforo ---

    @Test
    fun semaforo_respetaUmbrales() {
        assertEquals(EstadoSilo.CRITICO, EstadoSilo.desdePorcentaje(19.9f))
        assertEquals(EstadoSilo.ADVERTENCIA, EstadoSilo.desdePorcentaje(20f))
        assertEquals(EstadoSilo.ADVERTENCIA, EstadoSilo.desdePorcentaje(40f))
        assertEquals(EstadoSilo.NORMAL, EstadoSilo.desdePorcentaje(40.1f))
    }
}
