package com.example.silomonitorapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.theme.SemaforoAdvertencia
import com.example.silomonitorapp.ui.theme.SemaforoCritico
import com.example.silomonitorapp.ui.theme.SemaforoNormal
import com.google.android.gms.maps.model.BitmapDescriptorFactory

val EstadoSilo.color: Color
    get() = when (this) {
        EstadoSilo.NORMAL -> SemaforoNormal
        EstadoSilo.ADVERTENCIA -> SemaforoAdvertencia
        EstadoSilo.CRITICO -> SemaforoCritico
    }

/** Tono del marcador de Google Maps según el semáforo. */
val EstadoSilo.tonoMarcador: Float
    get() = when (this) {
        EstadoSilo.NORMAL -> BitmapDescriptorFactory.HUE_GREEN
        EstadoSilo.ADVERTENCIA -> BitmapDescriptorFactory.HUE_ORANGE
        EstadoSilo.CRITICO -> BitmapDescriptorFactory.HUE_RED
    }

@Composable
fun EstadoBadge(estado: EstadoSilo, modifier: Modifier = Modifier) {
    Row(
        modifier = modifier
            .background(estado.color.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(Modifier.size(8.dp).background(estado.color, CircleShape))
        Text(
            text = estado.etiqueta,
            color = estado.color,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
        )
    }
}
