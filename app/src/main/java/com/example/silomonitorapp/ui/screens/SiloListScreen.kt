package com.example.silomonitorapp.ui.screens

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Search
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
                    IconButton(onClick = onAbrirMapa) {
                        Icon(Icons.Filled.Map, contentDescription = "Ver mapa")
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
