package com.distribuidos.usuarios;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Clase principal de entrada para el Microservicio Backend de Autenticación y Usuarios.
 * Asignado al Integrante 5 del grupo de desarrollo.
 */
@SpringBootApplication
public class UsuariosApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(UsuariosApplication.class);
        
        // Configura el puerto TCP por defecto a 9003 si no se especifica mediante propiedad de sistema
        System.setProperty("server.port", System.getProperty("port", "9003"));
        
        // Inicia el servidor embebido Tomcat y el contexto de Spring Boot
        app.run(args);
    }
}
