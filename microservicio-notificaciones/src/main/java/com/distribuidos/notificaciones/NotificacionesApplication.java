package com.distribuidos.notificaciones;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NotificacionesApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(NotificacionesApplication.class);
        // Puerto por defecto 9004
        System.setProperty("server.port", System.getProperty("port", "9004"));
        app.run(args);
    }
}
