package com.example.silomonitorapp.ui.screens

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.components.color
import com.example.silomonitorapp.ui.components.formatearAutonomia
import com.example.silomonitorapp.ui.components.tonoMarcador
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.SiloUi
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.LatLngBounds
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapProperties
import com.google.maps.android.compose.MapUiSettings
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import kotlinx.coroutines.launch

private val CENTRO_MELIPILLA = LatLng(-33.69, -71.10)

@Composable
fun MapaSilosScreen(
    silos: List<SiloUi>,
    siloEnfocadoId: String?,
    onVolver: () -> Unit,
    onSiloClick: (SiloUi) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    var soloCriticos by rememberSaveable { mutableStateOf(false) }
    var mapaCargado by remember { mutableStateOf(false) }

    fun tienePermiso() = ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED || ContextCompat.checkSelfPermission(
        context, Manifest.permission.ACCESS_COARSE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    var permisoUbicacion by remember { mutableStateOf(tienePermiso()) }
    val solicitarPermiso = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { resultado -> permisoUbicacion = resultado.values.any { it } }

    LaunchedEffect(Unit) {
        if (!permisoUbicacion) {
            solicitarPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
        }
    }

    val visibles = if (soloCriticos) silos.filter { it.estado != EstadoSilo.NORMAL } else silos
    val camara = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(CENTRO_MELIPILLA, 10f)
    }

    // Encuadra el silo enfocado o todos los silos cuando el mapa termina de cargar
    LaunchedEffect(mapaCargado, siloEnfocadoId) {
        if (!mapaCargado || silos.isEmpty()) return@LaunchedEffect
        val enfocado = silos.firstOrNull { it.id == siloEnfocadoId }
        if (enfocado != null) {
            camara.animate(CameraUpdateFactory.newLatLngZoom(LatLng(enfocado.latitud, enfocado.longitud), 16f))
        } else {
            val limites = LatLngBounds.builder().apply {
                silos.forEach { include(LatLng(it.latitud, it.longitud)) }
            }.build()
            camara.animate(CameraUpdateFactory.newLatLngBounds(limites, 120))
        }
    }

    @SuppressLint("MissingPermission")
    fun centrarEnMiUbicacion() {
        if (!tienePermiso()) {
            solicitarPermiso.launch(
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
            )
            return
        }
        LocationServices.getFusedLocationProviderClient(context).lastLocation
            .addOnSuccessListener { ubicacion ->
                scope.launch {
                    if (ubicacion != null) {
                        camara.animate(
                            CameraUpdateFactory.newLatLngZoom(LatLng(ubicacion.latitude, ubicacion.longitude), 14f)
                        )
                    } else {
                        snackbar.showSnackbar("No se pudo obtener la ubicación GPS")
                    }
                }
            }
    }

    Scaffold(
        topBar = { AriztiaTopBar(titulo = "Mapa de Silos", onVolver = onVolver) },
        snackbarHost = { SnackbarHost(snackbar) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = ::centrarEnMiUbicacion,
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ) {
                Icon(Icons.Filled.MyLocation, contentDescription = "Mi ubicación")
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            GoogleMap(
                modifier = Modifier.fillMaxSize(),
                cameraPositionState = camara,
                properties = MapProperties(isMyLocationEnabled = permisoUbicacion),
                uiSettings = MapUiSettings(myLocationButtonEnabled = false, zoomControlsEnabled = false),
                onMapLoaded = { mapaCargado = true },
            ) {
                visibles.forEach { silo ->
                    val estado = remember(silo.id) { MarkerState(LatLng(silo.latitud, silo.longitud)) }
                    Marker(
                        state = estado,
                        title = "${silo.nombre} (${silo.porcentaje.toInt()}%)",
                        snippet = "${silo.estado.etiqueta} · Autonomía ${formatearAutonomia(silo.horasAutonomia)}",
                        icon = BitmapDescriptorFactory.defaultMarker(silo.estado.tonoMarcador),
                        onInfoWindowClick = { onSiloClick(silo) },
                    )
                }
            }

            Column(
                Modifier.align(Alignment.TopStart).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                FilterChip(
                    selected = soloCriticos,
                    onClick = { soloCriticos = !soloCriticos },
                    label = { Text("Solo en alerta") },
                    colors = FilterChipDefaults.filterChipColors(
                        containerColor = MaterialTheme.colorScheme.surface,
                    ),
                )
                LeyendaSemaforo()
            }
        }
    }
}

@Composable
private fun LeyendaSemaforo() {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp),
    ) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Semáforo", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
            listOf(
                EstadoSilo.NORMAL to "> 40%",
                EstadoSilo.ADVERTENCIA to "20 – 40%",
                EstadoSilo.CRITICO to "< 20%",
            ).forEach { (estado, rango) ->
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Box(Modifier.size(10.dp).background(estado.color, CircleShape))
                    Text("${estado.etiqueta} $rango", style = MaterialTheme.typography.labelSmall)
                }
            }
        }
    }
}
