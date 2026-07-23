package com.distribuidos.balanceador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Clase principal del Microservicio Balanceador de Carga.
 * Asignado al Integrante 2 del grupo de desarrollo.
 * 
 * La anotación @EnableScheduling habilita la ejecución en segundo plano
 * del hilo periódico de Heartbeat (monitoreo de salud).
 */
@SpringBootApplication
@EnableScheduling
public class BalanceadorApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BalanceadorApplication.class);
        
        // Puerto por defecto 8000 para el servidor Proxy Balanceador
        System.setProperty("server.port", System.getProperty("port", "8000"));
        
        app.run(args);
    }
}
