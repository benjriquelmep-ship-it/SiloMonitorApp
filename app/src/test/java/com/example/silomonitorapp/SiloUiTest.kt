package com.example.silomonitorapp

import com.example.silomonitorapp.ui.model.EstadoSilo
import com.example.silomonitorapp.ui.model.SiloUi
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.Test

class SiloUiTest {

    @Test
    fun `crear silo con datos correctos calcula porcentaje, estado critico y autonomia`() {
        val silo = SiloUi(
            id = "SIL-001",
            codigo = "SIL-001",
            nombre = "Silo Norte A",
            granja = "Granja El Paico",
            galpon = "Galpón 1",
            capacidadMaxKg = 20_000.0,
            stockActualKg = 3_000.0,
            consumoPromedioDiarioKg = 1_500.0,
            latitud = -33.7012,
            longitud = -71.0034
        )

        silo.codigo shouldBe "SIL-001"
        silo.nombre shouldBe "Silo Norte A"
        silo.porcentaje shouldBe 15f
        silo.estado shouldBe EstadoSilo.CRITICO
        silo.horasAutonomia shouldBe 48.0
    }

    @Test
    fun `silo sobre 40 porciento queda en estado normal`() {
        val silo = SiloUi(
            id = "SIL-002",
            codigo = "SIL-002",
            nombre = "Silo Principal",
            granja = "Granja Pomaire",
            galpon = "Galpón 1",
            capacidadMaxKg = 30_000.0,
            stockActualKg = 21_000.0,
            consumoPromedioDiarioKg = 2_000.0,
            latitud = -33.6489,
            longitud = -71.1612
        )

        silo.porcentaje shouldBe 70f
        silo.estado shouldBe EstadoSilo.NORMAL
    }
}