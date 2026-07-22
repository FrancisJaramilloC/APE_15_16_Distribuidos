package com.distribuidos.orquestador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrquestadorApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OrquestadorApplication.class);
        // Puerto por defecto 8080 (o 5001)
        System.setProperty("server.port", System.getProperty("port", "8080"));
        app.run(args);
    }
}
