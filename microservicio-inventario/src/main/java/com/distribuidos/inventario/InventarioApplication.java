package com.distribuidos.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InventarioApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InventarioApplication.class);
        System.setProperty("server.address", "0.0.0.0");
        System.setProperty("server.port", System.getProperty("port", "9005"));
        app.run(args);
    }
}
