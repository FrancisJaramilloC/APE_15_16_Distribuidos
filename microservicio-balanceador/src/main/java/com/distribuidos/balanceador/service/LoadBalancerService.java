package com.distribuidos.balanceador.service;

import com.distribuidos.balanceador.model.BackendNode;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Servicio encargado del Algoritmo de Balanceo de Carga (Round-Robin).
 * Mantiene la lista de nodos backend registrados y selecciona los nodos sanos.
 */
@Service
public class LoadBalancerService {

    // Lista de nodos thread-safe protegida para concurrencia
    private final List<BackendNode> nodes = Collections.synchronizedList(new ArrayList<>());
    
    // Contador atómico para implementar el turno rotativo de Round-Robin
    private final AtomicInteger currentIndex = new AtomicInteger(0);

    public LoadBalancerService() {
        // Soporta configuración dinámica vía variable de entorno BACKEND_NODES o propiedad (-Dbackend.nodes)
        String envNodes = System.getenv("BACKEND_NODES");
        if (envNodes == null || envNodes.trim().isEmpty()) {
            envNodes = System.getProperty("backend.nodes", "127.0.0.1:9001,127.0.0.1:9002,127.0.0.1:9003");
        }

        String[] parts = envNodes.split(",");
        for (String part : parts) {
            String p = part.trim();
            if (!p.isEmpty() && p.contains(":")) {
                String[] hp = p.split(":");
                try {
                    addNode(hp[0].trim(), Integer.parseInt(hp[1].trim()));
                } catch (Exception ignored) {}
            }
        }
    }

    /**
     * Retorna una copia de la lista de todos los nodos del cluster.
     */
    public List<BackendNode> getAllNodes() {
        return new ArrayList<>(nodes);
    }

    /**
     * Permite agregar dinámicamente un nuevo nodo al pool si no existía previamente.
     */
    public synchronized void addNode(String host, int port) {
        for (BackendNode n : nodes) {
            if (n.getHost().equals(host) && n.getPort() == port) {
                return;
            }
        }
        nodes.add(new BackendNode(host, port));
    }

    /**
     * Selecciona el siguiente nodo backend disponible aplicando el algoritmo Round-Robin.
     * Filtra únicamente los nodos cuyo estado físico sea "healthy" (sano).
     * 
     * @return El nodo BackendNode disponible, o null si no hay nodos activos.
     */
    public synchronized BackendNode chooseNode() {
        List<BackendNode> healthyNodes = new ArrayList<>();
        
        // Filtrar nodos activos comprobados por Heartbeat
        for (BackendNode n : nodes) {
            if (n.isHealthy()) {
                healthyNodes.add(n);
            }
        }

        if (healthyNodes.isEmpty()) {
            return null; // No hay nodos disponibles en el cluster
        }

        // Selección Round-Robin usando módulo sobre el tamaño de los nodos sanos
        int index = Math.abs(currentIndex.getAndIncrement()) % healthyNodes.size();
        return healthyNodes.get(index);
    }
}
