package com.silomonitor.mssilos.repository;

import com.silomonitor.mssilos.model.Movimiento;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoRepository extends JpaRepository<Movimiento, Long> {

    // Consulta JPQL (sobre las clases, no las tablas): historial de un silo, del más reciente al más antiguo.
    // Equivale en SQL a: SELECT * FROM movimientos WHERE silo_id = ? ORDER BY fecha DESC
    @Query("SELECT m FROM Movimiento m WHERE m.silo.id = :siloId ORDER BY m.fecha DESC")
    List<Movimiento> buscarPorSilo(@Param("siloId") String siloId);
}