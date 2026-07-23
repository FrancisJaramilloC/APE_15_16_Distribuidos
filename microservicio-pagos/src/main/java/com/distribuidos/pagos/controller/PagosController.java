package com.distribuidos.pagos.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class PagosController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("servicio", "Microservicio de Pagos (Integrante 1)");
        response.put("puerto", 9001);
        response.put("cpu", 15.4);
        response.put("ram", 42.1);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

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
