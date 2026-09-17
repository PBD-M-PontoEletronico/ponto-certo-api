package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Alocacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AlocacaoRepository extends JpaRepository<Alocacao, UUID> {

    List<Alocacao> findAllByUsuarioId(UUID usuarioId);

    List<Alocacao> findAllBySetorIdAndDataFimGreaterThanEqual(UUID setorId, LocalDate hoje);

    boolean existsBySetorIdAndDataFimGreaterThanEqual(UUID setorId, LocalDate hoje);
}