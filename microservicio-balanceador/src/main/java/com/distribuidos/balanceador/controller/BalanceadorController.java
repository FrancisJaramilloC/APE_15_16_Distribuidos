package com.distribuidos.balanceador.controller;

import com.distribuidos.balanceador.database.DatabaseManager;
import com.distribuidos.balanceador.model.BackendNode;
import com.distribuidos.balanceador.service.LoadBalancerService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Controlador Proxy para el Balanceador de Carga.
 * Recibe las solicitudes del Orquestador y las reenvía a los nodos backend activos.
 */
@RestController
@RequestMapping
public class BalanceadorController {

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private DatabaseManager databaseManager;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Endpoint Proxy de Balanceo (/balance/**).
     * Selecciona un nodo activo del pool (vía Round-Robin) y le delega la ejecución HTTP.
     * 
     * @param ruta Ruta opcional enviada por el cliente.
     * @return La respuesta HTTP entregada por el nodo backend elegido.
     */
    @GetMapping({"/balance/{ruta}", "/balance", "/balance/**"})
    public ResponseEntity<?> proxyRequest(@PathVariable(required = false) String ruta) {
        // Seleccionar un nodo backend sano comprobado por Heartbeat
        BackendNode chosen = loadBalancerService.chooseNode();
        
        if (chosen == null) {
            // Si todos los nodos backend están apagados
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "No hay servidores backend disponibles en el cluster");
            errorMap.put("status", 503);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorMap);
        }

        try {
            // Reenviar la petición al nodo backend seleccionado
            String path = (ruta != null) ? ruta : "";
            String targetUrl = chosen.getUrl() + "/" + path;
            ResponseEntity<String> response = restTemplate.getForEntity(targetUrl, String.class);
            
            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "Error al comunicarse con backend " + chosen.getAddress());
            errorMap.put("detalle", e.getMessage());
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(errorMap);
        }
    }

    /**
     * Endpoint REST para consultar la tabla estado_nodos de SQLite desde la web.
     */
    @GetMapping("/api/db/nodos")
    public ResponseEntity<List<Map<String, Object>>> getDbNodes() {
        return ResponseEntity.ok(databaseManager.getAllNodes());
    }

    /**
     * Endpoint REST para consultar los registros de auditoría circuit_log de SQLite.
     */
    @GetMapping("/api/db/circuit")
    public ResponseEntity<List<Map<String, Object>>> getDbCircuitLogs() {
        return ResponseEntity.ok(databaseManager.getCircuitLogs());
    }
}
