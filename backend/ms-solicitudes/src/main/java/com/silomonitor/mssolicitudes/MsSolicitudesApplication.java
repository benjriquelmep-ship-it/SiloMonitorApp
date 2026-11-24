package com.silomonitor.mssolicitudes;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

// Punto de entrada del microservicio de solicitudes de camión: levanta Tomcat en el puerto 8081
// y escanea los paquetes model, repository, service y controller.
@SpringBootApplication
public class MsSolicitudesApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsSolicitudesApplication.class, args);
    }
}