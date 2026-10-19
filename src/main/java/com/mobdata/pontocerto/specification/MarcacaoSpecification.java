package com.mobdata.pontocerto.specification;

import com.mobdata.pontocerto.model.Marcacao;
import com.mobdata.pontocerto.model.StatusMarcacao;
import com.mobdata.pontocerto.model.TipoMarcacao;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

public final class MarcacaoSpecification {

    // Mesmo fuso usado na abertura (AberturaService.FUSO) para decidir o que
    // é "hoje" nos filtros de período.
    private static final ZoneId FUSO = ZoneId.of("America/Recife");

    private MarcacaoSpecification() {
    }

    public static Specification<Marcacao> comUsuarioNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("usuario").get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<Marcacao> comSetorId(UUID setorId) {
        if (setorId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("setor").get("id"), setorId);
    }

    public static Specification<Marcacao> comEmpresaId(UUID empresaId) {
        if (empresaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("empresa").get("id"), empresaId);
    }

    public static Specification<Marcacao> comStatus(StatusMarcacao status) {
        if (status == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("status"), status);
    }

    public static Specification<Marcacao> comTipo(TipoMarcacao tipo) {
        if (tipo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Marcacao> comDataEntre(LocalDate de, LocalDate ate) {
        if (de == null && ate == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            if (de != null && ate != null) {
                return cb.between(root.get("horaAparelho"),
                        de.atStartOfDay(FUSO).toInstant(),
                        ate.plusDays(1).atStartOfDay(FUSO).toInstant());
            }
            if (de != null) {
                return cb.greaterThanOrEqualTo(root.get("horaAparelho"), de.atStartOfDay(FUSO).toInstant());
            }
            return cb.lessThan(root.get("horaAparelho"), ate.plusDays(1).atStartOfDay(FUSO).toInstant());
        };
    }
}