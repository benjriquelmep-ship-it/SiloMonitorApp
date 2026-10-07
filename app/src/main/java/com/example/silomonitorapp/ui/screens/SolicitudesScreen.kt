package com.example.silomonitorapp.ui.screens

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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.components.formatearKg
import com.example.silomonitorapp.ui.model.EstadoSolicitud
import com.example.silomonitorapp.ui.model.SolicitudUi
import com.example.silomonitorapp.ui.theme.SemaforoAdvertencia
import com.example.silomonitorapp.ui.theme.SemaforoCritico
import com.example.silomonitorapp.ui.theme.SemaforoNormal
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bandeja de coordinación de reposición.
 * [onRevisar] = null: el rol solo consulta (Supervisor). Con valor: aprueba o rechaza (Jefatura y Admin).
 */
@Composable
fun SolicitudesScreen(
    solicitudes: List<SolicitudUi>,
    onVolver: () -> Unit,
    onRevisar: ((id: Long, aprobada: Boolean) -> Unit)?,
) {
    Scaffold(
        topBar = { AriztiaTopBar(titulo = "Reposición de alimento", onVolver = onVolver) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        if (solicitudes.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding).padding(32.dp), contentAlignment = Alignment.Center) {
                Text(
                    "No hay solicitudes de camión.\nSe crean desde la ficha de un silo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(solicitudes, key = { it.id }) { solicitud ->
                TarjetaSolicitud(solicitud, onRevisar)
            }
        }
    }
}

private val formatoFecha = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("es-CL"))

@Composable
private fun TarjetaSolicitud(
    solicitud: SolicitudUi,
    onRevisar: ((id: Long, aprobada: Boolean) -> Unit)?,
) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Filled.LocalShipping, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(solicitud.siloNombre, fontWeight = FontWeight.Bold)
                    Text(
                        "${solicitud.siloId} · ${solicitud.granja}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Etiqueta(solicitud.estado.etiqueta, solicitud.estado.color)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(formatearKg(solicitud.kgSolicitados), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                if (solicitud.urgente) Etiqueta("Urgente", SemaforoCritico)
            }
            Text(
                "Solicitado por ${solicitud.solicitadoPor} · ${formatoFecha.format(Date(solicitud.fechaSolicitud))}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (solicitud.observacion.isNotBlank()) {
                Text(solicitud.observacion, style = MaterialTheme.typography.bodySmall)
            }
            solicitud.revisadoPor?.let {
                Text(
                    "${solicitud.estado.etiqueta} por $it",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (onRevisar != null && solicitud.estado == EstadoSolicitud.PENDIENTE) {
                Row(Modifier.fillMaxWidth().padding(top = 6.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { onRevisar(solicitud.id, false) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                    ) {
                        Icon(Icons.Filled.Close, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Rechazar")
                    }
                    Button(
                        onClick = { onRevisar(solicitud.id, true) },
                        modifier = Modifier.weight(1f),
                        shape = MaterialTheme.shapes.medium,
                        colors = ButtonDefaults.buttonColors(containerColor = SemaforoNormal),
                    ) {
                        Icon(Icons.Filled.Check, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Aprobar")
                    }
                }
            }
        }
    }
}

private val EstadoSolicitud.color: Color
    get() = when (this) {
        EstadoSolicitud.PENDIENTE -> SemaforoAdvertencia
        EstadoSolicitud.APROBADA -> SemaforoNormal
        EstadoSolicitud.RECHAZADA -> SemaforoCritico
    }

@Composable
private fun Etiqueta(texto: String, color: Color) {
    Text(
        texto,
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.12f), MaterialTheme.shapes.small)
            .padding(horizontal = 10.dp, vertical = 4.dp),
    )
}

@Preview(showBackground = true)
@Composable
private fun SolicitudesScreenPreview() {
    SiloMonitorAppTheme {
        SolicitudesScreen(
            solicitudes = listOf(
                SolicitudUi(1, "SIL-003", "Silo Sur A", "Granja El Paico", 21_100.0, true, "Nivel crítico",
                    "supervisor", System.currentTimeMillis(), EstadoSolicitud.PENDIENTE, null),
            ),
            onVolver = {},
            onRevisar = { _, _ -> },
        )
    }
}
