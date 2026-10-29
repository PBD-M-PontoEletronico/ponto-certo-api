package com.mobdata.pontocerto.specification;

import com.mobdata.pontocerto.model.Solicitacao;
import com.mobdata.pontocerto.model.StatusSolicitacao;
import com.mobdata.pontocerto.model.TipoSolicitacao;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class SolicitacaoSpecification {

    private SolicitacaoSpecification() {
    }

    public static Specification<Solicitacao> comUsuarioNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("usuario").get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<Solicitacao> comEmpresaId(UUID empresaId) {
        if (empresaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("empresa").get("id"), empresaId);
    }

    public static Specification<Solicitacao> comStatus(StatusSolicitacao status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Solicitacao> comTipo(TipoSolicitacao tipo) {
        if (tipo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Solicitacao> comDataEntre(LocalDate de, LocalDate ate) {
        if (de == null && ate == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            if (de != null && ate != null) {
                return cb.between(root.get("data"), de, ate);
            }
            if (de != null) {
                return cb.greaterThanOrEqualTo(root.get("data"), de);
            }
            return cb.lessThanOrEqualTo(root.get("data"), ate);
        };
    }

    /**
     * Ordem da fila: pendentes primeiro (o mais antigo no topo, para ser
     * atendido antes); depois os já analisados, do mais recente para o mais
     * antigo. Não entra na consulta de contagem da paginação.
     */
    public static Specification<Solicitacao> ordemDaFila() {
        return (root, query, cb) -> {
            if (query != null && !Long.class.equals(query.getResultType()) && !long.class.equals(query.getResultType())) {
                var prioridade = cb.selectCase()
                        .when(cb.equal(root.get("status"), StatusSolicitacao.PENDENTE), 0)
                        .otherwise(1);
                query.orderBy(
                        cb.asc(prioridade),
                        cb.desc(root.get("analisadoEm")),
                        cb.asc(root.get("criadoEm"))
                );
            }
            return cb.conjunction(); // não filtra nada, só ordena
        };
    }
}
