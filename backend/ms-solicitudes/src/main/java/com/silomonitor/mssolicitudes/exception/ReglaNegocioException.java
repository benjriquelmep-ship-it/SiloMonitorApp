package com.silomonitor.mssolicitudes.exception;

// Se lanza cuando se rompe una regla del flujo de reposición (camión ya pendiente,
// solicitud ya revisada...) -> el handler responde 409 Conflict con el mensaje.
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}