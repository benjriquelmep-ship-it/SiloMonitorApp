package com.silomonitor.mssilos.controller;

import com.silomonitor.mssilos.dto.MovimientoRequest;
import com.silomonitor.mssilos.dto.SiloRequest;
import com.silomonitor.mssilos.model.Movimiento;
import com.silomonitor.mssilos.model.Silo;
import com.silomonitor.mssilos.service.SiloService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// CONTROLLER: expone los endpoints REST. Recibe JSON, llama al servicio y responde JSON.
//
//  GET    /api/silos                      -> lista todos los silos
//  GET    /api/silos/{id}                 -> un silo
//  POST   /api/silos                      -> crea un silo                 (201 Created)
//  PUT    /api/silos/{id}                 -> edita un silo
//  DELETE /api/silos/{id}                 -> elimina un silo               (204 No Content)
//  GET    /api/silos/{id}/movimientos     -> historial de cargas/consumos
//  POST   /api/silos/{id}/movimientos     -> registra carga/consumo y devuelve el silo actualizado
@RestController
@RequestMapping("/api/silos")
public class SiloController {

    private final SiloService service;

    public SiloController(SiloService service) {
        this.service = service;
    }

    @GetMapping
    public List<Silo> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public Silo obtener(@PathVariable String id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Silo> crear(@Valid @RequestBody SiloRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PutMapping("/{id}")
    public Silo actualizar(@PathVariable String id, @Valid @RequestBody SiloRequest req) {
        return service.actualizar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/movimientos")
    public List<Movimiento> movimientos(@PathVariable String id) {
        return service.movimientos(id);
    }

    @PostMapping("/{id}/movimientos")
    public ResponseEntity<Silo> registrarMovimiento(@PathVariable String id, @Valid @RequestBody MovimientoRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.registrarMovimiento(id, req));
    }
}