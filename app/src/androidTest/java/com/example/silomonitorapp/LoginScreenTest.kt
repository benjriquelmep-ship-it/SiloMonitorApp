package com.example.silomonitorapp

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import com.example.silomonitorapp.ui.screens.LoginScreen
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import org.junit.Rule
import org.junit.Test

class LoginScreenTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun camposVaciosMuestranMensajesDeValidacion() {
        composeRule.setContent {
            SiloMonitorAppTheme {
                LoginScreen(onIngresar = { _, _ -> null })
            }
        }

        composeRule.onNodeWithText("Ingresar").performScrollTo().performClick()

        composeRule.onNodeWithText("El usuario es obligatorio").assertIsDisplayed()
        composeRule.onNodeWithText("La contraseña es obligatoria").assertIsDisplayed()
    }

    @Test
    fun credencialesInvalidasMuestranMensajeDeErrorEnPantalla() {
        composeRule.setContent {
            SiloMonitorAppTheme {
                LoginScreen(onIngresar = { _, _ -> "Usuario o contraseña incorrectos." })
            }
        }

        composeRule.onNodeWithText("Usuario").performTextInput("operario")
        composeRule.onNodeWithText("Contraseña").performTextInput("clave-incorrecta")
        composeRule.onNodeWithText("Ingresar").performScrollTo().performClick()

        composeRule.onNodeWithText("Usuario o contraseña incorrectos.").assertIsDisplayed()
    }
}