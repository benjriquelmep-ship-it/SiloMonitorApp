package com.example.silomonitorapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.theme.PistaBarra

private const val DURACION_ANIMACION_MS = 1200

/**
 * Anima el nivel (0f..1f) desde 0 al aparecer y luego sube/baja
 * cada vez que cambia el porcentaje del silo.
 */
@Composable
private fun nivelAnimado(porcentaje: Float): Float {
    val nivel = remember { Animatable(0f) }
    LaunchedEffect(porcentaje) {
        nivel.animateTo(
            targetValue = porcentaje.coerceIn(0f, 100f) / 100f,
            animationSpec = tween(DURACION_ANIMACION_MS, easing = FastOutSlowInEasing),
        )
    }
    return nivel.value
}

/** El color acompaña al nivel animado, cruzando el semáforo en tiempo real. */
@Composable
private fun colorAnimado(fraccion: Float): Color {
    val color by animateColorAsState(
        targetValue = EstadoSilo.desdePorcentaje(fraccion * 100f).color,
        animationSpec = tween(400),
        label = "colorNivel",
    )
    return color
}

/** Barra de progreso horizontal para las tarjetas de la lista. */
@Composable
fun BarraNivelAnimada(
    porcentaje: Float,
    modifier: Modifier = Modifier,
    alto: Dp = 12.dp,
) {
    val fraccion = nivelAnimado(porcentaje)
    val color = colorAnimado(fraccion)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(alto)
            .clip(RoundedCornerShape(50))
            .background(PistaBarra),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraccion)
                .clip(RoundedCornerShape(50))
                .background(color),
        )
    }
}

/** Silo vertical que se llena desde abajo, usado en la ficha del silo. */
@Composable
fun SiloNivelVertical(
    porcentaje: Float,
    modifier: Modifier = Modifier,
    ancho: Dp = 110.dp,
    alto: Dp = 220.dp,
) {
    val fraccion = nivelAnimado(porcentaje)
    val color = colorAnimado(fraccion)
    val forma = RoundedCornerShape(topStart = 48.dp, topEnd = 48.dp, bottomStart = 16.dp, bottomEnd = 16.dp)
    Box(
        modifier = modifier
            .width(ancho)
            .height(alto)
            .clip(forma)
            .background(PistaBarra)
            .border(3.dp, MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f), forma),
        contentAlignment = Alignment.BottomCenter,
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .fillMaxHeight(fraccion)
                .background(color),
        )
        Text(
            text = "${(fraccion * 100).toInt()}%",
            modifier = Modifier.align(Alignment.Center),
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = if (fraccion > 0.55f) Color.White else MaterialTheme.colorScheme.onSurface,
        )
    }
}
