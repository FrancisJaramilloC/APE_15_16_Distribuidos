package com.distribuidos.orquestador.circuit;

/**
 * Excepción personalizada lanzada cuando una solicitud es interceptada y rechazada
 * debido a que el Circuit Breaker se encuentra en estado ABIERTO (OPEN).
 */
public class CircuitBreakerOpenException extends RuntimeException {
    
    public CircuitBreakerOpenException(String message) {
        super(message);
    }
}
