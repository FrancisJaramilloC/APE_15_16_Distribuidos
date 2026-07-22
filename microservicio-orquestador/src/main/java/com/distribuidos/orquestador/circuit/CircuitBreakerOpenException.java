package com.distribuidos.orquestador.circuit;

public class CircuitBreakerOpenException extends RuntimeException {
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
