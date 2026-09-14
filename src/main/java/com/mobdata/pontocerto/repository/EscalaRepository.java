package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Escala;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface EscalaRepository extends JpaRepository<Escala, UUID> {

    List<Escala> findAllByEmpresaId(UUID empresaId);
}