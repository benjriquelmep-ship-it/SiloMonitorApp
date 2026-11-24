package com.silomonitor.mssolicitudes.exception;

// Se lanza cuando la solicitud pedida no existe -> el handler responde 404 Not Found.
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}