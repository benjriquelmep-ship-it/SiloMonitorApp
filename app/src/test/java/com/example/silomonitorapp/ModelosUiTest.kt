package com.example.silomonitorapp

import com.example.silomonitorapp.ui.model.ConsumoDia
import com.example.silomonitorapp.ui.model.DatosSilo
import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.EstadoSolicitud
import com.example.silomonitorapp.ui.model.MovimientoUi
import com.example.silomonitorapp.ui.model.SiloUi
import com.example.silomonitorapp.ui.model.SolicitudUi
import com.example.silomonitorapp.ui.model.TipoMovimiento
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

// PRUEBAS UNITARIAS de los modelos de UI: semáforo, porcentaje, autonomía y etiquetas.
class ModelosUiTest {

    private fun silo(capacidad: Double, stock: Double, consumo: Double) = SiloUi(
        id = "SIL-001", codigo = "SIL-001", nombre = "Silo", granja = "Granja", galpon = "Galpón 1",
        capacidadMaxKg = capacidad, stockActualKg = stock, consumoPromedioDiarioKg = consumo,
        latitud = -33.7, longitud = -71.0
    )

    @Test
    fun `semaforo respeta los limites de 20 y 40 por ciento`() {
        EstadoSilo.desdePorcentaje(19.9f) shouldBe EstadoSilo.CRITICO
        EstadoSilo.desdePorcentaje(20f) shouldBe EstadoSilo.ADVERTENCIA
        EstadoSilo.desdePorcentaje(40f) shouldBe EstadoSilo.ADVERTENCIA
        EstadoSilo.desdePorcentaje(40.1f) shouldBe EstadoSilo.NORMAL
    }

    @Test
    fun `silo en advertencia calcula porcentaje y autonomia`() {
        val s = silo(capacidad = 10_000.0, stock = 3_000.0, consumo = 1_000.0)

        s.porcentaje shouldBe 30f
        s.estado shouldBe EstadoSilo.ADVERTENCIA
        s.horasAutonomia shouldBe 72.0
    }

    @Test
    fun `capacidad cero da porcentaje cero y sin consumo no hay autonomia`() {
        val s = silo(capacidad = 0.0, stock = 500.0, consumo = 0.0)

        s.porcentaje shouldBe 0f
        s.estado shouldBe EstadoSilo.CRITICO
        s.horasAutonomia shouldBe null
    }

    @Test
    fun `porcentaje nunca supera el 100`() {
        silo(capacidad = 1_000.0, stock = 1_500.0, consumo = 100.0).porcentaje shouldBe 100f
    }

    @Test
    fun `etiquetas de los enums`() {
        EstadoSilo.NORMAL.etiqueta shouldBe "Normal"
        EstadoSilo.ADVERTENCIA.etiqueta shouldBe "Advertencia"
        EstadoSilo.CRITICO.etiqueta shouldBe "Crítico"
        TipoMovimiento.CARGA.etiqueta shouldBe "Carga"
        TipoMovimiento.CONSUMO.etiqueta shouldBe "Consumo"
        EstadoSolicitud.PENDIENTE.etiqueta shouldBe "Pendiente"
        EstadoSolicitud.APROBADA.etiqueta shouldBe "Aprobada"
        EstadoSolicitud.RECHAZADA.etiqueta shouldBe "Rechazada"
        EstadoSolicitud.valueOf("APROBADA") shouldBe EstadoSolicitud.APROBADA
    }

    @Test
    fun `modelos de formulario historial bandeja y grafico guardan sus datos`() {
        val datos = DatosSilo("SIL-002", "Silo", "Granja", "Galpón", "Engorda", 1_000.0, 500.0, 100.0, -33.0, -71.0)
        datos.codigo shouldBe "SIL-002"
        datos.tipoAlimento shouldBe "Engorda"
        datos.copy(nombre = "Otro").nombre shouldBe "Otro"

        val movimiento = MovimientoUi(1, TipoMovimiento.CARGA, 250.0, 10L, "Obs", null, true)
        movimiento.cantidadKg shouldBe 250.0
        movimiento.pendienteSincronizar shouldBe true

        val solicitud = SolicitudUi(
            id = 3, siloId = "SIL-002", siloNombre = "Silo", granja = "Granja", kgSolicitados = 400.0,
            urgente = true, observacion = "", solicitadoPor = "supervisor", fechaSolicitud = 1L,
            estado = EstadoSolicitud.PENDIENTE, revisadoPor = null
        )
        solicitud.urgente shouldBe true
        solicitud.estado shouldBe EstadoSolicitud.PENDIENTE

        val dia = ConsumoDia("lun 24", 1_200.0)
        dia.etiqueta shouldBe "lun 24"
        dia.kg shouldBe 1_200.0
    }
}