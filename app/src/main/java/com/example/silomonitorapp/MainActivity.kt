package com.example.silomonitorapp

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.silomonitorapp.ui.components.RegistroMovimientoDialog
import com.example.silomonitorapp.ui.screens.MapaSilosScreen
import com.example.silomonitorapp.ui.screens.QrScannerScreen
import com.example.silomonitorapp.ui.screens.SiloListScreen
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import com.example.silomonitorapp.ui.viewmodel.SiloViewModel

class MainActivity : ComponentActivity() {

    private val viewModel: SiloViewModel by viewModels {
        SiloViewModel.Factory(application)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            SiloMonitorAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val navController = rememberNavController()

                    val listaSilosUi by viewModel.silosUi.collectAsState()
                    val siloSeleccionado by viewModel.siloSeleccionado.collectAsState()
                    val mensajeOperacion by viewModel.mensajeOperacion.collectAsState()

                    LaunchedEffect(mensajeOperacion) {
                        mensajeOperacion?.let { mensaje ->
                            Toast.makeText(this@MainActivity, mensaje, Toast.LENGTH_LONG).show()
                            viewModel.limpiarMensaje()
                        }
                    }

                    NavHost(
                        navController = navController,
                        startDestination = "silos"
                    ) {
                        // 1. Pantalla principal con la lista de silos
                        composable("silos") {
                            SiloListScreen(
                                silos = listaSilosUi,
                                onSiloClick = { siloUi ->
                                    viewModel.seleccionarSiloPorId(siloUi.id)
                                },
                                onAbrirMapa = {
                                    navController.navigate("mapa")
                                },
                                onEscanearQr = {
                                    navController.navigate("escaner_qr")
                                }
                            )
                        }

                        // 2. Pantalla de Mapa (con los parámetros obligatorios que solicita)
                        composable("mapa") {
                            MapaSilosScreen(
                                silos = listaSilosUi,
                                siloEnfocadoId = null,
                                onSiloClick = { siloUi ->
                                    navController.popBackStack()
                                    viewModel.seleccionarSiloPorId(siloUi.id)
                                },
                                onVolver = {
                                    navController.popBackStack()
                                }
                            )
                        }

                        // 3. Pantalla de Escáner QR
                        composable("escaner_qr") {
                            QrScannerScreen(
                                onCodigoEscaneado = { codigoLeido ->
                                    navController.popBackStack()
                                    viewModel.seleccionarSiloPorId(codigoLeido)
                                },
                                onVolver = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }

                    // 4. Modal de registro de Carga y Consumo
                    siloSeleccionado?.let { silo ->
                        RegistroMovimientoDialog(
                            silo = silo,
                            onDismiss = { viewModel.seleccionarSilo(null) },
                            onConfirmar = { cantidadKg, esCarga ->
                                viewModel.registrarMovimiento(silo.id, cantidadKg, esCarga)
                            }
                        )
                    }
                }
            }
        }
    }
}