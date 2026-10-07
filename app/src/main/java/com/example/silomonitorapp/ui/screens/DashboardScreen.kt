package com.example.silomonitorapp.ui.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.components.BarraNivelAnimada
import com.example.silomonitorapp.ui.components.EstadoBadge
import com.example.silomonitorapp.ui.components.formatearAutonomia
import com.example.silomonitorapp.ui.components.formatearKg
import com.example.silomonitorapp.ui.model.ConsumoDia
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.silosDemo
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme

// Una sola serie (consumo): un tono neutro que no compite con los colores del semáforo
private val ColorConsumo = Color(0xFF3B6E8F)

/** Dashboard analítico de Jefatura: indicadores, niveles por silo y consumo de la semana. */
@Composable
fun DashboardScreen(
    silos: List<SiloUi>,
    consumoSemana: List<ConsumoDia>,
    solicitudesPendientes: Int,
    onVolver: () -> Unit,
) {
    val capacidadTotal = silos.sumOf { it.capacidadMaxKg }
    val stockTotal = silos.sumOf { it.stockActualKg }
    val llenadoGlobal = if (capacidadTotal > 0) (stockTotal / capacidadTotal * 100).toFloat() else 0f
    val criticos = silos.count { it.estado == EstadoSilo.CRITICO }
    val menorAutonomia = silos.filter { it.horasAutonomia != null }.minByOrNull { it.horasAutonomia!! }

    Scaffold(
        topBar = { AriztiaTopBar(titulo = "Dashboard", onVolver = onVolver) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // Indicadores principales
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador("Llenado global", "${llenadoGlobal.toInt()}%", "${formatearKg(stockTotal)} de ${formatearKg(capacidadTotal)}", Modifier.weight(1f))
                Indicador("Silos críticos", "$criticos de ${silos.size}", "Bajo el 20% de capacidad", Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Indicador(
                    "Menor autonomía",
                    menorAutonomia?.let { formatearAutonomia(it.horasAutonomia) } ?: "—",
                    menorAutonomia?.nombre ?: "Sin datos",
                    Modifier.weight(1f),
                )
                Indicador("Camiones pendientes", "$solicitudesPendientes", "Esperando aprobación", Modifier.weight(1f))
            }

            Seccion("Consumo de los últimos 7 días") {
                GraficoConsumo(consumoSemana)
            }

            Seccion("Nivel por silo") {
                // Del más crítico al más lleno; el estado va con texto, no solo con color
                silos.sortedBy { it.porcentaje }.forEachIndexed { indice, silo ->
                    Column(Modifier.padding(vertical = 8.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                silo.nombre,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text("${silo.porcentaje.toInt()}%", fontWeight = FontWeight.SemiBold)
                            Spacer(Modifier.width(8.dp))
                            EstadoBadge(silo.estado)
                        }
                        Spacer(Modifier.height(6.dp))
                        BarraNivelAnimada(porcentaje = silo.porcentaje)
                    }
                    if (indice < silos.lastIndex) HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun Indicador(titulo: String, valor: String, detalle: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(14.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(valor, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
            Text(
                detalle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun Seccion(titulo: String, contenido: @Composable () -> Unit) {
    Card(
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
    ) {
        Column(Modifier.padding(16.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(12.dp))
            contenido()
        }
    }
}

/** Gráfico de columnas del consumo diario. Al tocar una columna se muestra su valor exacto. */
@Composable
private fun GraficoConsumo(dias: List<ConsumoDia>) {
    if (dias.isEmpty()) return
    val maximo = dias.maxOf { it.kg }
    var seleccionado by remember(dias) { mutableStateOf(dias.indices.maxByOrNull { dias[it].kg }?.takeIf { maximo > 0 }) }
    val colorGrilla = MaterialTheme.colorScheme.outlineVariant
    val colorSuperficie = MaterialTheme.colorScheme.surface

    // Lectura del valor seleccionado (en tinta de texto, no en el color de la serie)
    Text(
        seleccionado?.let { "${dias[it].etiqueta}: ${formatearKg(dias[it].kg)} consumidos" }
            ?: if (maximo == 0.0) "Sin consumos registrados esta semana" else "Toca una columna para ver el detalle",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.height(8.dp))

    val descripcion = dias.joinToString(", ") { "${it.etiqueta} ${it.kg.toInt()} kg" }
    Box(
        Modifier
            .fillMaxWidth()
            .height(160.dp)
            .semantics { contentDescription = "Consumo diario: $descripcion" }
            .pointerInput(dias) {
                detectTapGestures { toque ->
                    val ancho = size.width / dias.size
                    seleccionado = (toque.x / ancho).toInt().coerceIn(0, dias.lastIndex)
                }
            },
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val ancho = size.width / dias.size
            val anchoBarra = (ancho * 0.55f).coerceAtMost(28.dp.toPx())
            // Línea base
            drawLine(colorGrilla, Offset(0f, size.height), Offset(size.width, size.height), strokeWidth = 1.dp.toPx())
            dias.forEachIndexed { i, dia ->
                if (maximo <= 0.0 || dia.kg <= 0.0) return@forEachIndexed
                val alto = (dia.kg / maximo).toFloat() * (size.height - 4.dp.toPx())
                val x = i * ancho + (ancho - anchoBarra) / 2
                val alfa = if (seleccionado == null || seleccionado == i) 1f else 0.45f
                drawRoundRect(
                    color = ColorConsumo.copy(alpha = alfa),
                    topLeft = Offset(x, size.height - alto),
                    size = Size(anchoBarra, alto),
                    cornerRadius = CornerRadius(4.dp.toPx()),
                )
                if (seleccionado == i) {
                    // Anillo del color de la superficie para destacar la columna elegida
                    drawRoundRect(
                        color = colorSuperficie,
                        topLeft = Offset(x - 1.dp.toPx(), size.height - alto - 1.dp.toPx()),
                        size = Size(anchoBarra + 2.dp.toPx(), alto + 2.dp.toPx()),
                        cornerRadius = CornerRadius(5.dp.toPx()),
                        style = Stroke(width = 2.dp.toPx()),
                    )
                }
            }
        }
    }
    Row(Modifier.fillMaxWidth().padding(top = 6.dp)) {
        dias.forEach { dia ->
            Text(
                dia.etiqueta,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun DashboardScreenPreview() {
    SiloMonitorAppTheme {
        DashboardScreen(
            silos = silosDemo,
            consumoSemana = listOf(
                ConsumoDia("mié 1", 1800.0), ConsumoDia("jue 2", 2100.0), ConsumoDia("vie 3", 0.0),
                ConsumoDia("sáb 4", 2600.0), ConsumoDia("dom 5", 1500.0), ConsumoDia("lun 6", 3100.0), ConsumoDia("mar 7", 900.0),
            ),
            solicitudesPendientes = 2,
            onVolver = {},
        )
    }
}
