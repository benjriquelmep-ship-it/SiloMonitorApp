package com.silomonitor.mssilos.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

// ENTIDAD JPA: cada objeto Silo es una fila de la tabla "silos" en MySQL.
// Tiene los mismos campos que SiloEntity de la app (Room) para que el JSON calce directo.
@Entity
@Table(name = "silos")
public class Silo {

    // Código del silo, ej: "SIL-001". No es autoincremental: lo define el usuario (y el QR).
    @Id
    @Column(length = 20)
    private String id;

    @Column(nullable = false)
    private String nombre;

    @Column(nullable = false)
    private String granja;

    private String galpon;

    private String tipoAlimento;

    @Column(nullable = false)
    private Double capacidadMaxima;

    // Stock actual en kg. Solo cambia con movimientos (cargas/consumos).
    @Column(nullable = false)
    private Double nivelActual;

    private Double consumoPromedioDiario;

    private Double latitud;

    private Double longitud;

    // JPA necesita un constructor vacío.
    public Silo() {
    }

    public Silo(String id, String nombre, String granja, String galpon, String tipoAlimento,
                Double capacidadMaxima, Double nivelActual, Double consumoPromedioDiario,
                Double latitud, Double longitud) {
        this.id = id;
        this.nombre = nombre;
        this.granja = granja;
        this.galpon = galpon;
        this.tipoAlimento = tipoAlimento;
        this.capacidadMaxima = capacidadMaxima;
        this.nivelActual = nivelActual;
        this.consumoPromedioDiario = consumoPromedioDiario;
        this.latitud = latitud;
        this.longitud = longitud;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNombre() { return nombre; }
    public void setNombre(String nombre) { this.nombre = nombre; }

    public String getGranja() { return granja; }
    public void setGranja(String granja) { this.granja = granja; }

    public String getGalpon() { return galpon; }
    public void setGalpon(String galpon) { this.galpon = galpon; }

    public String getTipoAlimento() { return tipoAlimento; }
    public void setTipoAlimento(String tipoAlimento) { this.tipoAlimento = tipoAlimento; }

    public Double getCapacidadMaxima() { return capacidadMaxima; }
    public void setCapacidadMaxima(Double capacidadMaxima) { this.capacidadMaxima = capacidadMaxima; }

    public Double getNivelActual() { return nivelActual; }
    public void setNivelActual(Double nivelActual) { this.nivelActual = nivelActual; }

    public Double getConsumoPromedioDiario() { return consumoPromedioDiario; }
    public void setConsumoPromedioDiario(Double consumoPromedioDiario) { this.consumoPromedioDiario = consumoPromedioDiario; }

    public Double getLatitud() { return latitud; }
    public void setLatitud(Double latitud) { this.latitud = latitud; }

    public Double getLongitud() { return longitud; }
    public void setLongitud(Double longitud) { this.longitud = longitud; }
}