package com.distribuidos.balanceador.service;

import com.distribuidos.balanceador.model.BackendNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
public class LoadBalancerService {

    private final List<BackendNode> nodes = Collections.synchronizedList(new ArrayList<>());
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public LoadBalancerService() {
        // Nodos por defecto para los 4 microservicios backend según Guía 16
        nodes.add(new BackendNode("127.0.0.1", 9001)); // Pagos
        nodes.add(new BackendNode("127.0.0.1", 9002)); // Inventario
        nodes.add(new BackendNode("127.0.0.1", 9003)); // Usuarios
        nodes.add(new BackendNode("127.0.0.1", 9004)); // Notificaciones
    }

    public List<BackendNode> getAllNodes() {
        return new ArrayList<>(nodes);
    }

    public synchronized void addNode(String host, int port) {
        for (BackendNode n : nodes) {
            if (n.getHost().equals(host) && n.getPort() == port) {
                return;
            }
        }
        nodes.add(new BackendNode(host, port));
    }

    public synchronized BackendNode chooseNode() {
        List<BackendNode> healthyNodes = new ArrayList<>();
        for (BackendNode n : nodes) {
            if (n.isHealthy()) {
                healthyNodes.add(n);
            }
        }

        if (healthyNodes.isEmpty()) {
            // Si no hay totalmente sanos, retornar alguno que no esté en 'down' o null
            return null;
        }

        int index = Math.abs(currentIndex.getAndIncrement()) % healthyNodes.size();
        return healthyNodes.get(index);
    }
}
