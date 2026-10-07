package com.example.silomonitorapp.data.reports

import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import androidx.core.content.FileProvider
import com.example.silomonitorapp.ui.model.SiloUi
import java.io.File
import java.io.FileOutputStream
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object ReporteHelper {

    /** Genera un archivo CSV con el inventario actual de silos */
    fun generarCsvInventario(context: Context, silos: List<SiloUi>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val nombreArchivo = "Reporte_Silos_$timeStamp.csv"
        val directorio = File(context.cacheDir, "reportes").apply { if (!exists()) mkdirs() }
        val archivo = File(directorio, nombreArchivo)

        FileWriter(archivo).use { writer ->
            writer.append("Codigo;Nombre;Granja;Galpon;Capacidad Max (Kg);Stock Actual (Kg);Porcentaje;Estado;Autonomia (Horas)\n")
            for (silo in silos) {
                writer.append("${silo.codigo};")
                writer.append("${silo.nombre};")
                writer.append("${silo.granja};")
                writer.append("${silo.galpon};")
                writer.append("${silo.capacidadMaxKg};")
                writer.append("${silo.stockActualKg};")
                writer.append("${silo.porcentaje.toInt()}%;")
                writer.append("${silo.estado.etiqueta};")
                writer.append("${silo.horasAutonomia?.toInt() ?: "N/D"}\n")
            }
        }
        return archivo
    }

    /** Genera un archivo PDF con formato oficial de Ariztía */
    fun generarPdfInventario(context: Context, silos: List<SiloUi>): File {
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val fechaLegible = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()).format(Date())
        val nombreArchivo = "Informe_Silos_$timeStamp.pdf"
        val directorio = File(context.cacheDir, "reportes").apply { if (!exists()) mkdirs() }
        val archivo = File(directorio, nombreArchivo)

        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create() // Formato A4 estándar
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        val paint = Paint()
        paint.isAntiAlias = true

        // Encabezado institucional rojo
        paint.color = Color.rgb(211, 47, 47)
        canvas.drawRect(0f, 0f, 595f, 70f, paint)

        // Título del reporte
        paint.color = Color.WHITE
        paint.textSize = 18f
        paint.isFakeBoldText = true
        canvas.drawText("ARIZTÍA - INFORME ESTADO DE SILOS", 24f, 42f, paint)

        // Subtítulo con fecha
        paint.textSize = 10f
        paint.isFakeBoldText = false
        canvas.drawText("Fecha de emisión: $fechaLegible", 24f, 58f, paint)

        // Resumen métricas
        paint.color = Color.DKGRAY
        paint.textSize = 11f
        paint.isFakeBoldText = true
        val stockTotal = silos.sumOf { it.stockActualKg }
        val capTotal = silos.sumOf { it.capacidadMaxKg }
        val criticos = silos.count { it.porcentaje < 20.0 }
        canvas.drawText("Total Silos: ${silos.size}   |   Stock Total: ${stockTotal.toInt()} Kg / ${capTotal.toInt()} Kg   |   Críticos: $criticos", 24f, 95f, paint)

        // Encabezados de tabla
        paint.color = Color.LTGRAY
        canvas.drawRect(24f, 110f, 571f, 130f, paint)

        paint.color = Color.BLACK
        paint.textSize = 9f
        paint.isFakeBoldText = true
        canvas.drawText("CÓDIGO", 30f, 124f, paint)
        canvas.drawText("NOMBRE", 80f, 124f, paint)
        canvas.drawText("GRANJA", 190f, 124f, paint)
        canvas.drawText("STOCK (KG)", 310f, 124f, paint)
        canvas.drawText("NIVEL", 410f, 124f, paint)
        canvas.drawText("ESTADO", 480f, 124f, paint)

        // Filas de silos
        paint.isFakeBoldText = false
        var y = 148f
        for (silo in silos) {
            if (y > 800f) break
            canvas.drawText(silo.codigo, 30f, y, paint)
            canvas.drawText(silo.nombre.take(18), 80f, y, paint)
            canvas.drawText(silo.granja.take(18), 190f, y, paint)
            canvas.drawText("${silo.stockActualKg.toInt()} / ${silo.capacidadMaxKg.toInt()}", 310f, y, paint)
            canvas.drawText("${silo.porcentaje.toInt()}%", 410f, y, paint)

            val colorTexto = when {
                silo.porcentaje < 20.0 -> Color.rgb(211, 47, 47)
                silo.porcentaje < 50.0 -> Color.rgb(230, 149, 0)
                else -> Color.rgb(46, 125, 50)
            }
            val paintEstado = Paint(paint).apply { color = colorTexto; isFakeBoldText = true }
            canvas.drawText(silo.estado.etiqueta, 480f, y, paintEstado)

            y += 20f
        }

        pdfDocument.finishPage(page)
        FileOutputStream(archivo).use { output ->
            pdfDocument.writeTo(output)
        }
        pdfDocument.close()
        return archivo
    }

    /** Abre el selector del sistema para compartir el documento */
    fun compartirArchivo(context: Context, archivo: File, mimeType: String) {
        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            archivo
        )

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            putExtra(Intent.EXTRA_SUBJECT, "Reporte Silos Ariztía - ${archivo.name}")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        context.startActivity(Intent.createChooser(intent, "Exportar / Compartir"))
    }
}