package com.distribuidos.orquestador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class OrquestadorApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OrquestadorApplication.class);
        System.setProperty("server.address", "0.0.0.0");
        System.setProperty("server.port", System.getProperty("port", "8080"));
        app.run(args);
    }
}
