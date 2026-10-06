package com.example.silomonitorapp.ui.screens

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PhotoCamera
import androidx.compose.material.icons.filled.Save
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.components.AriztiaTopBar
import com.example.silomonitorapp.ui.components.BarraNivelAnimada
import com.example.silomonitorapp.ui.components.MiniaturaFoto
import com.example.silomonitorapp.ui.components.formatearKg
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.TipoMovimiento
import com.example.silomonitorapp.ui.model.silosDemo
import com.example.silomonitorapp.ui.theme.SiloMonitorAppTheme

/**
 * Campos visuales del formulario de Carga / Consumo.
 * Las reglas de validación (sobrellenado y saldo negativo) viven en el ViewModel:
 * esta pantalla solo muestra el [error] que se le entregue.
 */
@Composable
fun MovimientoFormScreen(
    silo: SiloUi?,
    error: String?,
    onVolver: () -> Unit,
    onGuardar: (tipo: TipoMovimiento, kg: Double, observacion: String) -> Unit,
    onAdjuntarFoto: () -> Unit,
    fotoUri: Uri? = null,
) {
    var tipo by rememberSaveable { mutableStateOf(TipoMovimiento.CARGA) }
    var kgTexto by rememberSaveable { mutableStateOf("") }
    var observacion by rememberSaveable { mutableStateOf("") }
    val kg = kgTexto.replace(",", ".").toDoubleOrNull()

    Scaffold(
        topBar = { AriztiaTopBar(titulo = "Registrar movimiento", onVolver = onVolver) },
        containerColor = MaterialTheme.colorScheme.background,
    ) { padding ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (silo != null) {
                Card(
                    shape = MaterialTheme.shapes.medium,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                ) {
                    Column(Modifier.padding(16.dp)) {
                        Text(silo.nombre, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text(
                            "${silo.codigo} · ${silo.granja}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        Spacer(Modifier.height(12.dp))
                        BarraNivelAnimada(porcentaje = silo.porcentaje)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Stock: ${formatearKg(silo.stockActualKg)} de ${formatearKg(silo.capacidadMaxKg)}",
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }

            Text("Tipo de movimiento", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.SemiBold)
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                TipoMovimiento.entries.forEachIndexed { indice, opcion ->
                    SegmentedButton(
                        selected = tipo == opcion,
                        onClick = { tipo = opcion },
                        shape = SegmentedButtonDefaults.itemShape(indice, TipoMovimiento.entries.size),
                    ) { Text(opcion.etiqueta) }
                }
            }

            OutlinedTextField(
                value = kgTexto,
                onValueChange = { kgTexto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                label = { Text("Cantidad (kg)") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                isError = error != null,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                shape = MaterialTheme.shapes.medium,
            )

            OutlinedTextField(
                value = observacion,
                onValueChange = { observacion = it },
                label = { Text("Observación (opcional)") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3,
                shape = MaterialTheme.shapes.medium,
            )

            OutlinedButton(
                onClick = onAdjuntarFoto,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.PhotoCamera, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (fotoUri == null) "Adjuntar foto de evidencia" else "Cambiar foto de evidencia")
            }

            fotoUri?.let { MiniaturaFoto(it) }

            AnimatedVisibility(visible = error != null) {
                Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.12f))) {
                    Text(
                        error.orEmpty(),
                        modifier = Modifier.padding(16.dp),
                        color = MaterialTheme.colorScheme.error,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }

            Button(
                onClick = { kg?.let { onGuardar(tipo, it, observacion.trim()) } },
                enabled = kg != null && kg > 0,
                modifier = Modifier.fillMaxWidth().height(52.dp),
                shape = MaterialTheme.shapes.medium,
            ) {
                Icon(Icons.Filled.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Guardar ${tipo.etiqueta.lowercase()}")
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MovimientoFormScreenPreview() {
    SiloMonitorAppTheme {
        MovimientoFormScreen(
            silo = silosDemo[0],
            error = null,
            onVolver = {},
            onGuardar = { _, _, _ -> },
            onAdjuntarFoto = {},
        )
    }
}
