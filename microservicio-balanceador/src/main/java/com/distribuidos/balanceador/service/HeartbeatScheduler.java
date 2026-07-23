package com.distribuidos.balanceador.service;

import com.distribuidos.balanceador.database.DatabaseManager;
import com.distribuidos.balanceador.model.BackendNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

/**
 * Componente Encargado del Monitoreo Proactivo de Salud (Heartbeat).
 * Ejecuta un hilo secundario periódico que sondea el estado físico de los nodos
 * y actualiza los resultados en la base de datos SQLite (nodos.db).
 */
@Service
public class HeartbeatScheduler {

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private DatabaseManager databaseManager;

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Tarea Periódica ejecutada cada 2000 ms (2 segundos).
     * Satisface el requisito de detección de fallas físicas en menos de 3 segundos (Guía 15).
     */
    @Scheduled(fixedRate = 2000)
    public void performHeartbeat() {
        List<BackendNode> nodes = loadBalancerService.getAllNodes();

        for (BackendNode node : nodes) {
            long start = System.currentTimeMillis();
            try {
                // Realiza la petición HTTP GET /health hacia el microservicio backend
                String url = node.getUrl() + "/health";
                String response = restTemplate.getForObject(new URI(url), String.class);
                long elapsed = System.currentTimeMillis() - start;

                if (response != null && response.contains("status")) {
                    // El nodo respondió correctamente: Marcar como sano (healthy) y ACTIVO
                    node.setStatus("healthy");
                    node.setLatency((double) elapsed);
                    databaseManager.updateNodeStatus(node.getAddress(), node.getPort(), "ACTIVO", (double) elapsed);
                } else {
                    // Respuesta inválida: Marcar como caído (down) e INACTIVO
                    node.setStatus("down");
                    databaseManager.updateNodeStatus(node.getAddress(), node.getPort(), "INACTIVO", 0.0);
                }
            } catch (Exception e) {
                // Excepción de conexión o timeout: El nodo se encuentra apagado o inaccesible
                node.setStatus("down");
                node.setLatency(0.0);
                databaseManager.updateNodeStatus(node.getAddress(), node.getPort(), "INACTIVO", 0.0);
            }
        }
    }
}
