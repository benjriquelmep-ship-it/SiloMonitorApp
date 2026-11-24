package com.example.silomonitorapp

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import com.example.silomonitorapp.domain.Rol
import com.example.silomonitorapp.ui.viewmodel.SesionViewModel
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

// PRUEBAS UNITARIAS de SesionViewModel + SesionRepository (inicio y cierre de sesión).
// Qué se mockea: Application y SharedPreferences (archivo donde se recuerda la sesión).
// Qué se prueba: validaciones del login, sesión restaurada y cierre de sesión.
class SesionViewModelTest {

    private lateinit var application: Application
    private lateinit var preferencias: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    @BeforeEach
    fun setUp() {
        application = mockk(relaxed = true)
        preferencias = mockk(relaxed = true)
        editor = mockk(relaxed = true)
        every { application.applicationContext } returns application
        every { application.getSharedPreferences(any(), Context.MODE_PRIVATE) } returns preferencias
        every { preferencias.edit() } returns editor
        every { editor.putString(any(), any()) } returns editor
        every { editor.remove(any()) } returns editor
        // Por defecto no hay sesión guardada.
        every { preferencias.getString("usuario", null) } returns null
    }

    @Test
    fun `sin sesion guardada el usuario inicial es null`() {
        SesionViewModel(application).usuario.value shouldBe null
    }

    @Test
    fun `restaura la sesion guardada al abrir la app`() {
        every { preferencias.getString("usuario", null) } returns "jefatura"

        SesionViewModel(application).usuario.value?.rol shouldBe Rol.JEFATURA
    }

    @Test
    fun `campos vacios muestran mensaje y no consultan credenciales`() {
        val viewModel = SesionViewModel(application)

        viewModel.iniciarSesion("", "clave") shouldBe "Ingresa tu usuario y contraseña."
        viewModel.iniciarSesion("admin", " ") shouldBe "Ingresa tu usuario y contraseña."
        verify(exactly = 0) { editor.putString(any(), any()) }
    }

    @Test
    fun `credenciales incorrectas devuelven error y no guardan sesion`() {
        val viewModel = SesionViewModel(application)

        viewModel.iniciarSesion("admin", "clave-mala") shouldBe "Usuario o contraseña incorrectos."
        viewModel.usuario.value shouldBe null
        verify(exactly = 0) { editor.putString(any(), any()) }
    }

    @Test
    fun `login correcto guarda la sesion y publica el usuario`() {
        val viewModel = SesionViewModel(application)

        viewModel.iniciarSesion("admin", "Admin2026!") shouldBe null
        viewModel.usuario.value?.rol shouldBe Rol.ADMIN
        verify { editor.putString("usuario", "admin") }
    }

    @Test
    fun `cerrar sesion borra la sesion guardada`() {
        val viewModel = SesionViewModel(application)
        viewModel.iniciarSesion("operario", "Operario2026!")

        viewModel.cerrarSesion()

        viewModel.usuario.value shouldBe null
        verify { editor.remove("usuario") }
    }

    @Test
    fun `factory crea el SesionViewModel`() {
        val creado = SesionViewModel.Factory(application).create(SesionViewModel::class.java)
        creado.usuario.value shouldBe null
    }
}