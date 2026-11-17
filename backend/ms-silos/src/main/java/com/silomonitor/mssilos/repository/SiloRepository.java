package com.silomonitor.mssilos.repository;

import com.silomonitor.mssilos.model.Silo;
import org.springframework.data.jpa.repository.JpaRepository;

// REPOSITORY: Spring Data JPA genera automáticamente findAll, findById, save, deleteById, existsById...
// <Silo, String> = entidad que maneja y tipo de su @Id.
public interface SiloRepository extends JpaRepository<Silo, String> {
}