package com.silomonitor.mssolicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

// Body de POST /api/solicitudes.
// fechaSolicitud es opcional: si la app la manda (solicitud hecha sin internet) se respeta; si no, se usa "ahora".
public record SolicitudRequest(
        @NotBlank(message = "El código del silo es obligatorio") String siloId,
        @NotNull @Positive(message = "Los kilos deben ser mayores a 0") Double kgSolicitados,
        Boolean urgente,
        String observacion,
        @NotBlank(message = "Debe indicar quién solicita") String solicitadoPor,
        Long fechaSolicitud
) {
}