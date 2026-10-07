package com.example.silomonitorapp.ui.navigation

import android.Manifest
import android.content.pm.PackageManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.silomonitorapp.ui.components.crearUriFotoEvidencia
import com.example.silomonitorapp.ui.screens.MapaSilosScreen
import com.example.silomonitorapp.ui.screens.MovimientoFormScreen
import com.example.silomonitorapp.ui.screens.QrScannerScreen
import com.example.silomonitorapp.ui.model.DatosSilo
import com.example.silomonitorapp.ui.screens.SiloDetailScreen
import com.example.silomonitorapp.ui.screens.SiloFormScreen
import com.example.silomonitorapp.ui.screens.SiloListScreen
import com.example.silomonitorapp.ui.viewmodel.SiloViewModel
import kotlinx.coroutines.launch

object Rutas {
    const val ARG_SILO_ID = "siloId"

    const val LISTA = "lista"
    const val DETALLE = "detalle/{$ARG_SILO_ID}"
    const val MOVIMIENTO = "movimiento/{$ARG_SILO_ID}"
    const val MAPA = "mapa?$ARG_SILO_ID={$ARG_SILO_ID}"
    const val ESCANER = "escaner"
    const val FORMULARIO_SILO = "silo_form?$ARG_SILO_ID={$ARG_SILO_ID}"

    fun detalle(siloId: String) = "detalle/$siloId"
    fun movimiento(siloId: String) = "movimiento/$siloId"
    fun mapa(siloId: String? = null) = if (siloId == null) "mapa" else "mapa?$ARG_SILO_ID=$siloId"
    /** Sin id: alta de un silo nuevo. Con id: edición de ese silo. */
    fun formularioSilo(siloId: String? = null) = if (siloId == null) "silo_form" else "silo_form?$ARG_SILO_ID=$siloId"
}

private const val DURACION_TRANSICION_MS = 350

