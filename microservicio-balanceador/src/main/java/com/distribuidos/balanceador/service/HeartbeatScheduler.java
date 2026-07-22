package com.distribuidos.balanceador.service;

import com.distribuidos.balanceador.database.DatabaseManager;
import com.distribuidos.balanceador.model.BackendNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.List;

@Service
public class HeartbeatScheduler {

    @Autowired
    private LoadBalancerService loadBalancerService;

    @Autowired
    private DatabaseManager databaseManager;

    private final RestTemplate restTemplate = new RestTemplate();

    @Scheduled(fixedRate = 2000) // Sondeo cada 2 segundos para detección < 3s (Guía 15/16)
    public void performHeartbeat() {
        List<BackendNode> nodes = loadBalancerService.getAllNodes();

        for (BackendNode node : nodes) {
            long start = System.currentTimeMillis();
            try {
                // Timeout rápido de 1.5s
                String url = node.getUrl() + "/health";
                String response = restTemplate.getForObject(new URI(url), String.class);
                long elapsed = System.currentTimeMillis() - start;

                if (response != null && response.contains("status")) {
                    node.setStatus("healthy");
                    node.setLatency((double) elapsed);
                    databaseManager.updateNodeStatus(node.getAddress(), node.getPort(), "ACTIVO", (double) elapsed);
                } else {
                    node.setStatus("down");
                    databaseManager.updateNodeStatus(node.getAddress(), node.getPort(), "INACTIVO", 0.0);
                }
            } catch (Exception e) {
                node.setStatus("down");
                node.setLatency(0.0);
                databaseManager.updateNodeStatus(node.getAddress(), node.getPort(), "INACTIVO", 0.0);
            }
        }
    }
}
