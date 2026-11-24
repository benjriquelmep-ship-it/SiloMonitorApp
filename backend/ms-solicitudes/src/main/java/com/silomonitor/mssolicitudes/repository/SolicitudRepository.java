package com.silomonitor.mssolicitudes.repository;

import com.silomonitor.mssolicitudes.model.Solicitud;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

// REPOSITORY: Spring Data JPA genera el SQL a partir del nombre de cada método.
public interface SolicitudRepository extends JpaRepository<Solicitud, Long> {

    // Bandeja de reposición: primero las PENDIENTES (urgentes arriba) y luego las revisadas.
    // Consulta JPQL (sobre la clase Solicitud, no sobre la tabla).
    @Query("SELECT s FROM Solicitud s ORDER BY CASE s.estado WHEN 'PENDIENTE' THEN 0 ELSE 1 END, "
            + "s.urgente DESC, s.fechaSolicitud DESC")
    List<Solicitud> listarParaBandeja();

    // Solicitudes de un silo, de la más reciente a la más antigua.
    List<Solicitud> findBySiloIdOrderByFechaSolicitudDesc(String siloId);

    // ¿El silo ya tiene un camión pendiente? (regla: un solo camión pendiente por silo)
    boolean existsBySiloIdAndEstado(String siloId, String estado);
}