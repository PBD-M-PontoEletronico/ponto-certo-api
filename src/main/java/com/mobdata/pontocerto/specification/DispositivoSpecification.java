package com.mobdata.pontocerto.specification;

import com.mobdata.pontocerto.model.Dispositivo;
import com.mobdata.pontocerto.model.TipoDispositivo;
import org.springframework.data.jpa.domain.Specification;

import java.time.LocalDate;
import java.util.UUID;

public final class DispositivoSpecification {

    private DispositivoSpecification() {
    }

    public static Specification<Dispositivo> comUsuarioNome(String nome) {
        if (nome == null || nome.isBlank()) {
            return Specification.unrestricted();
        }
        return (root, query, cb) ->
                cb.like(cb.lower(root.get("usuario").get("nome")), "%" + nome.toLowerCase() + "%");
    }

    public static Specification<Dispositivo> comTipo(TipoDispositivo tipo) {
        if (tipo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("tipo"), tipo);
    }

    public static Specification<Dispositivo> comSetorId(UUID setorId) {
        if (setorId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("setor").get("id"), setorId);
    }

    public static Specification<Dispositivo> comAtivo(Boolean ativo) {
        if (ativo == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("ativo"), ativo);
    }

    public static Specification<Dispositivo> comEmpresaId(UUID empresaId) {
        if (empresaId == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> cb.equal(root.get("usuario").get("empresa").get("id"), empresaId);
    }

    public static Specification<Dispositivo> comUltimoAcessoEntre(LocalDate de, LocalDate ate) {
        return entrePeriodo("ultimoAcesso", de, ate);
    }

    public static Specification<Dispositivo> comDataVinculoEntre(LocalDate de, LocalDate ate) {
        return entrePeriodo("dataVinculo", de, ate);
    }

    // Os filtros de período comparam contra colunas LocalDateTime: "de" vira o
    // início do dia, "até" vira o início do dia seguinte (inclui o dia inteiro
    // escolhido, sem precisar a pessoa acertar uma hora exata).
    private static Specification<Dispositivo> entrePeriodo(String campo, LocalDate de, LocalDate ate) {
        if (de == null && ate == null) {
            return Specification.unrestricted();
        }
        return (root, query, cb) -> {
            if (de != null && ate != null) {
                return cb.between(root.get(campo), de.atStartOfDay(), ate.plusDays(1).atStartOfDay());
            }
            if (de != null) {
                return cb.greaterThanOrEqualTo(root.get(campo), de.atStartOfDay());
            }
            return cb.lessThan(root.get(campo), ate.plusDays(1).atStartOfDay());
        };
    }
}