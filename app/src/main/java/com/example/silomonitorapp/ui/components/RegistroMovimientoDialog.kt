package com.example.silomonitorapp.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.data.local.SiloEntity

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RegistroMovimientoDialog(
    silo: SiloEntity,
    onDismiss: () -> Unit,
    onConfirmar: (cantidadKg: Double, esCarga: Boolean) -> Unit
) {
    var cantidadTexto by remember { mutableStateOf("") }
    var esCarga by remember { mutableStateOf(true) }
    var errorValidacion by remember { mutableStateOf<String?>(null) }

    val stockDisponible = silo.nivelActual
    val espacioDisponible = silo.capacidadMaxima - silo.nivelActual

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(16.dp),
        title = {
            Column {
                Text(
                    text = "Registrar Movimiento",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${silo.nombre} (${silo.id}) - ${silo.granja}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Info de niveles actuales
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Nivel actual: ${stockDisponible.toInt()} kg / ${silo.capacidadMaxima.toInt()} kg",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = if (esCarga) "Espacio para carga: ${espacioDisponible.toInt()} kg"
                            else "Stock disponible para retiro: ${stockDisponible.toInt()} kg",
                            style = MaterialTheme.typography.bodySmall,
                            color = if (esCarga) Color(0xFF2E7D32) else Color(0xFFC62828)
                        )
                    }
                }

                // Selector de Operación (Carga / Consumo)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = esCarga,
                        onClick = {
                            esCarga = true
                            errorValidacion = null
                        },
                        label = { Text("Carga (+)") },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = !esCarga,
                        onClick = {
                            esCarga = false
                            errorValidacion = null
                        },
                        label = { Text("Consumo (-)") },
                        modifier = Modifier.weight(1f)
                    )
                }

                // Campo de entrada de kilos
                OutlinedTextField(
                    value = cantidadTexto,
                    onValueChange = {
                        cantidadTexto = it
                        errorValidacion = null
                    },
                    label = { Text("Cantidad (kg)") },
                    placeholder = { Text("Ej: 2000") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = errorValidacion != null,
                    supportingText = {
                        errorValidacion?.let {
                            Text(text = it, color = MaterialTheme.colorScheme.error)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val cantidad = cantidadTexto.toDoubleOrNull()
                    if (cantidad == null || cantidad <= 0) {
                        errorValidacion = "Ingrese una cantidad válida mayor a 0 kg"
                        return@Button
                    }

                    // Reglas de negocio de terreno:
                    if (esCarga && cantidad > espacioDisponible) {
                        errorValidacion = "Bloqueo: Excede la capacidad máxima por ${(cantidad - espacioDisponible).toInt()} kg."
                        return@Button
                    }
                    if (!esCarga && cantidad > stockDisponible) {
                        errorValidacion = "Bloqueo: Saldo negativo. Solo hay ${stockDisponible.toInt()} kg disponibles."
                        return@Button
                    }

                    onConfirmar(cantidad, esCarga)
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFD32F2F))
            ) {
                Text("Confirmar", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}