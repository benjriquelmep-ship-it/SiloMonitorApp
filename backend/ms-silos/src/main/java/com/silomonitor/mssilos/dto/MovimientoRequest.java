package com.silomonitor.mssilos.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;

// Body para registrar una carga o un consumo.
// fecha es opcional: si la app la manda (movimiento hecho sin internet) se respeta; si no, se usa "ahora".
public record MovimientoRequest(
        @NotNull @Pattern(regexp = "CARGA|CONSUMO", message = "El tipo debe ser CARGA o CONSUMO") String tipo,
        @NotNull @Positive(message = "Los kilos deben ser mayores a 0") Double cantidadKg,
        String observacion,
        Long fecha
) {
}