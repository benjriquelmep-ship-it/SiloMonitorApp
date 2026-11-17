package com.silomonitor.mssilos.exception;

// Se lanza cuando el silo pedido no existe -> el handler responde 404 Not Found.
public class RecursoNoEncontradoException extends RuntimeException {
    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}