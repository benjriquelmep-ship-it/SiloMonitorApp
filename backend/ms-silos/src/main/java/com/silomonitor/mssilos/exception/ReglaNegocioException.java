package com.silomonitor.mssilos.exception;

// Se lanza cuando se rompe una regla de terreno (sobrellenado, saldo negativo,
// código repetido...) -> el handler responde 409 Conflict con el mensaje.
public class ReglaNegocioException extends RuntimeException {
    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}