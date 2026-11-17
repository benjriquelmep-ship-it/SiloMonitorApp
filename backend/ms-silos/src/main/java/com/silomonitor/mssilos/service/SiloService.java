package com.silomonitor.mssilos.service;

import com.silomonitor.mssilos.dto.MovimientoRequest;
import com.silomonitor.mssilos.dto.SiloRequest;
import com.silomonitor.mssilos.exception.RecursoNoEncontradoException;
import com.silomonitor.mssilos.exception.ReglaNegocioException;
import com.silomonitor.mssilos.model.Movimiento;
import com.silomonitor.mssilos.model.Silo;
import com.silomonitor.mssilos.repository.MovimientoRepository;
import com.silomonitor.mssilos.repository.SiloRepository;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// SERVICE: aquí vive la lógica de negocio. El controller solo recibe la petición HTTP
// y delega; el repository solo guarda/lee. Así cada capa tiene una sola responsabilidad.
@Service
public class SiloService {

    private final SiloRepository siloRepository;
    private final MovimientoRepository movimientoRepository;

    // Inyección de dependencias por constructor: Spring entrega los repositorios.
    public SiloService(SiloRepository siloRepository, MovimientoRepository movimientoRepository) {
        this.siloRepository = siloRepository;
        this.movimientoRepository = movimientoRepository;
    }

    // Todos los silos, del más vacío al más lleno (igual que la lista de la app).
    public List<Silo> listar() {
        return siloRepository.findAll().stream()
                .sorted((a, b) -> Double.compare(
                        a.getNivelActual() / a.getCapacidadMaxima(),
                        b.getNivelActual() / b.getCapacidadMaxima()))
                .toList();
    }

    public Silo obtener(String id) {
        return siloRepository.findById(id.toUpperCase())
                .orElseThrow(() -> new RecursoNoEncontradoException("Silo no encontrado: " + id));
    }

    // CREATE: el código no se puede repetir y el stock no puede superar la capacidad.
    public Silo crear(SiloRequest req) {
        String codigo = req.id().trim().toUpperCase();
        if (siloRepository.existsById(codigo)) {
            throw new ReglaNegocioException("Ya existe un silo con el código " + codigo + ".");
        }
        if (req.nivelActual() > req.capacidadMaxima()) {
            throw new ReglaNegocioException("El stock no puede superar la capacidad máxima.");
        }
        Silo silo = new Silo(codigo, req.nombre().trim(), req.granja().trim(), req.galpon(), req.tipoAlimento(),
                req.capacidadMaxima(), req.nivelActual(), req.consumoPromedioDiario(), req.latitud(), req.longitud());
        return siloRepository.save(silo);
    }

    // UPDATE: edita datos maestros. El stock NO se toca aquí: solo cambia con movimientos.
    public Silo actualizar(String id, SiloRequest req) {
        Silo silo = obtener(id);
        if (silo.getNivelActual() > req.capacidadMaxima()) {
            throw new ReglaNegocioException("La capacidad no puede ser menor al stock actual ("
                    + silo.getNivelActual().intValue() + " kg).");
        }
        silo.setNombre(req.nombre().trim());
        silo.setGranja(req.granja().trim());
        silo.setGalpon(req.galpon());
        silo.setTipoAlimento(req.tipoAlimento());
        silo.setCapacidadMaxima(req.capacidadMaxima());
        silo.setConsumoPromedioDiario(req.consumoPromedioDiario());
        silo.setLatitud(req.latitud());
        silo.setLongitud(req.longitud());
        return siloRepository.save(silo);
    }

    // DELETE: al borrar el silo, MySQL borra sus movimientos (ON DELETE CASCADE).
    public void eliminar(String id) {
        Silo silo = obtener(id);
        siloRepository.delete(silo);
    }

    public List<Movimiento> movimientos(String siloId) {
        obtener(siloId); // 404 si el silo no existe
        return movimientoRepository.buscarPorSilo(siloId.toUpperCase());
    }

    // Registra una carga o un consumo aplicando las reglas de terreno.
    // @Transactional: el nuevo stock y el movimiento se guardan juntos (o ninguno si falla).
    @Transactional
    public Silo registrarMovimiento(String siloId, MovimientoRequest req) {
        Silo silo = obtener(siloId);
        boolean esCarga = "CARGA".equals(req.tipo());
        double nuevoNivel = calcularNuevoNivel(silo.getNivelActual(), silo.getCapacidadMaxima(), req.cantidadKg(), esCarga);

        silo.setNivelActual(nuevoNivel);
        siloRepository.save(silo);

        long fecha = req.fecha() != null ? req.fecha() : System.currentTimeMillis();
        String observacion = req.observacion() != null ? req.observacion() : "";
        movimientoRepository.save(new Movimiento(silo, req.tipo(), req.cantidadKg(), fecha, observacion));
        return silo;
    }

    // Mismas reglas que ReglasTerreno.kt de la app: no sobrellenar y no dejar saldo negativo.
    static double calcularNuevoNivel(double nivelActual, double capacidad, double kg, boolean esCarga) {
        if (kg <= 0) {
            throw new ReglaNegocioException("La cantidad debe ser mayor a 0 kg.");
        }
        if (esCarga) {
            if (nivelActual + kg > capacidad) {
                throw new ReglaNegocioException("Sobrellenado: la carga supera la capacidad máxima ("
                        + (int) capacidad + " kg). Espacio disponible: " + (int) (capacidad - nivelActual) + " kg.");
            }
            return nivelActual + kg;
        }
        if (kg > nivelActual) {
            throw new ReglaNegocioException("Saldo negativo: el consumo supera el stock actual ("
                    + (int) nivelActual + " kg).");
        }
        return nivelActual - kg;
    }
}