package com.example.silomonitorapp.notificaciones

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.silomonitorapp.MainActivity
import com.example.silomonitorapp.data.local.SiloEntity

/** Notificaciones locales del sistema para silos en nivel crítico (< 20%). */
object NotificadorAlertas {

    private const val CANAL_CRITICO = "silos_nivel_critico"

    fun crearCanal(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val canal = NotificationChannel(
            CANAL_CRITICO,
            "Silos en nivel crítico",
            NotificationManager.IMPORTANCE_HIGH
        ).apply {
            description = "Avisa cuando el alimento de un silo cae bajo el 20% de su capacidad"
        }
        context.getSystemService(NotificationManager::class.java).createNotificationChannel(canal)
    }

    fun notificarNivelCritico(context: Context, silo: SiloEntity) {
        val tienePermiso = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED
        if (!tienePermiso) return

        val porcentaje = (silo.nivelActual / silo.capacidadMaxima * 100).toInt()

        val abrirApp = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notificacion = NotificationCompat.Builder(context, CANAL_CRITICO)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle("Nivel crítico: ${silo.nombre} ($porcentaje%)")
            .setContentText("${silo.granja} · ${silo.galpon}. Coordinar reposición de alimento.")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setContentIntent(abrirApp)
            .setAutoCancel(true)
            .build()

        // Un id por silo: si vuelve a bajar, se actualiza la misma notificación
        NotificationManagerCompat.from(context).notify(silo.id.hashCode(), notificacion)
    }
}
