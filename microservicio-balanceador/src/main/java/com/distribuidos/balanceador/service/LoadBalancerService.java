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
        // Nodos backend por defecto (1 por integrante de backend: Pagos, Inventario, Usuarios)
        nodes.add(new BackendNode("192.168.1.30", 9001)); // Microservicio Pagos (Integrante 3)
        nodes.add(new BackendNode("192.168.1.31", 9002)); // Microservicio Inventario (Integrante 4)
        nodes.add(new BackendNode("192.168.1.32", 9003)); // Microservicio Usuarios (Integrante 5)
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
