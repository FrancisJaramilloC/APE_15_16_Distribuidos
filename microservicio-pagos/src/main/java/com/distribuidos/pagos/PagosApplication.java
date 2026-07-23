package com.distribuidos.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de entrada para el Microservicio Backend de Procesamiento de Pagos.
 * Asignado al Integrante 3 del grupo de desarrollo.
 */
@SpringBootApplication
public class PagosApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PagosApplication.class);
        
        // Configura el puerto TCP por defecto a 9001 si no se especifica mediante propiedad de sistema
        System.setProperty("server.port", System.getProperty("port", "9001"));
        
        // Inicia el servidor embebido Tomcat y el contexto de Spring Boot
        app.run(args);
    }
}
