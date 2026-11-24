package com.silomonitor.mssolicitudes.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.silomonitor.mssolicitudes.dto.RevisionRequest;
import com.silomonitor.mssolicitudes.dto.SolicitudRequest;
import com.silomonitor.mssolicitudes.exception.RecursoNoEncontradoException;
import com.silomonitor.mssolicitudes.exception.ReglaNegocioException;
import com.silomonitor.mssolicitudes.model.Solicitud;
import com.silomonitor.mssolicitudes.repository.SolicitudRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Pruebas del servicio SIN base de datos: el repositorio se reemplaza por un mock de Mockito.
@ExtendWith(MockitoExtension.class)
class SolicitudServiceTest {

    @Mock
    SolicitudRepository repository;

    SolicitudService service;

    Solicitud pendiente;

    @BeforeEach
    void setUp() {
        service = new SolicitudService(repository);
        pendiente = new Solicitud("SIL-001", 5000.0, true, "Nivel bajo", "supervisor", 1000L);
        pendiente.setId(1L);
    }

    @Test
    void crearGuardaSolicitudPendienteConCodigoEnMayusculas() {
        when(repository.existsBySiloIdAndEstado("SIL-001", Solicitud.PENDIENTE)).thenReturn(false);
        when(repository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud creada = service.crear(new SolicitudRequest(" sil-001 ", 5000.0, true, " Nivel bajo ", "supervisor", 1234L));

        assertEquals("SIL-001", creada.getSiloId());
        assertEquals(Solicitud.PENDIENTE, creada.getEstado());
        assertEquals("Nivel bajo", creada.getObservacion());
        assertEquals(1234L, creada.getFechaSolicitud());
        assertTrue(creada.getUrgente());
    }

    @Test
    void crearSinFechaNiUrgenteUsaValoresPorDefecto() {
        when(repository.existsBySiloIdAndEstado("SIL-002", Solicitud.PENDIENTE)).thenReturn(false);
        when(repository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud creada = service.crear(new SolicitudRequest("SIL-002", 1000.0, null, null, "admin", null));

        assertEquals(false, creada.getUrgente());
        assertEquals("", creada.getObservacion());
        assertTrue(creada.getFechaSolicitud() > 0);
    }

    @Test
    void crearConCamionYaPendienteLanzaReglaNegocio() {
        when(repository.existsBySiloIdAndEstado("SIL-001", Solicitud.PENDIENTE)).thenReturn(true);

        assertThrows(ReglaNegocioException.class,
                () -> service.crear(new SolicitudRequest("SIL-001", 5000.0, false, "", "supervisor", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void aprobarCambiaEstadoYRegistraQuienReviso() {
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(repository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud revisada = service.revisar(1L, new RevisionRequest(true, "jefatura", 2000L));

        assertEquals(Solicitud.APROBADA, revisada.getEstado());
        assertEquals("jefatura", revisada.getRevisadoPor());
        assertEquals(2000L, revisada.getFechaRevision());
    }

    @Test
    void rechazarCambiaEstadoARechazada() {
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));
        when(repository.save(any(Solicitud.class))).thenAnswer(inv -> inv.getArgument(0));

        Solicitud revisada = service.revisar(1L, new RevisionRequest(false, "jefatura", null));

        assertEquals(Solicitud.RECHAZADA, revisada.getEstado());
        assertTrue(revisada.getFechaRevision() > 0);
    }

    @Test
    void revisarSolicitudYaRevisadaLanzaReglaNegocio() {
        pendiente.setEstado(Solicitud.APROBADA);
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));

        assertThrows(ReglaNegocioException.class,
                () -> service.revisar(1L, new RevisionRequest(false, "jefatura", null)));
        verify(repository, never()).save(any());
    }

    @Test
    void obtenerInexistenteLanzaNoEncontrado() {
        when(repository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener(99L));
    }

    @Test
    void eliminarBorraLaSolicitud() {
        when(repository.findById(1L)).thenReturn(Optional.of(pendiente));

        service.eliminar(1L);

        verify(repository).delete(pendiente);
    }

    @Test
    void listarPorSiloNormalizaElCodigo() {
        when(repository.findBySiloIdOrderByFechaSolicitudDesc("SIL-001")).thenReturn(List.of(pendiente));

        assertEquals(1, service.listarPorSilo(" sil-001").size());
    }

    @Test
    void listarDevuelveLaBandejaOrdenada() {
        when(repository.listarParaBandeja()).thenReturn(List.of(pendiente));

        assertEquals(List.of(pendiente), service.listar());
    }
}