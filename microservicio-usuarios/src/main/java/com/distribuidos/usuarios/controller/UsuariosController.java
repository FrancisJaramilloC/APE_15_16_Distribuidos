package com.distribuidos.usuarios.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Controlador REST para el Microservicio de Usuarios.
 * Expone endpoints de diagnóstico y autenticación de usuarios.
 */
@RestController
@RequestMapping
public class UsuariosController {

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
        response.put("servicio", "Microservicio de Usuarios (Integrante 5)");
        response.put("puerto", 9003);
        response.put("cpu", 12.0);
        response.put("ram", 31.4);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }

    /**
     * Endpoint de Negocio para Verificar Usuario.
     * Invocado por el Balanceador en la rotación Round-Robin.
     * 
     * @return Respuesta JSON confirmando la autenticación del usuario.
     */
    @GetMapping({"/", "/procesar", "/verificar-usuario", "/api/usuarios/verificar"})
    public ResponseEntity<Map<String, Object>> verificarUsuario() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "OK");
        response.put("mensaje", "Usuario autenticado exitosamente por Microservicio de Usuarios en puerto 9003");
        response.put("usuarioId", "USR-9003-OK");
        response.put("puerto", 9003);
        response.put("timestamp", LocalDateTime.now().toString());
        
        return ResponseEntity.ok(response);
    }
}
