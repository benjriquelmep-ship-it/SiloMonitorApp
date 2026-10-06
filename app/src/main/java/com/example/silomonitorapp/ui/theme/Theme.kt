package com.example.silomonitorapp.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = RojoAriztia,
    onPrimary = Color.White,
    primaryContainer = Color(0xFFFFDAD6),
    onPrimaryContainer = Color(0xFF410002),
    secondary = AmarilloMostaza,
    onSecondary = TextoPrincipal,
    secondaryContainer = Color(0xFFFFF3C4),
    onSecondaryContainer = TextoPrincipal,
    background = GrisHielo,
    onBackground = TextoPrincipal,
    surface = BlancoTarjeta,
    onSurface = TextoPrincipal,
    surfaceVariant = Color(0xFFF1F3F4),
    onSurfaceVariant = TextoSecundario,
    error = SemaforoCritico,
)

private val DarkColorScheme = darkColorScheme(
    primary = RojoAriztiaClaro,
    onPrimary = Color.White,
    secondary = AmarilloMostaza,
    onSecondary = TextoPrincipal,
    background = Color(0xFF121212),
    surface = Color(0xFF1E1E1E),
)

// Tarjetas y superficies con bordes redondeados de 12dp
private val AriztiaShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
)

@Composable
fun SiloMonitorAppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    // Sin color dinámico: la app siempre respeta la paleta corporativa
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography,
        shapes = AriztiaShapes,
        content = content
    )
}
