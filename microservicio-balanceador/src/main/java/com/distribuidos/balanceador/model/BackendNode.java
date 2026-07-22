package com.distribuidos.balanceador.model;

public class BackendNode {
    private String host;
    private int port;
    private String status; // "healthy", "down"
    private double latency;

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

    public boolean isHealthy() {
        return "healthy".equalsIgnoreCase(status);
    }
}
