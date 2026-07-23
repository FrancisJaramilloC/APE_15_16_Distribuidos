package com.distribuidos.pagos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PagosApplication {
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PagosApplication.class);
        System.setProperty("server.address", "0.0.0.0");
        System.setProperty("server.port", System.getProperty("port", "9001"));
        app.run(args);
    }
}
