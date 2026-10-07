package com.example.silomonitorapp

import com.example.silomonitorapp.data.sesion.UsuariosDemo
import com.example.silomonitorapp.domain.Rol
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RolesTest {

    // --- Autenticación con usuarios de prueba (credenciales en el README) ---

    @Test
    fun login_conClaveCorrecta_devuelveUsuarioConSuRol() {
        val usuario = UsuariosDemo.autenticar("operario", "Operario2026!")
        assertNotNull(usuario)
        assertEquals(Rol.OPERARIO, usuario!!.rol)
    }

    @Test
    fun login_ignoraMayusculasYEspaciosEnElUsuario() {
        assertNotNull(UsuariosDemo.autenticar("  Admin ", "Admin2026!"))
    }

    @Test
    fun login_conClaveIncorrectaOUsuarioInexistente_falla() {
        assertNull(UsuariosDemo.autenticar("operario", "otra-clave"))
        assertNull(UsuariosDemo.autenticar("nadie", "Operario2026!"))
    }

    // --- Matriz RBAC ---

    @Test
    fun soloOperarioYAdmin_registranMovimientos() {
        assertTrue(Rol.OPERARIO.puedeRegistrarMovimientos)
        assertTrue(Rol.ADMIN.puedeRegistrarMovimientos)
        assertFalse(Rol.SUPERVISOR.puedeRegistrarMovimientos)
        assertFalse(Rol.JEFATURA.puedeRegistrarMovimientos)
    }

    @Test
    fun soloAdmin_gestionaSilos() {
        assertTrue(Rol.ADMIN.puedeGestionarSilos)
        Rol.entries.filter { it != Rol.ADMIN }.forEach { assertFalse(it.puedeGestionarSilos) }
    }

    @Test
    fun soloOperario_abreFormularioDesdeElQr() {
        assertTrue(Rol.OPERARIO.qrAbreFormulario)
        Rol.entries.filter { it != Rol.OPERARIO }.forEach { assertFalse(it.qrAbreFormulario) }
    }

    @Test
    fun supervisorSolicitaCamion_yJefaturaAprueba() {
        assertTrue(Rol.SUPERVISOR.puedeSolicitarCamion)
        assertFalse(Rol.SUPERVISOR.puedeAprobarCamion)
        assertTrue(Rol.JEFATURA.puedeAprobarCamion)
        assertFalse(Rol.JEFATURA.puedeSolicitarCamion)
        assertFalse(Rol.OPERARIO.puedeVerSolicitudes)
    }

    @Test
    fun soloJefaturaYAdmin_venDashboard() {
        assertTrue(Rol.JEFATURA.puedeVerDashboard)
        assertTrue(Rol.ADMIN.puedeVerDashboard)
        assertFalse(Rol.OPERARIO.puedeVerDashboard)
        assertFalse(Rol.SUPERVISOR.puedeVerDashboard)
    }

    @Test
    fun operario_soloVeSuGranja_yJefaturaVeTodas() {
        val operario = UsuariosDemo.buscar("operario")!!
        assertTrue(operario.puedeVerGranja("Granja El Paico"))
        assertFalse(operario.puedeVerGranja("Granja Pomaire"))

        val jefatura = UsuariosDemo.buscar("jefatura")!!
        assertTrue(jefatura.puedeVerGranja("Granja Melipilla"))
    }
}
