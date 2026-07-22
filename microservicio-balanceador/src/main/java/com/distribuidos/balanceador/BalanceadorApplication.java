package com.distribuidos.balanceador;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
public class BalanceadorApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(BalanceadorApplication.class);
        // Puerto por defecto 8000 (o 5000)
        System.setProperty("server.port", System.getProperty("port", "8000"));
        app.run(args);
    }
}
