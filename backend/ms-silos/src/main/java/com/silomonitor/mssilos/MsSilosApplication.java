package com.silomonitor.mssilos;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punto de entrada del microservicio: levanta Tomcat en el puerto 8080
// y escanea los paquetes model, repository, service y controller.
@SpringBootApplication
public class MsSilosApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSilosApplication.class, args);
    }
}