package com.silomonitor.mssolicitudes.config;

import com.silomonitor.mssolicitudes.model.Solicitud;
import com.silomonitor.mssolicitudes.repository.SolicitudRepository;
import java.util.List;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

// Se ejecuta una vez al iniciar el microservicio: si la tabla está vacía,
// inserta dos solicitudes de ejemplo para que la bandeja no aparezca vacía en la demo.
@Component
public class DataLoader implements CommandLineRunner {

    private static final long UN_DIA_MS = 24L * 60 * 60 * 1000;

    private final SolicitudRepository repository;

    public DataLoader(SolicitudRepository repository) {
        this.repository = repository;
    }

    @Override
    public void run(String... args) {
        if (repository.count() > 0) {
            return;
        }
        long ahora = System.currentTimeMillis();

        // Pendiente y urgente: SIL-003 está en nivel crítico en los datos de ejemplo.
        Solicitud urgente = new Solicitud("SIL-003", 18000.0, true, "Nivel crítico en galpón 3", "supervisor", ahora - UN_DIA_MS / 2);

        // Ya aprobada por la Jefatura.
        Solicitud aprobada = new Solicitud("SIL-005", 15000.0, false, "Reposición programada", "supervisor", ahora - 2 * UN_DIA_MS);
        aprobada.setEstado(Solicitud.APROBADA);
        aprobada.setRevisadoPor("jefatura");
        aprobada.setFechaRevision(ahora - UN_DIA_MS);

        repository.saveAll(List.of(urgente, aprobada));
    }
}