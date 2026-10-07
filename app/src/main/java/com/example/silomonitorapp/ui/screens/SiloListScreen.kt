package com.example.silomonitorapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.domain.Usuario
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.components.SiloCard
import com.example.silomonitorapp.ui.components.color
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.silosDemo
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme

@Composable
fun SiloListScreen(
    silos: List<SiloUi>,
    onSiloClick: (SiloUi) -> Unit,
    onAbrirMapa: () -> Unit,
    onEscanearQr: () -> Unit,
    onAgregarSilo: (() -> Unit)? = null, // null: el rol no puede crear silos
    usuario: Usuario? = null,
    onCerrarSesion: () -> Unit = {},
    onAbrirDashboard: (() -> Unit)? = null, // null: el rol no ve el dashboard
    onAbrirSolicitudes: (() -> Unit)? = null, // null: el rol no ve la reposición
    solicitudesPendientes: Int = 0,
    sincronizando: Boolean = false,
    onSincronizar: () -> Unit = {},
) {
    var busqueda by rememberSaveable { mutableStateOf("") }
    var filtro by rememberSaveable { mutableStateOf<EstadoSilo?>(null) }

    val visibles = silos
        .filter { filtro == null || it.estado == filtro }
        .filter {
            busqueda.isBlank() ||
                    it.nombre.contains(busqueda, ignoreCase = true) ||
                    it.codigo.contains(busqueda, ignoreCase = true) ||
                    it.granja.contains(busqueda, ignoreCase = true) ||
                    it.galpon.contains(busqueda, ignoreCase = true)
        }
        // Los críticos primero, luego por menor nivel
        .sortedWith(compareByDescending<SiloUi> { it.estado.ordinal }.thenBy { it.porcentaje })

    Scaffold(
        topBar = {
            AriztiaTopBar(
                titulo = "Monitoreo de Silos",
                acciones = {
                    if (sincronizando) {
                        IconButton(onClick = {}, enabled = false) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp,
                            )
                        }
                    } else {
                        IconButton(onClick = onSincronizar) {
                            Icon(Icons.Filled.Sync, contentDescription = "Sincronizar con la nube")
                        }
                    }
                    if (onAgregarSilo != null) {
                        IconButton(onClick = onAgregarSilo) {
                            Icon(Icons.Filled.Add, contentDescription = "Agregar silo")
                        }
                    }
                    IconButton(onClick = onAbrirMapa) {
                        Icon(Icons.Filled.Map, contentDescription = "Ver mapa")
                    }
                    if (usuario != null) {
                        IconButton(onClick = onCerrarSesion) {
                            Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = "Cerrar sesión")
                        }
                    }
                },
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = onEscanearQr,
                icon = { Icon(Icons.Filled.QrCodeScanner, contentDescription = null) },
                text = { Text("Escanear QR") },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 96.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            usuario?.let {
                item { TarjetaUsuario(it, onAbrirDashboard, onAbrirSolicitudes, solicitudesPendientes) }
            }
            item {
                ResumenSemaforo(
                    silos = silos,
                    filtro = filtro,
                    onFiltro = { filtro = if (filtro == it) null else it },
                )
            }
            item {
                OutlinedTextField(
                    value = busqueda,
                    onValueChange = { busqueda = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Buscar por nombre, código, granja o galpón") },
                    leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                    trailingIcon = {
                        if (busqueda.isNotEmpty()) {
                            IconButton(onClick = { busqueda = "" }) {
                                Icon(Icons.Filled.Clear, contentDescription = "Limpiar búsqueda")
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.medium,
                )
            }
            if (visibles.isEmpty()) {
                item {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text(
                            "No hay silos que coincidan",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            items(visibles, key = { it.id }) { silo ->
                SiloCard(
                    silo = silo,
                    onClick = { onSiloClick(silo) },
                    modifier = Modifier.animateItem(),
                )
            }
        }
    }
}

/** Quién está conectado, con qué rol y qué granjas puede ver. */
@Composable
private fun TarjetaUsuario(
    usuario: Usuario,
    onAbrirDashboard: (() -> Unit)?,
    onAbrirSolicitudes: (() -> Unit)?,
    solicitudesPendientes: Int,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.AccountCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(Modifier.weight(1f)) {
                    Text(usuario.nombre, fontWeight = FontWeight.SemiBold)
                    Text(
                        usuario.alcance,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Text(
                    usuario.rol.etiqueta,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier
                        .background(MaterialTheme.colorScheme.primary, MaterialTheme.shapes.small)
                        .padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
            if (onAbrirDashboard != null || onAbrirSolicitudes != null) {
                Row(Modifier.padding(top = 10.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    onAbrirDashboard?.let {
                        AssistChip(
                            onClick = it,
                            label = { Text("Dashboard") },
                            leadingIcon = { Icon(Icons.Filled.BarChart, contentDescription = null) },
                        )
                    }
                    onAbrirSolicitudes?.let {
                        AssistChip(
                            onClick = it,
                            label = { Text(if (solicitudesPendientes > 0) "Reposición ($solicitudesPendientes)" else "Reposición") },
                            leadingIcon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ResumenSemaforo(
    silos: List<SiloUi>,
    filtro: EstadoSilo?,
    onFiltro: (EstadoSilo) -> Unit,
) {
    Column(Modifier.animateContentSize()) {
        Text(
            "${silos.size} silos monitoreados",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            EstadoSilo.entries.forEach { estado ->
                val cantidad = silos.count { it.estado == estado }
                FilterChip(
                    selected = filtro == estado,
                    onClick = { onFiltro(estado) },
                    label = { Text("${estado.etiqueta}: $cantidad") },
                    colors = FilterChipDefaults.filterChipColors(
                        labelColor = estado.color,
                        selectedContainerColor = estado.color,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun SiloListScreenPreview() {
    SiloMonitorAppTheme {
        SiloListScreen(silosDemo, onSiloClick = {}, onAbrirMapa = {}, onEscanearQr = {})
    }
}