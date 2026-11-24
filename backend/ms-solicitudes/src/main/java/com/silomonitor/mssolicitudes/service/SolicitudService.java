package com.silomonitor.mssolicitudes.service;

import com.silomonitor.mssolicitudes.dto.RevisionRequest;
import com.silomonitor.mssolicitudes.dto.SolicitudRequest;
import com.silomonitor.mssolicitudes.exception.RecursoNoEncontradoException;
import com.silomonitor.mssolicitudes.exception.ReglaNegocioException;
import com.silomonitor.mssolicitudes.model.Solicitud;
import com.silomonitor.mssolicitudes.repository.SolicitudRepository;
import java.util.List;
import org.springframework.stereotype.Service;

// SERVICE: lógica de negocio de la reposición de alimento.
// El controller solo recibe la petición HTTP y delega; el repository solo guarda/lee.
@Service
public class SolicitudService {

    private final SolicitudRepository repository;

    // Inyección de dependencias por constructor: Spring entrega el repositorio.
    public SolicitudService(SolicitudRepository repository) {
        this.repository = repository;
    }

    public List<Solicitud> listar() {
        return repository.listarParaBandeja();
    }

    public List<Solicitud> listarPorSilo(String siloId) {
        return repository.findBySiloIdOrderByFechaSolicitudDesc(siloId.trim().toUpperCase());
    }

    public Solicitud obtener(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RecursoNoEncontradoException("Solicitud no encontrada: " + id));
    }

    // CREATE: regla de negocio -> no puede haber dos camiones pendientes para el mismo silo.
    // (Que los kilos quepan en el silo lo valida la app con los datos de ms-silos.)
    public Solicitud crear(SolicitudRequest req) {
        String siloId = req.siloId().trim().toUpperCase();
        if (repository.existsBySiloIdAndEstado(siloId, Solicitud.PENDIENTE)) {
            throw new ReglaNegocioException("Ya hay un camión pendiente de aprobación para este silo.");
        }
        long fecha = req.fechaSolicitud() != null ? req.fechaSolicitud() : System.currentTimeMillis();
        Solicitud solicitud = new Solicitud(
                siloId,
                req.kgSolicitados(),
                Boolean.TRUE.equals(req.urgente()),
                req.observacion() != null ? req.observacion().trim() : "",
                req.solicitadoPor().trim(),
                fecha
        );
        return repository.save(solicitud);
    }

    // UPDATE: la Jefatura aprueba o rechaza. Solo se pueden revisar solicitudes PENDIENTES.
    public Solicitud revisar(Long id, RevisionRequest req) {
        Solicitud solicitud = obtener(id);
        if (!Solicitud.PENDIENTE.equals(solicitud.getEstado())) {
            throw new ReglaNegocioException("La solicitud ya fue revisada (" + solicitud.getEstado() + ").");
        }
        solicitud.setEstado(Boolean.TRUE.equals(req.aprobada()) ? Solicitud.APROBADA : Solicitud.RECHAZADA);
        solicitud.setRevisadoPor(req.revisadoPor().trim());
        solicitud.setFechaRevision(req.fechaRevision() != null ? req.fechaRevision() : System.currentTimeMillis());
        return repository.save(solicitud);
    }

    // DELETE: elimina una solicitud (por ejemplo, creada por error).
    public void eliminar(Long id) {
        Solicitud solicitud = obtener(id);
        repository.delete(solicitud);
    }
}