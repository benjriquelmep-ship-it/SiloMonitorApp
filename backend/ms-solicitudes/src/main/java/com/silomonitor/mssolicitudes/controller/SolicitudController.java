package com.silomonitor.mssolicitudes.controller;

import com.silomonitor.mssolicitudes.dto.RevisionRequest;
import com.silomonitor.mssolicitudes.dto.SolicitudRequest;
import com.silomonitor.mssolicitudes.model.Solicitud;
import com.silomonitor.mssolicitudes.service.SolicitudService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

// CONTROLLER: expone los endpoints REST de las solicitudes de camión.
//
//  GET    /api/solicitudes                 -> bandeja completa (pendientes primero)
//  GET    /api/solicitudes?siloId=SIL-001  -> solicitudes de un silo
//  GET    /api/solicitudes/{id}            -> una solicitud
//  POST   /api/solicitudes                 -> crea una solicitud PENDIENTE   (201 Created)
//  PATCH  /api/solicitudes/{id}/revision   -> aprueba o rechaza
//  DELETE /api/solicitudes/{id}            -> elimina                         (204 No Content)
@RestController
@RequestMapping("/api/solicitudes")
public class SolicitudController {

    private final SolicitudService service;

    public SolicitudController(SolicitudService service) {
        this.service = service;
    }

    @GetMapping
    public List<Solicitud> listar(@RequestParam(required = false) String siloId) {
        return siloId == null || siloId.isBlank() ? service.listar() : service.listarPorSilo(siloId);
    }

    @GetMapping("/{id}")
    public Solicitud obtener(@PathVariable Long id) {
        return service.obtener(id);
    }

    @PostMapping
    public ResponseEntity<Solicitud> crear(@Valid @RequestBody SolicitudRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.crear(req));
    }

    @PatchMapping("/{id}/revision")
    public Solicitud revisar(@PathVariable Long id, @Valid @RequestBody RevisionRequest req) {
        return service.revisar(id, req);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        service.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}