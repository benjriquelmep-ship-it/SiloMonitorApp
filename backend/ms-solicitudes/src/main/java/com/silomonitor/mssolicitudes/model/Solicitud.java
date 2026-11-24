package com.silomonitor.mssolicitudes.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ENTIDAD JPA: cada solicitud de camión de reposición queda en la tabla "solicitudes".
// Flujo: el Supervisor la crea (PENDIENTE) y la Jefatura la aprueba o rechaza.
// siloId es solo el código del silo: este microservicio no accede a la base de ms-silos
// (cada microservicio tiene su propia base de datos).
@Entity
@Table(name = "solicitudes")
public class Solicitud {

    public static final String PENDIENTE = "PENDIENTE";
    public static final String APROBADA = "APROBADA";
    public static final String RECHAZADA = "RECHAZADA";

    // Id autoincremental generado por MySQL.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String siloId;

    @Column(nullable = false)
    private Double kgSolicitados;

    @Column(nullable = false)
    private Boolean urgente;

    private String observacion;

    @Column(nullable = false, length = 50)
    private String solicitadoPor;

    // Fechas en milisegundos (igual que System.currentTimeMillis() en la app).
    @Column(nullable = false)
    private Long fechaSolicitud;

    // PENDIENTE, APROBADA o RECHAZADA
    @Column(nullable = false, length = 10)
    private String estado;

    @Column(length = 50)
    private String revisadoPor;

    private Long fechaRevision;

    public Solicitud() {
    }

    public Solicitud(String siloId, Double kgSolicitados, Boolean urgente, String observacion,
                     String solicitadoPor, Long fechaSolicitud) {
        this.siloId = siloId;
        this.kgSolicitados = kgSolicitados;
        this.urgente = urgente;
        this.observacion = observacion;
        this.solicitadoPor = solicitadoPor;
        this.fechaSolicitud = fechaSolicitud;
        this.estado = PENDIENTE;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSiloId() { return siloId; }
    public void setSiloId(String siloId) { this.siloId = siloId; }

    public Double getKgSolicitados() { return kgSolicitados; }
    public void setKgSolicitados(Double kgSolicitados) { this.kgSolicitados = kgSolicitados; }

    public Boolean getUrgente() { return urgente; }
    public void setUrgente(Boolean urgente) { this.urgente = urgente; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }

    public String getSolicitadoPor() { return solicitadoPor; }
    public void setSolicitadoPor(String solicitadoPor) { this.solicitadoPor = solicitadoPor; }

    public Long getFechaSolicitud() { return fechaSolicitud; }
    public void setFechaSolicitud(Long fechaSolicitud) { this.fechaSolicitud = fechaSolicitud; }

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getRevisadoPor() { return revisadoPor; }
    public void setRevisadoPor(String revisadoPor) { this.revisadoPor = revisadoPor; }

    public Long getFechaRevision() { return fechaRevision; }
    public void setFechaRevision(Long fechaRevision) { this.fechaRevision = fechaRevision; }
}