package com.mobdata.pontocerto.repository;

import com.mobdata.pontocerto.model.Marcacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public interface MarcacaoRepository extends JpaRepository<Marcacao, UUID> {

    // Marcações do funcionário a partir de um instante, mais recentes primeiro
    // (limitadas a 100 para a abertura não crescer sem controle).
    List<Marcacao> findTop100ByUsuarioIdAndHoraAparelhoGreaterThanEqualOrderByHoraAparelhoDesc(
            UUID usuarioId, Instant desde);
}
