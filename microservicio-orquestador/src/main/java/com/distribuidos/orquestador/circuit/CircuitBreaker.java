package com.distribuidos.orquestador.circuit;

import com.distribuidos.orquestador.database.DatabaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Supplier;

/**
 * Implementación del Patrón de Tolerancia a Fallos Circuit Breaker.
 * Funciona como una Máquina de Estados Finitos con 3 estados:
 * 
 * - CLOSED (Cerrado): El sistema opera normalmente. Las solicitudes fluyen hacia la red.
 * - OPEN (Abierto): Se detectaron 3 fallos consecutivos. Corta la comunicación de red y devuelve respuestas Fallback inmediatas.
 * - HALF_OPEN (Semi-Abierto): Transcurridos 10 segundos de cooldown, prueba una solicitud para evaluar la recuperación autónoma del cluster.
 */
@Component
public class CircuitBreaker {

    private final String serviceName = "ServicioOrquestador";
    private final int failureThreshold = 3;        // Umbral de 3 fallos consecutivos para abrir el circuito
    private final long recoveryTimeoutMs = 10000; // 10 segundos de enfriamiento (cooldown)

    private String state = "CLOSED"; // Estado inicial del circuito
    private int failureCount = 0;    // Contador acumulado de fallos consecutivos
    private int successCount = 0;    // Contador acumulado de éxitos
    private long lastStateChange = System.currentTimeMillis(); // Timestamp del último cambio de estado

    @Autowired
    private DatabaseManager databaseManager;

    /**
     * Realiza la transición de estado de forma thread-safe y registra el evento en SQLite (circuit_log).
     */
    private synchronized void transitionTo(String newState, String reason) {
        String oldState = this.state;
        if (!oldState.equals(newState)) {
            this.state = newState;
            this.lastStateChange = System.currentTimeMillis();
            
            if ("CLOSED".equals(newState)) {
                this.failureCount = 0;
            }

            // Persiste el cambio de estado en la tabla circuit_log de SQLite
            if (databaseManager != null) {
                databaseManager.logCircuitTransition(serviceName, oldState, newState, reason);
            }
        }
    }

    /**
     * Evalúa si una solicitud puede ejecutarse o si debe bloquearse.
     * Si el circuito está OPEN y han pasado más de 10s, conmuta automáticamente a HALF_OPEN.
     */
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

    /**
     * Registra una respuesta exitosa.
     * Si el circuito estaba en HALF_OPEN, se restablece a CLOSED.
     */
    public synchronized void recordSuccess() {
        this.successCount++;
        if ("HALF_OPEN".equals(state)) {
            transitionTo("CLOSED", "Prueba en HALF_OPEN exitosa. Circuito restablecido a CLOSED.");
        } else {
            this.failureCount = 0;
        }
    }

    /**
     * Registra una falla o timeout en la llamada.
     * Si se alcanza el umbral de 3 fallos en CLOSED, abre el circuito a OPEN.
     */
    public synchronized void recordFailure(String errorMsg) {
        this.failureCount++;
        if ("HALF_OPEN".equals(state)) {
            transitionTo("OPEN", "Prueba en HALF_OPEN falló (" + errorMsg + "). Circuito regresa a OPEN.");
        } else if ("CLOSED".equals(state) && failureCount >= failureThreshold) {
            transitionTo("OPEN", "Se alcanzó el umbral de " + failureThreshold + " fallos consecutivos. Circuito ABIERTO.");
        }
    }

    /**
     * Encapsula la ejecución de una función de red dentro del Circuit Breaker.
     * Lanza CircuitBreakerOpenException si el circuito se encuentra ABIERTO.
     */
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

    /**
     * Retorna el mapa de estado actual con el tiempo restante de cooldown en segundos.
     */
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
