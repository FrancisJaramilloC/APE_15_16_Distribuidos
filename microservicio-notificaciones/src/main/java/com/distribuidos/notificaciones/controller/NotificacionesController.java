package com.distribuidos.notificaciones.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping
public class NotificacionesController {

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("servicio", "Microservicio de Notificaciones (Integrante 4)");
        response.put("puerto", 9004);
        response.put("cpu", 10.5);
        response.put("ram", 28.9);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }

    @GetMapping({"/", "/enviar-notificacion", "/api/notificaciones/enviar"})
    public ResponseEntity<Map<String, Object>> enviarNotificacion() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("mensaje", "Notificación enviada exitosamente por Microservicio de Notificaciones en puerto 9004");
        response.put("notificacionId", "NTF-9004-SENT");
        response.put("puerto", 9004);
        response.put("timestamp", LocalDateTime.now().toString());
        return ResponseEntity.ok(response);
    }
}
