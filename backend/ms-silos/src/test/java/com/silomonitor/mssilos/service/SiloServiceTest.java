package com.silomonitor.mssilos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.silomonitor.mssilos.dto.MovimientoRequest;
import com.silomonitor.mssilos.dto.SiloRequest;
import com.silomonitor.mssilos.exception.RecursoNoEncontradoException;
import com.silomonitor.mssilos.exception.ReglaNegocioException;
import com.silomonitor.mssilos.model.Movimiento;
import com.silomonitor.mssilos.model.Silo;
import com.silomonitor.mssilos.repository.MovimientoRepository;
import com.silomonitor.mssilos.repository.SiloRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

// Pruebas del servicio SIN base de datos: los repositorios se reemplazan por mocks de Mockito.
@ExtendWith(MockitoExtension.class)
class SiloServiceTest {

    @Mock
    SiloRepository siloRepository;

    @Mock
    MovimientoRepository movimientoRepository;

    SiloService service;

    Silo silo;

    @BeforeEach
    void setUp() {
        service = new SiloService(siloRepository, movimientoRepository);
        silo = new Silo("SIL-001", "Silo Norte A", "Granja", "G1", "Engorda", 1000.0, 400.0, 100.0, -33.0, -71.0);
    }

    @Test
    void cargaValidaSumaAlStock() {
        when(siloRepository.findById("SIL-001")).thenReturn(Optional.of(silo));

        Silo resultado = service.registrarMovimiento("SIL-001", new MovimientoRequest("CARGA", 300.0, "", null));

        assertEquals(700.0, resultado.getNivelActual());
        verify(movimientoRepository).save(any(Movimiento.class));
    }

    @Test
    void cargaQueSobrellenaEsRechazada() {
        when(siloRepository.findById("SIL-001")).thenReturn(Optional.of(silo));

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarMovimiento("SIL-001", new MovimientoRequest("CARGA", 700.0, "", null)));
        verify(movimientoRepository, never()).save(any());
    }

    @Test
    void consumoMayorAlStockEsRechazado() {
        when(siloRepository.findById("SIL-001")).thenReturn(Optional.of(silo));

        assertThrows(ReglaNegocioException.class,
                () -> service.registrarMovimiento("SIL-001", new MovimientoRequest("CONSUMO", 500.0, "", null)));
    }

    @Test
    void crearConCodigoRepetidoEsRechazado() {
        when(siloRepository.existsById("SIL-001")).thenReturn(true);
        SiloRequest req = new SiloRequest("sil-001", "Otro", "Granja", "", "", 1000.0, 0.0, 10.0, -33.0, -71.0);

        assertThrows(ReglaNegocioException.class, () -> service.crear(req));
    }

    @Test
    void obtenerSiloInexistenteLanza404() {
        when(siloRepository.findById("SIL-999")).thenReturn(Optional.empty());

        assertThrows(RecursoNoEncontradoException.class, () -> service.obtener("SIL-999"));
    }
}