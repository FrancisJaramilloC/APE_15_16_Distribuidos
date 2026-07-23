package com.distribuidos.balanceador.model;

/**
 * Modelo de Datos que representa un Nodo Backend dentro del Cluster.
 * Mantiene la dirección IP, puerto, estado de salud ("healthy" / "down") y latencia.
 */
public class BackendNode {

    private String host;
    private int port;
    private String status;  // Estado físico: "healthy" o "down"
    private double latency; // Latencia medida en milisegundos

    public BackendNode(String host, int port) {
        this.host = host;
        this.port = port;
        this.status = "unknown";
        this.latency = 0.0;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    public String getUrl() {
        return "http://" + host + ":" + port;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getLatency() {
        return latency;
    }

    public void setLatency(double latency) {
        this.latency = latency;
    }

    /**
     * Verifica si el nodo está completamente sano y activo para recibir peticiones.
     */
    public boolean isHealthy() {
        return "healthy".equalsIgnoreCase(status);
    }
}
