package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Alocacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface AlocacaoRepository extends JpaRepository<Alocacao, UUID> {

    List<Alocacao> findAllByUsuarioId(UUID usuarioId);

    List<Alocacao> findAllBySetorIdAndDataFimIsNull(UUID setorId);

    boolean existsBySetorIdAndDataFimIsNull(UUID setorId);

    @Query("""
        SELECT a FROM Alocacao a
        WHERE a.usuario.id = :usuarioId
          AND a.setor.id = :setorId
          AND (
              (cast(:dataFim as date) IS NULL AND a.dataFim IS NULL)
              OR (cast(:dataFim as date) IS NULL AND a.dataFim >= :dataInicio)
              OR (a.dataFim IS NULL AND a.dataInicio <= :dataFim)
              OR (a.dataInicio <= :dataFim AND a.dataFim >= :dataInicio)
          )
    """)
    List<Alocacao> buscarSobreposicoes(
            @Param("usuarioId") UUID usuarioId,
            @Param("setorId") UUID setorId,
            @Param("dataInicio") LocalDate dataInicio,
            @Param("dataFim") LocalDate dataFim
    );
}