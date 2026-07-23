package com.distribuidos.orquestador.controller;

import com.distribuidos.orquestador.circuit.CircuitBreaker;
import com.distribuidos.orquestador.circuit.CircuitBreakerOpenException;
import com.distribuidos.orquestador.database.DatabaseManager;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador Principal del Orquestador.
 * Procesa las peticiones entrantes del cliente envolviéndolas dentro del Circuit Breaker.
 * Si el circuito se abre, retorna una respuesta alternativa (Fallback) de forma inmediata.
 */
@RestController
@RequestMapping
public class OrquestadorController {

    @Autowired
    private CircuitBreaker circuitBreaker;

    @Autowired
    private DatabaseManager databaseManager;

    @Value("${balancer.url:http://127.0.0.1:8000}")
    private String balancerUrl;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Endpoint Principal de Orquestación (/pago).
     * Invoca al Balanceador protegido por el Circuit Breaker.
     * 
     * @return Respuesta HTTP 200 OK si el backend responde, o HTTP 503 FALLBACK si el circuito está OPEN.
     */
    @GetMapping({"/", "/pago", "/orden", "/api/orquestar"})
    public ResponseEntity<Map<String, Object>> orquestarPeticion() {
        long start = System.currentTimeMillis();

        try {
            // Invoca la llamada de red envuelta dentro de la máquina de estados del Circuit Breaker
            String responseBody = circuitBreaker.execute(() -> {
                String url = balancerUrl + "/balance/procesar";
                return restTemplate.getForObject(url, String.class);
            });

            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> result = new HashMap<>();
            result.put("status", "SUCCESS");
            result.put("microservicio", "Orquestador (Spring Boot)");
            result.put("circuitState", circuitBreaker.getState());
            result.put("tiempoRespuestaMs", elapsed);
            result.put("respuestaBackend", responseBody);

            return ResponseEntity.ok(result);

        } catch (CircuitBreakerOpenException e) {
            // Manejo del Fallback cuando el Circuit Breaker intercepta y bloquea la llamada (Estado OPEN)
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> fallback = new HashMap<>();
            fallback.put("status", "FALLBACK");
            fallback.put("circuitState", "OPEN");
            fallback.put("error", "Circuit Breaker ABIERTO (OPEN)");
            fallback.put("mensaje", "Servicio no disponible por protección de Circuit Breaker. Respuesta alternativa entregada inmediatamente.");
            fallback.put("tiempoRespuestaMs", elapsed);

            Map<String, Object> fallbackData = new HashMap<>();
            fallbackData.put("transaccionId", "FALLBACK-SPRINGBOOT-999");
            fallbackData.put("mensaje", "Su solicitud ha sido encolada de forma segura.");
            fallback.put("fallbackData", fallbackData);

            // Retorna HTTP 503 SERVICE UNAVAILABLE con el payload de Fallback en < 0.01ms
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(fallback);

        } catch (Exception e) {
            long elapsed = System.currentTimeMillis() - start;

            Map<String, Object> errorResult = new HashMap<>();
            errorResult.put("status", "ERROR");
            errorResult.put("circuitState", circuitBreaker.getState());
            errorResult.put("error", e.getMessage());
            errorResult.put("tiempoRespuestaMs", elapsed);

            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorResult);
        }
    }

    /**
     * Endpoint REST para consultar el estado en tiempo real del Circuit Breaker.
     */
    @GetMapping("/api/circuit/status")
    public ResponseEntity<Map<String, Object>> getCircuitStatus() {
        return ResponseEntity.ok(circuitBreaker.getStatus());
    }

    /**
     * Endpoint REST para consultar las transiciones registradas en la tabla circuit_log de SQLite.
     */
    @GetMapping("/api/circuit/logs")
    public ResponseEntity<List<Map<String, Object>>> getCircuitLogs() {
        return ResponseEntity.ok(databaseManager.getCircuitLogs());
    }
}
