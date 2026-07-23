package com.distribuidos.orquestador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal del Microservicio Orquestador / API Gateway.
 * Asignado al Integrante 1 del grupo de desarrollo.
 * 
 * Intercepta todas las peticiones del cliente y las protege
 * integrando el patrón de tolerancia a fallos Circuit Breaker.
 */
@SpringBootApplication
public class OrquestadorApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(OrquestadorApplication.class);
        
        // Puerto por defecto 8080 para la puerta de entrada principal del sistema
        System.setProperty("server.port", System.getProperty("port", "8080"));
        
        app.run(args);
    }
}
