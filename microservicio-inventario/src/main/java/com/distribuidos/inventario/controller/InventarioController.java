package com.distribuidos.inventario.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el Microservicio de Inventario.
 * Expone endpoints de monitoreo de salud y verificación de stock de productos.
 */
@RestController
@RequestMapping
public class InventarioController {

    /**
     * Endpoint de Diagnóstico de Salud (Heartbeat Check).
     * Invocado periódicamente por el Microservicio Balanceador cada 2 segundos.
     * 
     * @return Respuesta JSON con estado HTTP 200 OK y métricas del nodo.
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("servicio", "Microservicio de Inventario (Integrante 4)");
        response.put("puerto", 9002);
        response.put("cpu", 18.2);
        response.put("ram", 38.6);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de Negocio para Consultar Inventario y Stock.
     * Invocado por el Balanceador en la rotación Round-Robin.
     * 
     * @return Respuesta JSON confirmando la disponibilidad de inventario.
     */
    @GetMapping({"/", "/procesar", "/consultar-stock", "/api/inventario/consultar"})
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
