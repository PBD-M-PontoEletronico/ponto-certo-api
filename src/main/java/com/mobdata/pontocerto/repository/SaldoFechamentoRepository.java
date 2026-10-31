package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.SaldoFechamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface SaldoFechamentoRepository extends JpaRepository<SaldoFechamento, UUID> {

    Optional<SaldoFechamento> findFirstByUsuarioIdAndAnoMesLessThanOrderByAnoMesDesc(UUID usuarioId, int anoMes);
    void deleteByFechamentoId(UUID fechamentoId);
}