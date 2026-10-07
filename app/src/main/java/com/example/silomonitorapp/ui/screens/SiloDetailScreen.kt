package com.example.silomonitorapp.ui.screens

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.IconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.components.EstadoBadge
import com.example.silomonitorapp.ui.components.SiloNivelVertical
import com.example.silomonitorapp.ui.components.color
import com.example.silomonitorapp.ui.components.formatearAutonomia
import com.example.silomonitorapp.ui.components.MiniaturaFoto
import com.example.silomonitorapp.ui.components.formatearKg
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.MovimientoUi
import com.example.silomonitorapp.ui.model.TipoMovimiento
import com.example.silomonitorapp.ui.theme.SemaforoNormal
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.silosDemo
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme

@Composable
fun SiloDetailScreen(
    silo: SiloUi?,
    onVolver: () -> Unit,
    onRegistrarMovimiento: ((SiloUi) -> Unit)?, // null: el rol solo consulta
    onVerEnMapa: (SiloUi) -> Unit,
    movimientos: List<MovimientoUi> = emptyList(),
    onEditar: ((SiloUi) -> Unit)? = null, // null: el rol no puede editar silos
    onSolicitarCamion: ((SiloUi) -> Unit)? = null, // null: el rol no coordina reposición
) {
    Scaffold(
        topBar = {
            AriztiaTopBar(
                titulo = silo?.nombre ?: "Silo",
                onVolver = onVolver,
                acciones = {
                    if (silo != null && onEditar != null) {
                        IconButton(onClick = { onEditar(silo) }) {
                            Icon(Icons.Filled.Edit, contentDescription = "Editar silo")
                        }
                    }
                },
            )
        },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (silo == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("Silo no encontrado")
            }
            return@Scaffold
        }
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Row(
                    Modifier.fillMaxWidth().padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    SiloNivelVertical(porcentaje = silo.porcentaje)
                    Spacer(Modifier.width(20.dp))
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        EstadoBadge(silo.estado)
                        Text(silo.codigo, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                        Text(silo.granja, style = MaterialTheme.typography.bodyMedium)
                        Text(
                            silo.galpon,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }

            if (silo.estado != EstadoSilo.NORMAL) {
                AvisoEstado(silo.estado)
            }

            Card(
                shape = MaterialTheme.shapes.medium,
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
            ) {
                Column(Modifier.padding(16.dp)) {
                    Text("Ficha técnica", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(8.dp))
                    FilaDato("Stock actual", formatearKg(silo.stockActualKg))
                    FilaDato("Capacidad máxima", formatearKg(silo.capacidadMaxKg))
                    FilaDato("Consumo promedio diario", formatearKg(silo.consumoPromedioDiarioKg))
                    FilaDato("Autonomía estimada", formatearAutonomia(silo.horasAutonomia), destacado = true)
                    FilaDato("Coordenadas", "%.4f, %.4f".format(silo.latitud, silo.longitud), mostrarDivisor = false)
                }
            }

            if (onRegistrarMovimiento != null) {
                Button(
                    onClick = { onRegistrarMovimiento(silo) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                ) {
                    Icon(Icons.Filled.EditNote, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Registrar movimiento")
                }
            }
            if (onSolicitarCamion != null) {
                OutlinedButton(
                    onClick = { onSolicitarCamion(silo) },
                    modifier = Modifier.fillMaxWidth().height(52.dp),
                    shape = MaterialTheme.shapes.medium,
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                ) {
                    Icon(Icons.Filled.LocalShipping, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Solicitar camión de reposición")
                }
            }
            OutlinedButton(
                onClick = { onVerEnMapa(silo) },
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.primary),
            ) {
                Icon(Icons.Filled.Place, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Ver en el mapa")
            }

            HistorialMovimientos(movimientos)
        }
    }
}

private val formatoFecha = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("es-CL"))

@Composable
private fun HistorialMovimientos(movimientos: List<MovimientoUi>) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text("Últimos movimientos", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            if (movimientos.isEmpty()) {
                Text(
                    "Aún no hay movimientos registrados.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            movimientos.forEachIndexed { indice, movimiento ->
                FilaMovimiento(movimiento)
                if (indice < movimientos.lastIndex) HorizontalDivider()
            }
        }
    }
}

@Composable
private fun FilaMovimiento(movimiento: MovimientoUi) {
    val esCarga = movimiento.tipo == TipoMovimiento.CARGA
    val colorTipo = if (esCarga) SemaforoNormal else MaterialTheme.colorScheme.primary
    Row(Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.Top) {
        Icon(
            if (esCarga) Icons.Filled.ArrowUpward else Icons.Filled.ArrowDownward,
            contentDescription = movimiento.tipo.etiqueta,
            tint = colorTipo,
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                "${movimiento.tipo.etiqueta} ${if (esCarga) "+" else "−"}${formatearKg(movimiento.cantidadKg)}",
                fontWeight = FontWeight.SemiBold,
                color = colorTipo,
            )
            Text(
                formatoFecha.format(Date(movimiento.fecha)),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (movimiento.observacion.isNotBlank()) {
                Text(movimiento.observacion, style = MaterialTheme.typography.bodySmall)
            }
            movimiento.fotoUri?.let {
                MiniaturaFoto(Uri.parse(it), Modifier.padding(top = 8.dp))
            }
        }
        if (movimiento.pendienteSincronizar) {
            Spacer(Modifier.width(8.dp))
            Icon(Icons.Filled.CloudOff, contentDescription = "Pendiente de sincronizar", tint = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun AvisoEstado(estado: EstadoSilo) {
    val texto = when (estado) {
        EstadoSilo.CRITICO -> "Riesgo inminente de desabastecimiento. Coordinar reposición urgente."
        else -> "Reposición recomendada para prevenir quiebres de stock."
    }
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = estado.color.copy(alpha = 0.12f)),
    ) {
        Text(
            texto,
            modifier = Modifier.padding(16.dp),
            color = estado.color,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun FilaDato(
    etiqueta: String,
    valor: String,
    destacado: Boolean = false,
    mostrarDivisor: Boolean = true,
) {
    Row(
        Modifier.fillMaxWidth().padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(etiqueta, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            valor,
            fontWeight = if (destacado) FontWeight.Bold else FontWeight.Medium,
            color = if (destacado) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
        )
    }
    if (mostrarDivisor) HorizontalDivider()
}

@Preview(showBackground = true)
@Composable
private fun SiloDetailScreenPreview() {
    SiloMonitorAppTheme {
        SiloDetailScreen(silosDemo[2], onVolver = {}, onRegistrarMovimiento = {}, onVerEnMapa = {})
    }
}
