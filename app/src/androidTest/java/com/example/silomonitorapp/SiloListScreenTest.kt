package com.example.silomonitorapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import com.example.silomonitorapp.ui.model.silosDemo
import com.example.silomonitorapp.ui.screens.SiloListScreen
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class SiloListScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun mostrarListadoDeSilos() {
        composeRule.setContent {
            SiloMonitorAppTheme {
                SiloListScreen(
                    silos = silosDemo.take(2),
                    onSiloClick = {},
                    onAbrirMapa = {},
                    onEscanearQr = {}
                )
            }
        }

        composeRule.onNodeWithText("Silo Norte A").assertIsDisplayed()
        composeRule.onNodeWithText("Silo Norte B").assertIsDisplayed()
    }

    @Test
    fun busquedaFiltraSilosPorNombre() {
        composeRule.setContent {
            SiloMonitorAppTheme {
                SiloListScreen(
                    silos = silosDemo.take(3),
                    onSiloClick = {},
                    onAbrirMapa = {},
                    onEscanearQr = {}
                )
            }
        }

        composeRule
            .onNodeWithText("Buscar por nombre, código, granja o galpón")
            .performTextInput("Sur A")

        composeRule.onNodeWithText("Silo Sur A").assertIsDisplayed()
        composeRule.onNodeWithText("Silo Norte A").assertDoesNotExist()
    }

    @Test
    fun botonSincronizarEjecutaEvento() {
        var sincronizado = false

        composeRule.setContent {
            SiloMonitorAppTheme {
                SiloListScreen(
                    silos = silosDemo.take(1),
                    onSiloClick = {},
                    onAbrirMapa = {},
                    onEscanearQr = {},
                    onSincronizar = { sincronizado = true }
                )
            }
        }

        composeRule
            .onNodeWithContentDescription("Sincronizar con la nube")
            .performClick()

        assertTrue(sincronizado)
    }

    @Test
    fun botonEscanearQrEjecutaEvento() {
        var escanerAbierto = false

        composeRule.setContent {
            SiloMonitorAppTheme {
                SiloListScreen(
                    silos = silosDemo.take(1),
                    onSiloClick = {},
                    onAbrirMapa = {},
                    onEscanearQr = { escanerAbierto = true }
                )
            }
        }

        composeRule
            .onNodeWithText("Escanear QR", useUnmergedTree = true)
            .performClick()

        assertTrue(escanerAbierto)
    }
}