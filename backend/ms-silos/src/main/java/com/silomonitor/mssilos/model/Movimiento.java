package com.silomonitor.mssilos.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

// ENTIDAD JPA: cada carga o consumo de alimento queda registrado en la tabla "movimientos".
// Relación: muchos movimientos pertenecen a un silo (@ManyToOne).
@Entity
@Table(name = "movimientos")
public class Movimiento {

    // Id autoincremental generado por MySQL.
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Clave foránea silo_id. Si se borra el silo, MySQL borra sus movimientos (ON DELETE CASCADE).
    // @JsonIgnore: no se manda el silo completo dentro de cada movimiento (se usa siloId).
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "silo_id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JsonIgnore
    private Silo silo;

    // "CARGA" o "CONSUMO"
    @Column(nullable = false, length = 10)
    private String tipo;

    @Column(nullable = false)
    private Double cantidadKg;

    // Fecha en milisegundos (igual que System.currentTimeMillis() en la app).
    @Column(nullable = false)
    private Long fecha;

    private String observacion;

    public Movimiento() {
    }

    public Movimiento(Silo silo, String tipo, Double cantidadKg, Long fecha, String observacion) {
        this.silo = silo;
        this.tipo = tipo;
        this.cantidadKg = cantidadKg;
        this.fecha = fecha;
        this.observacion = observacion;
    }

    // Se expone en el JSON como "siloId" en vez del objeto Silo completo.
    public String getSiloId() {
        return silo != null ? silo.getId() : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public Silo getSilo() { return silo; }
    public void setSilo(Silo silo) { this.silo = silo; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public Double getCantidadKg() { return cantidadKg; }
    public void setCantidadKg(Double cantidadKg) { this.cantidadKg = cantidadKg; }

    public Long getFecha() { return fecha; }
    public void setFecha(Long fecha) { this.fecha = fecha; }

    public String getObservacion() { return observacion; }
    public void setObservacion(String observacion) { this.observacion = observacion; }
}