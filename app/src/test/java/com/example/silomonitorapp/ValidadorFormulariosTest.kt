package com.example.silomonitorapp

import com.example.silomonitorapp.domain.ValidadorFormularios
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class ValidadorFormulariosTest {

    @Test
    fun validarCapacidad_conCeroONegativo_retornaError() {
        assertNotNull(ValidadorFormularios.validarCapacidad("0"))
        assertNotNull(ValidadorFormularios.validarCapacidad("-500"))
        assertNotNull(ValidadorFormularios.validarCapacidad("abc"))
        assertNotNull(ValidadorFormularios.validarCapacidad(""))
    }

    @Test
    fun validarCapacidad_conValorValido_retornaNull() {
        assertNull(ValidadorFormularios.validarCapacidad("15000"))
        assertNull(ValidadorFormularios.validarCapacidad("2500.5"))
    }

    @Test
    fun validarUsuario_vacio_retornaError() {
        assertNotNull(ValidadorFormularios.validarUsuario(""))
        assertNotNull(ValidadorFormularios.validarUsuario("   "))
    }

    @Test
    fun validarUsuario_valido_retornaNull() {
        assertNull(ValidadorFormularios.validarUsuario("admin@ariztia.cl"))
    }

    @Test
    fun validarPassword_vacio_retornaError() {
        assertNotNull(ValidadorFormularios.validarPassword(""))
        assertNotNull(ValidadorFormularios.validarPassword("   "))
    }

    @Test
    fun validarPassword_valido_retornaNull() {
        assertNull(ValidadorFormularios.validarPassword("ariztia2026"))
    }

    @Test
    fun validarCoordenadas_fueraDeRango_retornaError() {
        assertNotNull(ValidadorFormularios.validarCoordenada("-95.0", esLatitud = true))
        assertNotNull(ValidadorFormularios.validarCoordenada("190.0", esLatitud = false))
    }

    @Test
    fun validarCoordenadas_validas_retornaNull() {
        assertNull(ValidadorFormularios.validarCoordenada("-33.685", esLatitud = true))
        assertNull(ValidadorFormularios.validarCoordenada("-71.215", esLatitud = false))
    }

    @Test
    fun validarKilos_excediendoCapacidad_retornaError() {
        assertNotNull(ValidadorFormularios.validarKilos("25000", capacidadMaxima = 20000.0))
        assertNotNull(ValidadorFormularios.validarKilos("-10", capacidadMaxima = 20000.0))
        assertNotNull(ValidadorFormularios.validarKilos("0", capacidadMaxima = 20000.0))
    }

    @Test
    fun validarKilos_valido_retornaNull() {
        assertNull(ValidadorFormularios.validarKilos("5000", capacidadMaxima = 20000.0))
    }

    @Test
    fun numeros_aceptanComaDecimal() {
        assertEquals(2500.5, ValidadorFormularios.aNumero("2500,5")!!, 0.0)
        assertNull(ValidadorFormularios.validarCapacidad("2500,5"))
        assertNull(ValidadorFormularios.validarCoordenada("-33,685", esLatitud = true))
    }

    @Test
    fun validarCodigoSilo_rechazaEspaciosYSimbolos() {
        assertNotNull(ValidadorFormularios.validarCodigoSilo("SIL 007"))
        assertNotNull(ValidadorFormularios.validarCodigoSilo("SIL#7"))
        assertNull(ValidadorFormularios.validarCodigoSilo("TOLVA-12"))
    }

    @Test
    fun validarStock_noPuedeSuperarCapacidadNiSerNegativo() {
        assertNotNull(ValidadorFormularios.validarStock("30000", "25000"))
        assertNotNull(ValidadorFormularios.validarStock("-1", "25000"))
        assertNotNull(ValidadorFormularios.validarStock("", "25000"))
        assertNull(ValidadorFormularios.validarStock("25000", "25000"))
    }

    @Test
    fun validarConsumoYGranja_obligatorios() {
        assertNotNull(ValidadorFormularios.validarConsumo(""))
        assertNotNull(ValidadorFormularios.validarConsumo("abc"))
        assertNull(ValidadorFormularios.validarConsumo("1800"))
        assertNotNull(ValidadorFormularios.validarObligatorio(" ", "La granja"))
        assertNull(ValidadorFormularios.validarObligatorio("Granja El Paico", "La granja"))
    }
}