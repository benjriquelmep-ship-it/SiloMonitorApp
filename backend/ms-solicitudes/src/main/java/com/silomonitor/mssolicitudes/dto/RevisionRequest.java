package com.silomonitor.mssolicitudes.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

// Body de PATCH /api/solicitudes/{id}/revision: la Jefatura aprueba (true) o rechaza (false).
// fechaRevision es opcional (se respeta si la revisión se hizo sin internet).
public record RevisionRequest(
        @NotNull(message = "Debe indicar si se aprueba o se rechaza") Boolean aprobada,
        @NotBlank(message = "Debe indicar quién revisa") String revisadoPor,
        Long fechaRevision
) {
}