package com.distribuidos.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class UsuariosApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(UsuariosApplication.class);
        System.setProperty("server.address", "0.0.0.0");
        System.setProperty("server.port", System.getProperty("port", "9003"));
        app.run(args);
    }
}
