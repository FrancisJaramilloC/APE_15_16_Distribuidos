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

    @GetMapping({"/", "/pago", "/orden", "/api/orquestar"})
    public ResponseEntity<Map<String, Object>> orquestarPeticion() {
        long start = System.currentTimeMillis();

        try {
            // Llamada envuelta en Circuit Breaker
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

    @GetMapping("/api/circuit/status")
    public ResponseEntity<Map<String, Object>> getCircuitStatus() {
        return ResponseEntity.ok(circuitBreaker.getStatus());
    }

    @GetMapping("/api/circuit/logs")
    public ResponseEntity<List<Map<String, Object>>> getCircuitLogs() {
        return ResponseEntity.ok(databaseManager.getCircuitLogs());
    }
}
