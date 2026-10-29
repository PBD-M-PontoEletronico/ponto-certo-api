package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Solicitacao;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoOcorrencia;
import com.mobdata.pontocerto.model.TipoMarcacao;
import com.mobdata.pontocerto.model.TipoSolicitacao;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface SolicitacaoRepository extends JpaRepository<Solicitacao, UUID>, JpaSpecificationExecutor<Solicitacao> {

    // Pedidos do próprio funcionário (app), mais recentes primeiro.
    List<Solicitacao> findAllByUsuarioIdOrderByCriadoEmDesc(UUID usuarioId);

    // Justificativas do funcionário num intervalo — usadas no espelho.
    List<Solicitacao> findAllByUsuarioIdAndTipoAndDataBetweenOrderByDataAsc(
            UUID usuarioId, TipoSolicitacao tipo, LocalDate inicio, LocalDate fim);

    // Evitam pedido repetido enquanto o primeiro ainda está pendente.
    boolean existsByUsuarioIdAndTipoAndDataAndTipoMarcacaoAndStatus(
            UUID usuarioId, TipoSolicitacao tipo, LocalDate data, TipoMarcacao tipoMarcacao, StatusSolicitacao status);

    boolean existsByUsuarioIdAndTipoAndDataAndOcorrenciaAndStatus(
            UUID usuarioId, TipoSolicitacao tipo, LocalDate data, TipoOcorrencia ocorrencia, StatusSolicitacao status);
}
