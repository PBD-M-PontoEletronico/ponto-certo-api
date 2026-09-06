package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SetorRepository extends JpaRepository<Setor, UUID> {

    List<Setor> findAllByEmpresaId(UUID empresaId);
}