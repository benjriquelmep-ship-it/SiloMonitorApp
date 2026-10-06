package com.example.silomonitorapp.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope.SlideDirection
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.silomonitorapp.ui.model.SilosDemoStore
import com.example.silomonitorapp.ui.screens.EscanerQrPlaceholderScreen
import com.example.silomonitorapp.ui.screens.MapaSilosScreen
import com.example.silomonitorapp.ui.screens.MovimientoFormScreen
import com.example.silomonitorapp.ui.screens.SiloDetailScreen
import com.example.silomonitorapp.ui.screens.SiloListScreen

object Rutas {
    const val ARG_SILO_ID = "siloId"

    const val LISTA = "lista"
    const val DETALLE = "detalle/{$ARG_SILO_ID}"
    const val MOVIMIENTO = "movimiento/{$ARG_SILO_ID}"
    const val MAPA = "mapa?$ARG_SILO_ID={$ARG_SILO_ID}"
    const val ESCANER = "escaner"

    fun detalle(siloId: String) = "detalle/$siloId"
    fun movimiento(siloId: String) = "movimiento/$siloId"
    fun mapa(siloId: String? = null) = if (siloId == null) "mapa" else "mapa?$ARG_SILO_ID=$siloId"
}

private const val DURACION_TRANSICION_MS = 350

@Composable
fun AppNavHost(
    navController: NavHostController = rememberNavController(),
    // TODO(integración): reemplazar por el SiloViewModel con Room
    store: SilosDemoStore = remember { SilosDemoStore() },
) {
    val silos = store.silos

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
            )
        }

        composable(
            Rutas.DETALLE,
            arguments = listOf(navArgument(Rutas.ARG_SILO_ID) { type = NavType.StringType }),
        ) { entrada ->
            val id = entrada.arguments?.getString(Rutas.ARG_SILO_ID).orEmpty()
            SiloDetailScreen(
                silo = store.buscar(id),
                onVolver = { navController.popBackStack() },
                onRegistrarMovimiento = { navController.navigate(Rutas.movimiento(it.id)) },
                onVerEnMapa = { navController.navigate(Rutas.mapa(it.id)) },
            )
        }

        composable(
            Rutas.MOVIMIENTO,
            arguments = listOf(navArgument(Rutas.ARG_SILO_ID) { type = NavType.StringType }),
        ) { entrada ->
            val id = entrada.arguments?.getString(Rutas.ARG_SILO_ID).orEmpty()
            var error by remember { mutableStateOf<String?>(null) }
            MovimientoFormScreen(
                silo = store.buscar(id),
                error = error,
                onVolver = { navController.popBackStack() },
                onGuardar = { tipo, kg, _ ->
                    error = store.aplicarMovimiento(id, tipo, kg)
                    if (error == null) navController.popBackStack()
                },
                onAdjuntarFoto = { /* Cámara de evidencia: Integrante 1 */ },
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
            EscanerQrPlaceholderScreen(onVolver = { navController.popBackStack() })
        }
    }
}