@Composable
fun AppNavHost(
    viewModel: SiloViewModel,
    navController: NavHostController = rememberNavController(),
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val silos by viewModel.silosUi.collectAsState()
    val mensajeOperacion by viewModel.mensajeOperacion.collectAsState()

    LaunchedEffect(mensajeOperacion) {
        mensajeOperacion?.let { mensaje ->
            Toast.makeText(context, mensaje, Toast.LENGTH_LONG).show()
            viewModel.limpiarMensaje()
        }
    }

    NavHost(
        navController = navController,
        startDestination = Rutas.LISTA,
        enterTransition = { slideIntoContainer(SlideDirection.Start, tween(DURACION_TRANSICION_MS)) },
        exitTransition = { slideOutOfContainer(SlideDirection.Start, tween(DURACION_TRANSICION_MS)) },
        popEnterTransition = { slideIntoContainer(SlideDirection.End, tween(DURACION_TRANSICION_MS)) },
        popExitTransition = { slideOutOfContainer(SlideDirection.End, tween(DURACION_TRANSICION_MS)) },
    ) {
        composable(Rutas.LISTA) {
            SiloListScreen(
                silos = silos,
                onSiloClick = { navController.navigate(Rutas.detalle(it.id)) },
                onAbrirMapa = { navController.navigate(Rutas.mapa()) },
                onEscanearQr = { navController.navigate(Rutas.ESCANER) },
                onAgregarSilo = { navController.navigate(Rutas.formularioSilo()) },
            )
        }

        composable(
            Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_SILO_ID) { type = NavType.StringType }),
        ) { entrada ->
            val id = entrada.arguments?.getString(Rutas.ARG_SILO_ID).orEmpty()
            val movimientos by remember(id) { viewModel.historial(id) }.collectAsState(initial = emptyList())
            SiloDetailScreen(
                silo = silos.firstOrNull { it.id == id },
                onVolver = { navController.popBackStack() },
                onRegistrarMovimiento = { navController.navigate(Rutas.movimiento(it.id)) },
                onVerEnMapa = { navController.navigate(Rutas.mapa(it.id)) },
                movimientos = movimientos,
                onEditar = { navController.navigate(Rutas.formularioSilo(it.id)) },
            )
        }

        composable(
            Rutas.FORMULARIO_SILO,
            arguments = listOf(navArgument(Rutas.ARG_SILO_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entrada ->
            val idEditar = entrada.arguments?.getString(Rutas.ARG_SILO_ID)
            val esNuevo = idEditar == null
            var datosIniciales by remember { mutableStateOf<DatosSilo?>(null) }
            var error by remember { mutableStateOf<String?>(null) }
            LaunchedEffect(idEditar) {
                if (idEditar != null) datosIniciales = viewModel.obtenerDatosSilo(idEditar)
            }
            SiloFormScreen(
                datosIniciales = datosIniciales,
                esNuevo = esNuevo,
                error = error,
                onVolver = { navController.popBackStack() },
                onGuardar = { datos ->
                    scope.launch {
                        error = viewModel.guardarSilo(datos, esNuevo)
                        if (error == null) navController.popBackStack()
                    }
                },
            )
        }

        composable(
            Rutas.MOVIMIENTO,
            arguments = listOf(navArgument(Rutas.ARG_SILO_ID) { type = NavType.StringType }),
        ) { entrada ->
            val id = entrada.arguments?.getString(Rutas.ARG_SILO_ID).orEmpty()
            var error by remember { mutableStateOf<String?>(null) }

            // Foto de evidencia: la cámara del sistema la guarda en el archivo que le entregamos
            var fotoUri by rememberSaveable { mutableStateOf<Uri?>(null) }
            var uriEnCaptura by rememberSaveable { mutableStateOf<Uri?>(null) }
            val tomarFoto = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { guardada ->
                if (guardada) fotoUri = uriEnCaptura
            }
            fun abrirCamara() {
                crearUriFotoEvidencia(context).also {
                    uriEnCaptura = it
                    tomarFoto.launch(it)
                }
            }
            val pedirPermisoCamara = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { concedido ->
                if (concedido) abrirCamara()
                else Toast.makeText(context, "Se necesita la cámara para adjuntar la foto", Toast.LENGTH_LONG).show()
            }

            MovimientoFormScreen(
                silo = silos.firstOrNull { it.id == id },
                error = error,
                onVolver = { navController.popBackStack() },
                onGuardar = { tipo, kg, observacion ->
                    scope.launch {
                        error = viewModel.registrarMovimiento(id, tipo, kg, observacion, fotoUri?.toString())
                        if (error == null) navController.popBackStack()
                    }
                },
                onAdjuntarFoto = {
                    val tienePermiso = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                        PackageManager.PERMISSION_GRANTED
                    if (tienePermiso) abrirCamara() else pedirPermisoCamara.launch(Manifest.permission.CAMERA)
                },
                fotoUri = fotoUri,
            )
        }

        composable(
            Rutas.MAPA,
            arguments = listOf(navArgument(Rutas.ARG_SILO_ID) {
                type = NavType.StringType
                nullable = true
                defaultValue = null
            }),
        ) { entrada ->
            MapaSilosScreen(
                silos = silos,
                siloEnfocadoId = entrada.arguments?.getString(Rutas.ARG_SILO_ID),
                onVolver = { navController.popBackStack() },
                onSiloClick = { navController.navigate(Rutas.detalle(it.id)) },
            )
        }

        composable(Rutas.ESCANER) {
            QrScannerScreen(
                // Operario: el QR abre directo el formulario de movimiento del silo
                onCodigoEscaneado = { codigo ->
                    scope.launch {
                        if (viewModel.existeSilo(codigo)) {
                            navController.navigate(Rutas.movimiento(codigo)) {
                                popUpTo(Rutas.ESCANER) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                },
                onVolver = { navController.popBackStack() },
            )
        }
    }
}
