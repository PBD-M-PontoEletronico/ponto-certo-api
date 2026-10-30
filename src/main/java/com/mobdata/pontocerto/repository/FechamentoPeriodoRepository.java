package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.FechamentoPeriodo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface FechamentoPeriodoRepository extends JpaRepository<FechamentoPeriodo, UUID> {

    Optional<FechamentoPeriodo> findByEmpresaIdAndAnoAndMes(UUID empresaId, int ano, int mes);

    List<FechamentoPeriodo> findAllByEmpresaIdOrderByAnoDescMesDesc(UUID empresaId);
}
