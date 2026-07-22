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

@RestController
@RequestMapping
public class BalanceadorController {

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private DatabaseManager databaseManager;

    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping({"/balance/{ruta}", "/balance", "/balance/**"})
    public ResponseEntity<?> proxyRequest(@PathVariable(required = false) String ruta) {
        BackendNode chosen = loadBalancerService.chooseNode();
        if (chosen == null) {
            Map<String, Object> errorMap = new HashMap<>();
            errorMap.put("error", "No hay servidores backend disponibles en el cluster");
            errorMap.put("status", 503);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(errorMap);
        }

        try {
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

    @GetMapping("/api/db/nodos")
    public ResponseEntity<List<Map<String, Object>>> getDbNodes() {
        return ResponseEntity.ok(databaseManager.getAllNodes());
    }

    @GetMapping("/api/db/circuit")
    public ResponseEntity<List<Map<String, Object>>> getDbCircuitLogs() {
        return ResponseEntity.ok(databaseManager.getCircuitLogs());
    }
}
