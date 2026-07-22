package com.distribuidos.orquestador.circuit;

import com.distribuidos.orquestador.database.DatabaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

@Component
public class CircuitBreaker {

    private final String serviceName = "ServicioOrquestador";
    private final int failureThreshold = 3;        // Umbral de 3 fallos consecutivos
    private final long recoveryTimeoutMs = 10000; // 10 segundos de cooldown

    private String state = "CLOSED"; // CLOSED, OPEN, HALF_OPEN
    private int failureCount = 0;
    private int successCount = 0;
    private long lastStateChange = System.currentTimeMillis();

    @Autowired
    private DatabaseManager databaseManager;

    private synchronized void transitionTo(String newState, String reason) {
        String oldState = this.state;
        if (!oldState.equals(newState)) {
            this.state = newState;
            this.lastStateChange = System.currentTimeMillis();
            if ("CLOSED".equals(newState)) {
                this.failureCount = 0;
            }

            if (databaseManager != null) {
                databaseManager.logCircuitTransition(serviceName, oldState, newState, reason);
            }
        }
    }

    public synchronized boolean canExecute() {
        if ("OPEN".equals(state)) {
            long elapsed = System.currentTimeMillis() - lastStateChange;
            if (elapsed >= recoveryTimeoutMs) {
                transitionTo("HALF_OPEN", "Pasaron 10s en OPEN. Probando recuperación (HALF_OPEN).");
                return true;
            }
            return false;
        }
        return true;
    }

    public synchronized void recordSuccess() {
        this.successCount++;
        if ("HALF_OPEN".equals(state)) {
            transitionTo("CLOSED", "Prueba en HALF_OPEN exitosa. Circuito restablecido a CLOSED.");
        } else {
            this.failureCount = 0;
        }
    }

    public synchronized void recordFailure(String errorMsg) {
        this.failureCount++;
        if ("HALF_OPEN".equals(state)) {
            transitionTo("OPEN", "Prueba en HALF_OPEN falló (" + errorMsg + "). Circuito regresa a OPEN.");
        } else if ("CLOSED".equals(state) && failureCount >= failureThreshold) {
            transitionTo("OPEN", "Se alcanzó el umbral de " + failureThreshold + " fallos consecutivos. Circuito ABIERTO.");
        }
    }

    public <T> T execute(Supplier<T> supplier) {
        if (!canExecute()) {
            throw new CircuitBreakerOpenException("Circuito ABIERTO (OPEN). Petición bloqueada para evitar fallos en cascada.");
        }

        try {
            T result = supplier.get();
            recordSuccess();
            return result;
        } catch (Exception e) {
            recordFailure(e.getMessage());
            throw e;
        }
    }

    public synchronized String getState() {
        return state;
    }

    public synchronized Map<String, Object> getStatus() {
        Map<String, Object> status = new HashMap<>();
        long elapsed = System.currentTimeMillis() - lastStateChange;
        long timeRemaining = "OPEN".equals(state) ? Math.max(0, (recoveryTimeoutMs - elapsed) / 1000) : 0;

        status.put("servicio", serviceName);
        status.put("estado", state);
        status.put("fallosConsecutivos", failureCount);
        status.put("umbralFallos", failureThreshold);
        status.put("cooldownSegundos", recoveryTimeoutMs / 1000);
        status.put("tiempoRestanteCooldownSegundos", timeRemaining);
        return status;
    }
}
