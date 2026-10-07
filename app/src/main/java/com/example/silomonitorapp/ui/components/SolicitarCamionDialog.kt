package com.example.silomonitorapp.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.domain.ReglasTerreno
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.SiloUi

/**
 * Coordinar reposición: el Supervisor pide un camión para el silo.
 * Sugiere los kilos que faltan para llenarlo y marca urgente si el silo está crítico.
 */
@Composable
fun SolicitarCamionDialog(
    silo: SiloUi,
    error: String?,
    onDismiss: () -> Unit,
    onConfirmar: (kg: Double, urgente: Boolean, observacion: String) -> Unit,
) {
    val sugeridos = ReglasTerreno.kgSugeridosReposicion(silo.stockActualKg, silo.capacidadMaxKg)
    var kgTexto by rememberSaveable { mutableStateOf(sugeridos.toLong().toString()) }
    var urgente by rememberSaveable { mutableStateOf(silo.estado == EstadoSilo.CRITICO) }
    var observacion by rememberSaveable { mutableStateOf("") }
    val kg = kgTexto.replace(",", ".").toDoubleOrNull()

    AlertDialog(
        onDismissRequest = onDismiss,
        icon = { Icon(Icons.Filled.LocalShipping, contentDescription = null) },
        title = { Text("Solicitar camión") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    "${silo.nombre} · ${silo.granja}\nEspacio libre: ${formatearKg(sugeridos)}",
                    style = MaterialTheme.typography.bodyMedium,
                )
                OutlinedTextField(
                    value = kgTexto,
                    onValueChange = { kgTexto = it.filter { c -> c.isDigit() || c == '.' || c == ',' } },
                    label = { Text("Kilos a reponer") },
                    singleLine = true,
                    isError = error != null,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Urgente", fontWeight = FontWeight.SemiBold)
                        Text(
                            "Se prioriza en la bandeja de Jefatura",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Switch(checked = urgente, onCheckedChange = { urgente = it })
                }
                OutlinedTextField(
                    value = observacion,
                    onValueChange = { observacion = it },
                    label = { Text("Observación (opcional)") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 2,
                )
                error?.let {
                    Text(it, color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.SemiBold)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { kg?.let { onConfirmar(it, urgente, observacion) } },
                enabled = kg != null && kg > 0,
            ) { Text("Enviar solicitud") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancelar") }
        },
    )
}
