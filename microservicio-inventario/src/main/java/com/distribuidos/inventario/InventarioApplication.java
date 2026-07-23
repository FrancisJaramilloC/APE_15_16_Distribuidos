package com.distribuidos.inventario;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de entrada para el Microservicio Backend de Gestión de Inventario.
 * Asignado al Integrante 4 del grupo de desarrollo.
 */
@SpringBootApplication
public class InventarioApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(InventarioApplication.class);
        
        // Configura el puerto TCP por defecto a 9002 si no se especifica mediante propiedad de sistema
        System.setProperty("server.port", System.getProperty("port", "9002"));
        
        // Inicia el servidor embebido Tomcat y el contexto de Spring Boot
        app.run(args);
    }
}
