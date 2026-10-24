package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Setor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.UUID;

public interface SetorRepository extends JpaRepository<Setor, UUID>, JpaSpecificationExecutor<Setor> {

    List<Setor> findAllByEmpresaId(UUID empresaId);
}