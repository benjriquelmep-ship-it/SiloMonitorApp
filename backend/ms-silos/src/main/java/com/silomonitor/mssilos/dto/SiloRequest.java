package com.silomonitor.mssilos.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;

// DTO (Data Transfer Object): lo que la app manda en el body al crear o editar un silo.
// Las anotaciones validan el JSON antes de llegar al servicio (@Valid en el controller).
public record SiloRequest(
        @NotBlank(message = "El código es obligatorio") String id,
        @NotBlank(message = "El nombre es obligatorio") String nombre,
        @NotBlank(message = "La granja es obligatoria") String granja,
        String galpon,
        String tipoAlimento,
        @NotNull @Positive(message = "La capacidad debe ser mayor a 0") Double capacidadMaxima,
        @NotNull @PositiveOrZero(message = "El stock no puede ser negativo") Double nivelActual,
        @NotNull @PositiveOrZero(message = "El consumo no puede ser negativo") Double consumoPromedioDiario,
        @NotNull @DecimalMin("-90") @DecimalMax("90") Double latitud,
        @NotNull @DecimalMin("-180") @DecimalMax("180") Double longitud
) {
}