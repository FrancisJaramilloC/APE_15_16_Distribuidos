package com.distribuidos.inventario.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class InventarioController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("servicio", "Microservicio de Inventario (Integrante 2)");
        response.put("puerto", 9002);
        response.put("cpu", 18.2);
        response.put("ram", 38.6);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/", "/consultar-stock", "/api/inventario/consultar"})
    public ResponseEntity<Map<String, Object>> consultarInventario() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("mensaje", "Stock verificado exitosamente por Microservicio de Inventario en puerto 9002");
        response.put("stockDisponible", 150);
        response.put("puerto", 9002);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
