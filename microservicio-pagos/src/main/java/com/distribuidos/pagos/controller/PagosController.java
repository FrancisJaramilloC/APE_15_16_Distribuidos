package com.distribuidos.pagos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el Microservicio de Pagos.
 * Expone los endpoints de diagnóstico de salud y procesamiento de transacciones.
 */
@RestController
@RequestMapping
public class PagosController {

    /**
     * Endpoint de Diagnóstico de Salud (Heartbeat Check).
     * Invocado periódicamente por el Microservicio Balanceador cada 2 segundos.
     * 
     * @return Respuesta JSON con estado HTTP 200 OK, métricas de consumo y timestamp.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("servicio", "Microservicio de Pagos (Integrante 3)");
        response.put("puerto", 9001);
        response.put("cpu", 15.4); // Uso simulado de CPU en %
        response.put("ram", 42.1); // Uso simulado de RAM en %
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de Negocio para Procesar Pagos.
     * Invocado por el Balanceador cuando reenvía una solicitud del cliente.
     * Mapeado a múltiples rutas para evitar errores 404 de enrutamiento.
     * 
     * @return Respuesta JSON confirmando el procesamiento exitoso de la transacción.
     */
    @GetMapping({"/", "/procesar", "/procesar-pago", "/api/pagos/procesar"})
    public ResponseEntity<Map<String, Object>> procesarPago() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("mensaje", "Pago procesado exitosamente por Microservicio de Pagos en puerto 9001");
        response.put("transaccionId", "TX-PAGOS-" + System.currentTimeMillis());
        response.put("puerto", 9001);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
