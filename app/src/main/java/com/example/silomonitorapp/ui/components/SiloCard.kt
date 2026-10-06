package com.example.silomonitorapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.silosDemo
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme
import java.text.NumberFormat
import java.util.Locale

private val formatoKg = NumberFormat.getIntegerInstance(Locale.forLanguageTag("es-CL"))

fun formatearKg(kg: Double): String = "${formatoKg.format(kg)} kg"

fun formatearAutonomia(horas: Double?): String = when {
    horas == null -> "Sin datos de consumo"
    horas < 24 -> "${horas.toInt()} h"
    else -> "${(horas / 24).toInt()} d ${(horas % 24).toInt()} h"
}

@Composable
fun SiloCard(silo: SiloUi, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Card(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Row(Modifier.height(IntrinsicSize.Min)) {
            // Franja lateral con el color del semáforo
            Box(
                Modifier
                    .width(6.dp)
                    .fillMaxHeight()
                    .background(silo.estado.color)
            )
            Column(Modifier.padding(16.dp).weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            silo.nombre,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            "${silo.codigo} · ${silo.granja} · ${silo.galpon}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    EstadoBadge(silo.estado)
                }
                Spacer(Modifier.height(12.dp))
                BarraNivelAnimada(porcentaje = silo.porcentaje)
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        "${formatearKg(silo.stockActualKg)} / ${formatearKg(silo.capacidadMaxKg)}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Outlined.Schedule,
                            contentDescription = "Autonomía",
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            formatearAutonomia(silo.horasAutonomia),
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFF8F9FA)
@Composable
private fun SiloCardPreview() {
    SiloMonitorAppTheme {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            silosDemo.take(3).forEach { SiloCard(it, onClick = {}) }
        }
    }
}
